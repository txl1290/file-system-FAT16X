package fs.fat;

import device.Disk;
import fs.IFileSystem;
import fs.protocol.FAT16X;
import utils.DateUtil;
import utils.FsHelper;
import utils.InputParser;
import utils.Transfer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FatFileSystem implements IFileSystem {

    FAT16X fatfs;

    Disk disk;

    Fd rootFd;

    Map<String, Fd> fdMap = new HashMap<>();

    public FatFileSystem() {
        fatfs = new FAT16X();
        // 根目录
        FAT16X.DirectoryEntry directoryEntry = FAT16X.DirectoryEntry.builder()
                .fileName("root".getBytes(StandardCharsets.UTF_8))
                .attribute(FAT16X.DIR_ATTR)
                .fileSize(0)
                .startingCluster((short) fatfs.rootDirStartClusterIdx())
                .build();
        rootFd = Fd.builder()
                .entry(MixedEntry.builder()
                        .directoryEntry(directoryEntry)
                        .build())
                .currentCluster(fatfs.rootDirStartClusterIdx())
                .currentSector(fatfs.rootDirStartSectorIdx())
                .offset(0)
                .build();
    }

    public void mount(Disk disk) {
        this.disk = disk;
        init();
    }

    private void init() {
        // 读取磁盘非数据区信息
        byte[] firstSector = disk.readSector(0);
        if(!FsHelper.isEmpty(firstSector)) {
            FAT16X.BootSector bootSector = new FAT16X.BootSector(firstSector);
            fatfs.setBootSector(bootSector);
        } else {
            flushBootSector();
        }

        fdMap.put("/", rootFd);

        initFatTable();
        flushFatTable();
    }

    private void initFatTable() {
        // 从磁盘读取FAT表
        int clusterCount = fatfs.clusterCount();
        short[] fatTable = new short[clusterCount];
        int fatTableIdx = 0;
        byte[] fatData = readFatTable();
        for (int i = 0; i < fatData.length; i += 2) {
            fatTable[fatTableIdx++] = Transfer.bytes2Short(fatData[i], fatData[i + 1]);
        }

        // 如果FAT表为空，则初始化
        if(fatTable[0] == FAT16X.EMPTY_BYTE) {
            // FAT表非数据区标记
            fatTable[0] = fatfs.getBootSector().getMediaDescriptor();
            for (int i = 1; i < fatfs.dataRegionStartClusterIdx(); i++) {
                fatTable[i] = FAT16X.FAT16X_EOC;
            }
        }
        fatfs.setFatTable(fatTable);
    }

    /**
     * Open a file
     */
    @Override
    public synchronized Fd open(String path) {
        Fd fd;
        if(fdMap.containsKey(path) && fdMap.get(path).valid()) {
            fd = fdMap.get(path);
        } else {
            fd = findFd(path);
            if(fd == null) {
                throw new IllegalStateException("File not found: " + path);
            }
        }
        return fd;
    }

    public synchronized Fd findFd(String path) {
        if("/".equals(path)) {
            return rootFd;
        }

        String[] pathArr = path.split("/");
        Fd fd = rootFd;
        Fd parentFd;
        String searchPath = "";
        for (String name : pathArr) {
            if("".equals(name)) {
                continue;
            }
            searchPath += "/" + name;
            if(fdMap.containsKey(searchPath) && fdMap.get(searchPath).valid()) {
                fd = fdMap.get(searchPath);
                continue;
            }
            List<MixedEntry> entries = listFiles(fd);
            Optional<MixedEntry> entry = entries.stream().filter(e -> e.getFullName().equals(name)).findFirst();
            if(entry.isPresent()) {
                MixedEntry e = entry.get();
                parentFd = fd;
                int startCluster = Transfer.short2Int(e.getStartingCluster());
                fd = Fd.builder()
                        .entry(e)
                        .currentCluster(startCluster)
                        .currentSector(startCluster * fatfs.getBootSector().getSectorsPerCluster())
                        .offset(0)
                        .parentFd(parentFd)
                        .build();
                fdMap.put(searchPath, fd);
            } else {
                return null;
            }
        }
        return fd;
    }

    private void appendFile(Fd fd, MixedEntry file) {
        List<MixedEntry> files = listFiles(fd);
        if(files.stream().anyMatch(e -> e.getFullName().equals(file.getFullName()))) {
            throw new IllegalStateException("File already exists: " + file.getFullName());
        }
        file.setStartingCluster((short) allocEmptyCluster());
        files.add(file);
        byte[] data = Transfer.mixEntriesToBytes(files);
        resetFd(fd);
        write(fd, data, data.length);
    }

    @Override
    public void close(Fd fd) {
        synchronized(fd) {
            if(fd != rootFd) {
                flushFd(fd);
                collectFileCluster(Transfer.short2Int(fd.getEntry().getStartingCluster()),
                        fd.getEntry().getFileSize());
                fd.close();
            }
        }
    }

    /**
     * 读取全量文件内容
     */
    public String readAll(Fd fd) {
        if(fd == null || !fd.valid()) {
            throw new IllegalArgumentException("invalid fd");
        }

        synchronized(fd) {
            int size = fd.getEntry().getFileSize();
            byte[] buf = new byte[size];
            resetFd(fd);
            read(fd, buf, buf.length);
            return new String(buf, StandardCharsets.UTF_8);
        }
    }

    @Override
    public void read(Fd fd, byte[] buf, int len) {
        if(fd == null || !fd.valid()) {
            throw new IllegalArgumentException("invalid fd");
        }

        synchronized(fd) {
            int clusterIdx = fd.getCurrentCluster();
            int sectorIdx = fd.getCurrentSector();
            int offset = fd.getOffset();
            int readLen = 0;
            while (readLen < len && clusterIdx >= 0) {
                byte[] sectorData = disk.readSector(sectorIdx);
                int copyLen = Math.min(len - readLen, sectorData.length - offset);
                System.arraycopy(sectorData, offset, buf, readLen, copyLen);
                readLen += copyLen;
                if(offset + copyLen == sectorData.length) {
                    offset = 0;
                    sectorIdx++;
                    if(sectorIdx % fatfs.getBootSector().getSectorsPerCluster() == 0) {
                        clusterIdx = getNextCluster(clusterIdx);
                        sectorIdx = clusterIdx * fatfs.getBootSector().getSectorsPerCluster();
                        fd.setCurrentCluster(clusterIdx);
                    }
                    fd.setCurrentSector(sectorIdx);
                } else {
                    offset += copyLen;
                }
                fd.setOffset(offset);
            }
        }
    }

    /**
     * 基于文件偏移量写入数据
     * 主要处理的是fd的寻址，真正的写入操作交给write(Fd fd, byte[] buf, int len)
     */
    public void write(Fd fd, byte[] buf, int off, int len) {
        if(fd == null || !fd.valid()) {
            throw new IllegalArgumentException("invalid fd");
        }

        synchronized(fd) {
            setFdOffset(fd, off);
            write(fd, buf, len);
        }
    }

    public void setFdOffset(Fd fd, int off) {
        int startCluster = fd.getEntry().getStartingCluster();

        // 计算偏移的cluster、sector和offset
        int sectorOffset = off / sectorSize();
        int offset = off % sectorSize();
        int clusterOffset = sectorOffset / sectorsPerCluster();
        int nextCluster = startCluster;
        while (clusterOffset > 0) {
            // 需要找到下一个cluster
            nextCluster = getNextCluster(nextCluster);
            if(nextCluster == -1) {
                // 偏移量为文件尾
                sectorOffset = sectorsPerCluster() - 1;
                offset = sectorSize();
                break;
            } else {
                fd.setCurrentCluster(nextCluster);
                clusterOffset--;
                sectorOffset -= sectorsPerCluster();
            }
        }
        fd.setCurrentSector(fd.getCurrentCluster() * sectorsPerCluster() + sectorOffset);
        fd.setOffset(offset);
    }

    @Override
    public void write(Fd fd, byte[] buf, int len) {
        if(fd == null || !fd.valid()) {
            throw new IllegalArgumentException("invalid fd");
        }

        synchronized(fd) {
            MixedEntry entry = fd.getEntry();
            int clusterIdx = fd.getCurrentCluster();
            int sectorIdx = fd.getCurrentSector();
            int offset = fd.getOffset();
            int writeLen = 0;
            boolean isAllocNewCluster = false;
            while (writeLen < len) {
                byte[] sectorData = disk.readSector(sectorIdx);
                int copyLen = Math.min(len - writeLen, sectorData.length - offset);
                System.arraycopy(buf, writeLen, sectorData, offset, copyLen);
                writeLen += copyLen;
                disk.writeSector(sectorIdx, sectorData);
                if(offset + copyLen == sectorData.length && writeLen < len) {
                    offset = 0;
                    sectorIdx++;
                    // 如果当前簇已经写满，则分配新的簇，更新FAT表，根目录除外
                    if(sectorIdx % fatfs.getBootSector().getSectorsPerCluster() == 0) {
                        if(clusterIdx == fatfs.rootDirStartClusterIdx()) {
                            throw new IllegalStateException("root directory is full");
                        }

                        if(fatfs.getFatTable()[clusterIdx] == FAT16X.FAT16X_END_OF_FILE) {
                            // 分配新的簇
                            clusterIdx = allocEmptyCluster();
                            fatfs.getFatTable()[fd.getCurrentCluster()] = (short) clusterIdx;
                            isAllocNewCluster = true;
                        } else {
                            clusterIdx = getNextCluster(clusterIdx);
                        }

                        fd.setCurrentCluster(clusterIdx);
                        sectorIdx = clusterIdx * fatfs.getBootSector().getSectorsPerCluster();
                    }
                    fd.setCurrentSector(sectorIdx);
                } else {
                    offset += copyLen;
                }
                fd.setOffset(offset);
            }
            // 更新文件大小
            if(fd.getEntry().isFile()) {
                entry.setFileSize(entry.getFileSize() + writeLen);
            }
            entry.setLastAccessDateStamp(DateUtil.getCurrentDateTimeStamp());
            entry.setLastWriteTimeStamp(DateUtil.getCurrentTime());

            // 如果分配了新的簇，则需要更新FAT表
            if(isAllocNewCluster) {
                flushFatTable();
            }
        }
    }

    @Override
    public List<MixedEntry> listFiles(Fd fd) {
        if(fd == null || !fd.valid()) {
            throw new IllegalArgumentException("invalid fd");
        }

        synchronized(fd) {
            resetFd(fd);

            List<MixedEntry> entries = new ArrayList<>();
            MixedEntry entry = fd.getEntry();
            if(entry.isDir()) {
                byte[] buf = new byte[fatfs.sectorSize()];
                List<Byte> left = new ArrayList<>();
                while (fd.getCurrentCluster() >= 0) {
                    read(fd, buf, buf.length);
                    // 合并上次剩余的数据
                    byte[] data = new byte[left.size() + buf.length];
                    left.forEach(b -> data[left.indexOf(b)] = b);
                    System.arraycopy(buf, 0, data, left.size(), buf.length);
                    entries.addAll(Transfer.bytesToMixEntries(data, left));
                }
            }
            return entries;
        }
    }

    /**
     * 创建文件
     */
    @Override
    public void appendFile(String path, boolean isDir) {
        Fd parentFd = open(path.substring(0, path.lastIndexOf("/")));
        if(parentFd == null) {
            throw new IllegalArgumentException("invalid path");
        }

        FAT16X.DirectoryEntry directoryEntry = FAT16X.DirectoryEntry.builder()
                .creationTimeStamp(DateUtil.getCurrentTime())
                .lastAccessDateStamp(DateUtil.getCurrentDateTimeStamp())
                .lastWriteTimeStamp(DateUtil.getCurrentTime())
                .fileSize(0)
                .build();

        MixedEntry file = new MixedEntry(null, directoryEntry);

        if(isDir) {
            directoryEntry.setAttribute(FAT16X.DIR_ATTR);
            String dirName = InputParser.getDirName(path);
            file.setFileName(dirName, "");
        } else {
            directoryEntry.setAttribute(FAT16X.FILE_ATTR);
            String fileName = InputParser.getFileName(path);
            file.setFileName(fileName, InputParser.getFileExtension(path));
        }

        synchronized(parentFd) {
            appendFile(parentFd, file);
            close(parentFd);
            flushFatTable();
        }
    }

    @Override
    public void removeFile(String path) {
        Fd fd = open(path);
        synchronized(fd) {
            if(fd.getEntry().isDir() && listFiles(fd).size() > 0) {
                throw new IllegalStateException("dir is not empty, can not remove");
            } else {
                collectClusterChain(fd.getEntry().getStartingCluster());
                removeFile(fd.getParentFd(), fd.getEntry());
                flushFatTable();
                fdMap.remove(path);
            }
        }
    }

    private void removeFile(Fd fd, MixedEntry entry) {
        synchronized(fd) {
            List<MixedEntry> files = listFiles(fd);
            Iterator<MixedEntry> it = files.iterator();
            while (it.hasNext()) {
                MixedEntry file = it.next();
                if(file.getFullName().equals(entry.getFullName())) {
                    it.remove();
                    break;
                }
            }
            byte[] buf = Transfer.mixEntriesToBytes(files);
            resetFd(fd);
            // 覆盖原来的目录条目，删除导致的多余部分用0填充
            write(fd, buf, buf.length);
            int resetSize = FAT16X.ENTRY_SIZE;
            if(entry.getLfnEntries() != null) {
                resetSize += entry.getLfnEntries().length * FAT16X.ENTRY_SIZE;
            }
            write(fd, new byte[resetSize], resetSize);
        }
    }

    private void freeCluster(int clusterIdx) {
        fatfs.getFatTable()[clusterIdx] = FAT16X.FAT16X_FREE_CLUSTER;
        // 清空簇
        byte[] empty = new byte[fatfs.sectorSize()];
        int sectorIdx = clusterIdx * fatfs.getBootSector().getSectorsPerCluster();
        for (int i = 0; i < fatfs.getBootSector().getSectorsPerCluster(); i++) {
            disk.writeSector(sectorIdx + i, empty);
        }
    }

    public synchronized void format() {
        try {
            // 清空磁盘
            disk.getFw().setLength(0L);

            fdMap.clear();

            // 重新写入引导扇区
            disk.getFw().setLength(2L * 1024 * 1024 * 1024);
            flushBootSector();
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int sectorSize() {
        return fatfs.sectorSize();
    }

    public int sectorsPerCluster() {
        return fatfs.getBootSector().getSectorsPerCluster();
    }

    private int allocEmptyCluster() {
        int clusterIdx = -1;
        short[] fatTable = fatfs.getFatTable();

        // 偏移量1，因为0号簇是保留的；没有提前计算FAT及其副本占用的簇，可以算但是优先级不高
        for (int i = 1; i <= FAT16X.MAX_CLUSTER_CAN_APPLY; i++) {
            if(fatTable[i] == FAT16X.FAT16X_FREE_CLUSTER) {
                clusterIdx = i;
                break;
            }
        }
        if(clusterIdx == -1) {
            throw new IllegalStateException("disk has no enough space");
        }
        fatTable[clusterIdx] = FAT16X.FAT16X_END_OF_FILE;
        return clusterIdx;
    }

    private void resetFd(Fd fd) {
        if(fd == rootFd) {
            fd.setCurrentCluster(fatfs.rootDirStartClusterIdx());
            fd.setCurrentSector(fatfs.rootDirStartSectorIdx());
            fd.setOffset(0);
        } else {
            fd.setCurrentCluster(Transfer.short2Int(fd.getEntry().getStartingCluster()));
            fd.setCurrentSector(fd.getCurrentCluster() * fatfs.getBootSector().getSectorsPerCluster());
            fd.setOffset(0);
        }
    }

    /**
     * 刷新文件描述符的目录项
     */
    private void flushFd(Fd fd) {
        MixedEntry entry = fd.getEntry();
        Fd parentFd = fd.getParentFd();
        synchronized(parentFd) {
            List<MixedEntry> entries = listFiles(parentFd);
            // 从entries中找到entry，替换
            for (int i = 0; i < entries.size(); i++) {
                MixedEntry e = entries.get(i);
                if(e.getFullName().equals(entry.getFullName())) {
                    entries.set(i, entry);
                    break;
                }
            }

            byte[] buf = Transfer.mixEntriesToBytes(entries);
            resetFd(parentFd);
            write(parentFd, buf, buf.length);
        }
    }

    private void flushBootSector() {
        disk.writeSector(0, fatfs.getBootSector().toBytes());
    }

    private byte[] readFatTable() {
        byte[] fatTableData = new byte[fatfs.fatSectorCount() * fatfs.sectorSize()];
        int startSectorIdx = fatfs.fatStartSectorIdx();
        int endSectorIdx = fatfs.fatEndSectorIdx();
        for (int i = startSectorIdx; i <= endSectorIdx; i++) {
            byte[] sectorData = disk.readSector(i);
            System.arraycopy(sectorData, 0, fatTableData, (i - startSectorIdx) * fatfs.sectorSize(), fatfs.sectorSize());
        }
        return fatTableData;
    }

    private void flushFatTable() {
        short[] fatTable = fatfs.getFatTable();
        byte[] sectorData = new byte[disk.sectorSize()];
        int mod = disk.sectorSize() / 2;
        int startSectorIdx = fatfs.fatStartSectorIdx();
        for (int i = 0; i < fatTable.length; i++) {
            System.arraycopy(Transfer.short2Bytes(fatTable[i]), 0, sectorData, (i * 2) % disk.sectorSize(), 2);
            if((i + 1) % mod == 0) {
                int sectorIdx = startSectorIdx + i / mod;
                disk.writeSector(sectorIdx, sectorData);
                sectorData = new byte[disk.sectorSize()];
            }
        }

        // FAT副本拷贝
        copyFat();
    }

    /**
     * 将FAT副本拷贝到磁盘
     */
    private void copyFat() {
        int inc = fatfs.fatSectorCount();
        int startSectorIdx = fatfs.fatStartSectorIdx();
        int endSectorIdx = fatfs.fatEndSectorIdx();
        for (int i = 1; i < fatfs.getBootSector().getNumberOfFATCopies(); i++) {
            for (int j = startSectorIdx; j <= endSectorIdx; j++) {
                byte[] sectorData = disk.readSector(j);
                disk.writeSector(j + i * inc, sectorData);
            }
        }
    }

    /**
     * 回收多余的簇，比如文件大小变小了
     * 同步回收比较慢，可以考虑异步回收
     */
    private void collectFileCluster(int startCluster, int fileSize) {
        int usedCluster = Math.max(1, (int) Math.ceil(fileSize * 1.0 / fatfs.clusterSize()));
        int clusterIdx = startCluster;
        int lastClusterIdx = clusterIdx;

        // 找到文件最后一个簇
        for (int i = 0; i < usedCluster; i++) {
            if(i == usedCluster - 1) {
                lastClusterIdx = clusterIdx;
            }
            clusterIdx = getNextCluster(clusterIdx);
        }

        // 回收簇链
        if(clusterIdx >= 0) {
            collectClusterChain(clusterIdx);
            // 重置文件结尾
            fatfs.getFatTable()[lastClusterIdx] = FAT16X.FAT16X_END_OF_FILE;
            flushFatTable();
        }
    }

    /**
     * 回收簇及其后的簇链
     */
    private void collectClusterChain(int clusterIdx) {
        while (clusterIdx != -1) {
            int nextClusterIdx = getNextCluster(clusterIdx);
            freeCluster(clusterIdx);
            clusterIdx = nextClusterIdx;
        }
    }

    public int getNextCluster(int startCluster) {
        int clusterIdx = Transfer.short2Int(fatfs.getFatTable()[startCluster]);
        if(clusterIdx >= FAT16X.MIN_CLUSTER_CAN_APPLY && clusterIdx <= FAT16X.MAX_CLUSTER_CAN_APPLY) {
            return clusterIdx;
        } else {
            return -1;
        }
    }
}
