package dirven;

import io.Disk;
import protocol.FAT16X;
import utils.DateUtil;
import utils.FsHelper;
import utils.InputParser;
import utils.Transfer;

import java.util.ArrayList;
import java.util.List;

public class DiskDriven {

    private static Disk disk;

    private static String currentPath;

    static {
        disk = new Disk();
        currentPath = "/";
    }

    public static Disk getDisk() {
        return disk;
    }

    public static String getCurrentPath() {
        return currentPath;
    }

    public static void setCurrentPath(String currentPath) {
        DiskDriven.currentPath = currentPath;
    }

    public static void format() {
        disk.clean();
        disk = new Disk();
        // 初始化boot sector持久化
        disk.writeSector(0, disk.getFs().getBootSector().toBytes());
        // 初始化FAT持久化
        storeFat();
    }

    public static FAT16X.DirectoryEntry createFile(String absolutePath, int fizeSize) {
        String fileName = InputParser.getFileName(absolutePath);
        String fileExt = InputParser.getFileExtension(absolutePath);

        FAT16X.DirectoryEntry file = FAT16X.DirectoryEntry.builder()
                .creationTimeStamp(DateUtil.getCurrentTime())
                .lastAccessDateStamp(DateUtil.getCurrentDateTimeStamp())
                .lastWriteTimeStamp(DateUtil.getCurrentTime())
                .attribute(FAT16X.FILE_ATTR)
                .fileSize(fizeSize)
                .build();
        file.setFileName(fileName).setFileNameExt(fileExt);
        return createDirectoryEntry(absolutePath, file);
    }

    public static FAT16X.DirectoryEntry makeDirectory(String absolutePath) {
        String dirName = InputParser.getDirName(absolutePath);
        FAT16X.DirectoryEntry dir = FAT16X.DirectoryEntry.builder()
                .creationTimeStamp(DateUtil.getCurrentTime())
                .lastAccessDateStamp(DateUtil.getCurrentDateTimeStamp())
                .lastWriteTimeStamp(DateUtil.getCurrentTime())
                .attribute(FAT16X.DIR_ATTR)
                .fileSize(0)
                .build();
        dir.setFileName(dirName);
        return createDirectoryEntry(absolutePath, dir);
    }

    public static FAT16X.DirectoryEntry createRootDirectoryEntry(FAT16X.DirectoryEntry directoryEntry) {
        FAT16X.DirectoryEntry[] entries = disk.getFs().getRootDirectory();
        if(entries.length >= disk.getFs().getBootSector().getMaxRootEntries()) {
            throw new IllegalArgumentException("root directory is full");
        }
        int allocClusterIdx = allocEmptyCluster();
        directoryEntry.setStartingCluster((short) allocClusterIdx);
        disk.getFs().setRootDirectory(FsHelper.addEntry(entries, directoryEntry));
        storeRootDirectory();
        storeFat();
        return directoryEntry;
    }

    /**
     * 写文件内容，并更新文件元信息
     */
    public static void writeFileContent(String redirectPath, byte[] content) {
        String absolutePath = getAbsolutePath(redirectPath);
        FAT16X.DirectoryEntry file = findEntry(absolutePath);
        if(file == null) {
            // create a new file
            file = createFile(absolutePath, 0);
        }

        writeFileContent(file, content);

        // 写文件有可能失败，所以需要后更新文件元信息
        String parentPath = InputParser.getFileParentPath(absolutePath);
        file.setFileSize(content.length);
        file.setLastWriteTimeStamp(DateUtil.getCurrentTime());
        file.setLastAccessDateStamp(DateUtil.getCurrentDateTimeStamp());
        updateEntryToDisk(parentPath, file);
    }

    /**
     * 读取文件内容
     */
    public static byte[] readFileContent(FAT16X.DirectoryEntry entry) {
        byte[] content = new byte[entry.getFileSize()];
        if(entry.isFile()) {
            int clusterIdx = Transfer.short2Int(entry.getStartingCluster());
            short[] fatTable = disk.getFs().getFatTable();
            int clusterCount = 0;
            do {
                byte[] clusterData = disk.readCluster(clusterIdx);
                int offset = clusterCount * disk.clusterSize();
                if(offset + clusterData.length > content.length) {
                    System.arraycopy(clusterData, 0, content, offset, content.length - offset);
                } else {
                    System.arraycopy(clusterData, 0, content, offset, clusterData.length);
                }
                clusterCount++;
                clusterIdx = Transfer.short2Int(fatTable[clusterIdx]);
            } while (clusterIdx >= FAT16X.MIN_CLUSTER_CAN_APPLY && clusterIdx <= FAT16X.MAX_CLUSTER_CAN_APPLY);
        }
        return content;
    }

    /**
     * 读取文件夹条目信息
     */
    public static FAT16X.DirectoryEntry[] readDirEntries(FAT16X.DirectoryEntry entry) {
        List<FAT16X.DirectoryEntry> entries = new ArrayList<>();
        if(entry.isDir()) {
            int startClusterIdx = Transfer.short2Int(entry.getStartingCluster());
            short[] fatTable = disk.getFs().getFatTable();
            do {
                byte[] clusterData = disk.readCluster(startClusterIdx);
                entries.addAll(Transfer.bytesToEntries(clusterData));
                startClusterIdx = Transfer.short2Int(fatTable[startClusterIdx]);
            } while (startClusterIdx >= FAT16X.MIN_CLUSTER_CAN_APPLY && startClusterIdx <= FAT16X.MAX_CLUSTER_CAN_APPLY);
        }
        return entries.toArray(new FAT16X.DirectoryEntry[0]);
    }

    /**
     * 文件内容持久化（全量）
     */
    private static void writeFileContent(FAT16X.DirectoryEntry file, byte[] content) {
        // 可能会跨cluster
        int startClusterIdx = Transfer.short2Int(file.getStartingCluster());
        writeFileContent(startClusterIdx, content);
    }

    /**
     * 可操作部分文件内容持久化，需提前计算好增量content以及对应的startClusterIdx
     */
    private static void writeFileContent(int startClusterIdx, byte[] content) {
        short[] fatTable = disk.getFs().getFatTable();
        List<Integer> usedFatIdx = new ArrayList<>();
        do {
            usedFatIdx.add(startClusterIdx);
            startClusterIdx = Transfer.short2Int(fatTable[startClusterIdx]);
        } while (startClusterIdx >= FAT16X.MIN_CLUSTER_CAN_APPLY && startClusterIdx <= FAT16X.MAX_CLUSTER_CAN_APPLY);

        // 计算需要的cluster数量
        int needClusterCount = (int) Math.ceil(content.length * 1.0 / disk.clusterSize());

        // 如果需要的cluster数量大于已经分配的cluster数量，则需要分配新的cluster
        if(needClusterCount > usedFatIdx.size()) {
            int needAllocClusterCount = needClusterCount - usedFatIdx.size();
            for (int i = 0; i < needAllocClusterCount; i++) {
                int allocClusterIdx = allocEmptyCluster();
                fatTable[usedFatIdx.get(usedFatIdx.size() - 1)] = (short) allocClusterIdx;
                usedFatIdx.add(allocClusterIdx);
            }
        } else if(needClusterCount < usedFatIdx.size()) {
            // 如果需要的cluster数量小于已经分配的cluster数量，则需要回收多余的cluster
            int needFreeClusterCount = usedFatIdx.size() - needClusterCount;
            for (int i = 0; i < needFreeClusterCount; i++) {
                int freeClusterIdx = usedFatIdx.get(usedFatIdx.size() - 1);
                fatTable[usedFatIdx.get(usedFatIdx.size() - 2)] = FAT16X.FAT16X_END_OF_FILE;
                usedFatIdx.remove(usedFatIdx.size() - 1);
                disk.freeCluster(freeClusterIdx);
            }
        }

        // 写入数据
        for (int i = 0; i < needClusterCount; i++) {
            int offset = i * disk.clusterSize();
            byte[] clusterData = new byte[disk.clusterSize()];
            if(offset + clusterData.length > content.length) {
                System.arraycopy(content, offset, clusterData, 0, content.length - offset);
            } else {
                System.arraycopy(content, offset, clusterData, 0, clusterData.length);
            }
            disk.writeCluster(usedFatIdx.get(i), clusterData);
        }

        storeFat();
    }

    public static String getAbsolutePath(String path) {
        if(!InputParser.isRoot(path) && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if(InputParser.isAbsolutePath(path)) {
            return InputParser.trimPath(path);
        } else {
            if(InputParser.isRoot(getCurrentPath())) {
                return InputParser.trimPath(getCurrentPath() + path);
            } else {
                return InputParser.trimPath(getCurrentPath() + "/" + path);
            }
        }
    }

    /**
     * 按照绝对路径查找DirectoryEntry
     */
    public static FAT16X.DirectoryEntry findEntry(String absolutePath) {
        return findEntry(InputParser.getFilePathArr(absolutePath));
    }

    /**
     * 写文件夹条目信息（全量覆盖）
     */
    private static void writeDirEntries(FAT16X.DirectoryEntry dir, FAT16X.DirectoryEntry[] entries) {
        byte[] content = new byte[entries.length * FAT16X.ENTRY_SIZE];
        for (int i = 0; i < entries.length; i++) {
            byte[] entryBytes = entries[i].toBytes();
            System.arraycopy(entryBytes, 0, content, i * FAT16X.ENTRY_SIZE, entryBytes.length);
        }
        writeFileContent(dir, content);
    }

    /**
     * 性能优化：添加一个文件夹条目信息，只写入磁盘最后一个cluster
     */
    private static void addDirEntry(FAT16X.DirectoryEntry parent, FAT16X.DirectoryEntry entry) {
        // 找到dir最后一个cluster
        int lastClusterIdx = Transfer.short2Int(parent.getStartingCluster());
        short[] fatTable = disk.getFs().getFatTable();
        while (fatTable[lastClusterIdx] != FAT16X.FAT16X_END_OF_FILE) {
            lastClusterIdx = Transfer.short2Int(fatTable[lastClusterIdx]);
        }
        byte[] clusterData = disk.readCluster(lastClusterIdx);
        List<FAT16X.DirectoryEntry> entries = Transfer.bytesToEntries(clusterData);
        entries.add(entry);
        byte[] content = new byte[entries.size() * FAT16X.ENTRY_SIZE];
        for (int i = 0; i < entries.size(); i++) {
            byte[] entryBytes = entries.get(i).toBytes();
            System.arraycopy(entryBytes, 0, content, i * FAT16X.ENTRY_SIZE, entryBytes.length);
        }
        writeFileContent(lastClusterIdx, content);
    }

    /**
     * 更新条目信息到磁盘，并更新父目录的时间戳
     */
    private static void updateEntryToDisk(String parentPath, FAT16X.DirectoryEntry entry) {
        if(InputParser.isRoot(parentPath)) {
            // 更新根目录的条目
            updateRootEntryToDisk(entry);
        } else {
            // 更新父目录的条目
            FAT16X.DirectoryEntry parentEntry = findEntry(parentPath);
            if(parentEntry == null) {
                throw new IllegalStateException("no such file or directory: " + parentPath);
            }
            updateEntryToDisk(parentEntry, entry);

            // 更新父目录的时间戳
            updateParentEntryTimeStamp(parentPath, parentEntry);
        }
    }

    private static void updateParentEntryTimeStamp(String parentPath, FAT16X.DirectoryEntry parentEntry) {
        String grandParentPath = InputParser.getFileParentPath(parentPath);
        parentEntry.setLastAccessDateStamp(DateUtil.getCurrentDateTimeStamp());
        parentEntry.setLastWriteTimeStamp(DateUtil.getCurrentTime());
        if(InputParser.isRoot(grandParentPath)) {
            updateRootEntryToDisk(parentEntry);
        } else {
            FAT16X.DirectoryEntry grandParentEntry = findEntry(grandParentPath);
            updateEntryToDisk(grandParentEntry, parentEntry);
        }
    }

    private static void updateEntryToDisk(FAT16X.DirectoryEntry parent, FAT16X.DirectoryEntry entry) {
        if(parent == null) {
            return;
        }

        FAT16X.DirectoryEntry[] entries = readDirEntries(parent);
        //找到entries中的entry，然后更新它
        boolean match = false;
        for (int i = 0; i < entries.length; i++) {
            if(entries[i].equals(entry)) {
                entries[i] = entry;
                match = true;
                break;
            }
        }
        if(match) {
            // todo: 有优化空间，只更新entry对应的cluster
            writeDirEntries(parent, entries);
        }
    }

    private static void updateRootEntryToDisk(FAT16X.DirectoryEntry entry) {
        FAT16X.DirectoryEntry[] entries = disk.getFs().getRootDirectory();
        //找到entries中的entry，然后更新它
        boolean match = false;
        for (int i = 0; i < entries.length; i++) {
            if(entries[i].equals(entry)) {
                entries[i] = entry;
                match = true;
                break;
            }
        }
        if(match) {
            storeRootDirectory();
        }
    }

    private static FAT16X.DirectoryEntry findEntry(String[] paths) {
        FAT16X.DirectoryEntry entry = null;
        FAT16X.DirectoryEntry[] findRange = disk.getFs().getRootDirectory();
        for (String subPath : paths) {
            entry = findEntry(findRange, subPath);
            if(entry == null) {
                break;
            } else if(entry.isDir()) {
                findRange = readDirEntries(entry);
            } else {
                findRange = new FAT16X.DirectoryEntry[0];
            }
        }
        // 更新访问时间戳
        if(entry != null) {
            entry.setLastAccessDateStamp(DateUtil.getCurrentDateTimeStamp());
        }
        return entry;
    }

    private static FAT16X.DirectoryEntry findEntry(FAT16X.DirectoryEntry[] entries, String entryName) {
        for (FAT16X.DirectoryEntry entry : entries) {
            if(entry.getFullName().equals(entryName)) {
                return entry;
            }
        }
        return null;
    }

    private static int allocEmptyCluster() {
        int clusterIdx = -1;
        short[] fatTable = disk.getFs().getFatTable();

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

    /**
     * 将FAT写入磁盘
     */
    private static void storeFat() {
        short[] fatTable = disk.getFs().getFatTable();
        byte[] sectorData = new byte[disk.sectorSize()];
        int mod = disk.sectorSize() / 2;
        int startClusterIdx = disk.fatStartClusterIdx();
        for (int i = 0; i < fatTable.length; i++) {
            System.arraycopy(Transfer.short2Bytes(fatTable[i]), 0, sectorData, (i * 2) % disk.sectorSize(), 2);
            if((i + 1) % mod == 0) {
                int sectorIdx = startClusterIdx * disk.getFs().getBootSector().getSectorsPerCluster() + i / mod;
                disk.writeSector(sectorIdx, sectorData);
                sectorData = new byte[disk.sectorSize()];
            }
        }
        if(!FsHelper.isEmpty(sectorData)) {
            int sectorIdx = startClusterIdx * disk.getFs().getBootSector().getSectorsPerCluster() + (int) Math.ceil(fatTable.length * 1.0 / mod);
            disk.writeSector(sectorIdx, sectorData);
        }

        // FAT副本拷贝
        copyFat();
    }

    /**
     * 将FAT副本拷贝到磁盘
     */
    private static void copyFat() {
        for (int i = 1; i < disk.getFs().getBootSector().getNumberOfFATCopies(); i++) {
            int inc = disk.fatEndClusterIdx() - disk.fatStartClusterIdx();
            for (int j = disk.fatStartClusterIdx(); j < disk.fatEndClusterIdx(); j++) {
                byte[] clusterData = disk.readCluster(j);
                disk.writeCluster(j + inc * i, clusterData);
            }
        }
    }

    /**
     * 将根目录写入磁盘
     */
    private static void storeRootDirectory() {
        FAT16X.DirectoryEntry[] entries = disk.getFs().getRootDirectory();
        byte[] sectorData = new byte[disk.sectorSize()];
        int mod = disk.sectorSize() / FAT16X.ENTRY_SIZE;
        for (int i = 0; i < entries.length; i++) {
            System.arraycopy(entries[i].toBytes(), 0, sectorData, (i * FAT16X.ENTRY_SIZE) % disk.sectorSize(), FAT16X.ENTRY_SIZE);
            if((i + 1) % mod == 0) {
                disk.writeSector((i + 1) / mod, sectorData);
                sectorData = new byte[disk.sectorSize()];
            }
        }
        if(!FsHelper.isEmpty(sectorData)) {
            disk.writeSector((int) Math.ceil(entries.length * 1.0 / mod), sectorData);
        }
    }

    private static FAT16X.DirectoryEntry createDirectoryEntry(String absolutePath, FAT16X.DirectoryEntry directoryEntry) {
        // todo：优化查询次数
        FAT16X.DirectoryEntry entry = findEntry(absolutePath);
        if(entry != null) {
            throw new IllegalArgumentException("file already exists: " + absolutePath);
        }

        String parentPath = InputParser.getFileParentPath(absolutePath);

        if(InputParser.isRoot(parentPath)) {
            return createRootDirectoryEntry(directoryEntry);
        }

        FAT16X.DirectoryEntry parentEntry = findEntry(parentPath);

        if(parentEntry == null) {
            throw new IllegalArgumentException("no such file or directory: " + absolutePath);
        }

        int allocClusterIdx = allocEmptyCluster();
        directoryEntry.setStartingCluster((short) allocClusterIdx);

        addDirEntry(parentEntry, directoryEntry);

        // 更新父目录的信息
        updateParentEntryTimeStamp(parentPath, parentEntry);
        storeFat();
        return directoryEntry;
    }
}
