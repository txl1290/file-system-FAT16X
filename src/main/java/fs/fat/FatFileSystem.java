package fs.fat;

import device.Disk;
import fs.IFileSystem;
import utils.DateUtil;
import utils.FsHelper;
import utils.InputParser;
import utils.Transfer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FatFileSystem implements IFileSystem {

    static FAT16X fatfs;

    static Disk disk;

    static Fd rootFd;

    static {
        fatfs = new FAT16X();
        try {
            RandomAccessFile fw = new RandomAccessFile(fatfs.getDataRegion(), "rw");
            fw.setLength(2L * 1024 * 1024 * 1024);
            disk = new Disk(fw);
            flushBootSector();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FatFileSystem() {
        // 初始化文件系统
        init();
        // 根目录
        rootFd = Fd.builder()
                .entry(FAT16X.DirectoryEntry.builder()
                        .fileName("root".getBytes(StandardCharsets.UTF_8))
                        .attribute(FAT16X.DIR_ATTR)
                        .fileSize(0)
                        .startingCluster((short) fatfs.rootDirStartClusterIdx())
                        .build())
                .currentCluster(fatfs.rootDirStartClusterIdx())
                .currentSector(fatfs.rootDirStartSectorIdx())
                .offset(0)
                .build();
    }

    private void init() {
        // 读取磁盘非数据区信息
        byte[] firstSector = disk.readSector(0);
        if(!FsHelper.isEmpty(firstSector)) {
            FAT16X.BootSector bootSector = new FAT16X.BootSector(firstSector);
            fatfs.setBootSector(bootSector);
        }

        initFatTable();
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
    public Fd open(String path) {
        Fd fd = findFd(path);
        if(fd == null) {
            throw new IllegalStateException("File not found: " + path);
        }
        return fd;
    }

    public Fd findFd(String path) {
        String[] pathArr = path.split("/");
        resetFd(rootFd);
        Fd fd = rootFd;
        Fd parentFd;
        for (int i = 0; i < pathArr.length; i++) {
            String name = pathArr[i];
            if("".equals(name)) {
                continue;
            }
            List<FAT16X.DirectoryEntry> entries = listFiles(fd);
            Optional<FAT16X.DirectoryEntry> entry = entries.stream().filter(e -> e.getFullName().equals(name)).findFirst();
            if(entry.isPresent()) {
                FAT16X.DirectoryEntry e = entry.get();
                parentFd = fd;
                resetFd(parentFd);
                int startCluster = Transfer.short2Int(e.getStartingCluster());
                fd = Fd.builder()
                        .entry(e)
                        .currentCluster(startCluster)
                        .currentSector(startCluster * fatfs.getBootSector().getSectorsPerCluster())
                        .offset(0)
                        .parentFd(parentFd)
                        .build();
            } else {
                return null;
            }
        }
        return fd;
    }

    private void appendFile(Fd fd, FAT16X.DirectoryEntry file) {
        List<FAT16X.DirectoryEntry> files = listFiles(fd);
        files.add(file);
        byte[] data = Transfer.entriesToBytes(files);
        resetFd(fd);
        write(fd, data, data.length);
    }

    @Override
    public void close(Fd fd) {
        if(fd != null && fd != rootFd) {
            flushFd(fd);
            collectFileCluster(Transfer.short2Int(fd.getEntry().getStartingCluster()), fd.getEntry().getFileSize());
            fd.close();
        }
    }

    @Override
    public void read(Fd fd, byte[] buf, int len) {
        if(fd == null || !fd.valid()) {
            throw new IllegalArgumentException("invalid fd");
        }

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

    @Override
    public void write(Fd fd, byte[] buf, int len) {
        if(fd == null || !fd.valid()) {
            throw new IllegalArgumentException("invalid fd");
        }

        FAT16X.DirectoryEntry entry = fd.getEntry();
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

    @Override
    public List<FAT16X.DirectoryEntry> listFiles(Fd fd) {
        if(fd == null || !fd.valid()) {
            throw new IllegalArgumentException("invalid fd");
        }

        List<FAT16X.DirectoryEntry> entries = new ArrayList<>();
        FAT16X.DirectoryEntry entry = fd.getEntry();
        if(entry.isDir()) {
            byte[] buf = new byte[fatfs.sectorSize()];
            while (fd.getCurrentCluster() >= 0) {
                read(fd, buf, buf.length);
                entries.addAll(Transfer.bytesToEntries(buf));
            }
        }
        return entries;
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
        FAT16X.DirectoryEntry file = FAT16X.DirectoryEntry.builder()
                .startingCluster((short) allocEmptyCluster())
                .creationTimeStamp(DateUtil.getCurrentTime())
                .lastAccessDateStamp(DateUtil.getCurrentDateTimeStamp())
                .lastWriteTimeStamp(DateUtil.getCurrentTime())
                .fileSize(0)
                .build();

        if(isDir) {
            file.setAttribute(FAT16X.DIR_ATTR);
            file.setFileName(InputParser.getDirName(path));
        } else {
            file.setAttribute(FAT16X.FILE_ATTR);
            file.setFileName(InputParser.getFileName(path)).setFileNameExt(InputParser.getFileExtension(path));
        }
        appendFile(parentFd, file);
        close(parentFd);
        flushFatTable();
    }

    private void freeCluster(int clusterIdx) {
        fatfs.getFatTable()[clusterIdx] = FAT16X.FAT16X_FREE_CLUSTER;
        // 清空簇
        byte[] empty = new byte[fatfs.sectorSize() * fatfs.getBootSector().getSectorsPerCluster()];
        int sectorIdx = clusterIdx * fatfs.getBootSector().getSectorsPerCluster();
        for (int i = 0; i < fatfs.getBootSector().getSectorsPerCluster(); i++) {
            disk.writeSector(sectorIdx + i, empty);
        }
    }

    public void format() {
        try {
            // 清空磁盘
            disk.getFw().setLength(0L);

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
        FAT16X.DirectoryEntry entry = fd.getEntry();
        Fd parentFd = fd.getParentFd();
        List<FAT16X.DirectoryEntry> entries = listFiles(parentFd);
        // 从entries中找到entry，替换
        for (int i = 0; i < entries.size(); i++) {
            FAT16X.DirectoryEntry e = entries.get(i);
            if(e.getFullName().equals(entry.getFullName())) {
                entries.set(i, entry);
                break;
            }
        }

        byte[] buf = Transfer.entriesToBytes(entries);
        resetFd(parentFd);
        write(parentFd, buf, buf.length);
    }

    private static void flushBootSector() {
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
