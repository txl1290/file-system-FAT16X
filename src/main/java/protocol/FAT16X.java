package protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import utils.Transfer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

@Data
public class FAT16X {

    public static final byte EMPTY_BYTE = (byte) 0x00;

    public static final byte FILE_ATTR = 0x00;

    public static final byte DIR_ATTR = 0x10;

    public static final byte READ_ONLY_ATTR = 0x01;

    public static final short FAT16X_EOC = (short) 0xFFFF;

    public static final short FAT16X_FREE_CLUSTER = (short) 0x0000;

    public static final short FAT16X_BAD_CLUSTER = (short) 0xFFF7;

    public static final short FAT16X_END_OF_FILE = (short) 0xFFF8;

    public static final int ENTRY_SIZE = 32;

    /**
     * 协议规定的最大可申请的簇号，FAT取值定义中的约束
     */
    public static final int MAX_CLUSTER_CAN_APPLY = 0xFFEF;

    protected static final char[] FILE_SIZE_UNITS = new char[] { 'B', 'K', 'M', 'G', 'T' };

    BootSector bootSector = new BootSector();

    short[] fatTable;

    DirectoryEntry[] rootDirectory;

    File dataRegion;

    public FAT16X() {
    }

    @Data
    public static class BootSector {
        byte[] jumpCode = new byte[3];
        byte[] oemName = new byte[8];
        short bytesPerSector = 512;
        byte sectorsPerCluster = 64;
        short reservedSectors = 1;
        byte numberOfFATCopies = 2;
        short numberOfPossibleRootEntries = 63;
        short smallNumberOfSectors = 0;
        byte mediaDescriptor = (byte) 0xF8;
        short sectorsPerFAT = 256;
        short sectorsPerTrack = 0;
        short numberOfHeads = 0;
        int hiddenSectors = 0;
        int largeNumberOfSectors = 4194240;
        byte driveNumber = 0;
        byte reserved = 0;
        byte extendedBootSignature = 0;
        int volumeSerialNumber = 0;
        byte[] volumeLabe = new byte[11];
        byte[] fileSystemType = new byte[8];
        byte[] bootstrapCode = new byte[448];
        short bootSectorSignature = (short) 0xAA55;

        public BootSector() {
            jumpCode[0] = (byte) 0xEB;
            jumpCode[1] = 0x3C;
            jumpCode[2] = (byte) 0x90;
            Arrays.fill(oemName, (byte) 0);
            setOemName("mos".getBytes());
            setFileSystemType(new byte[] { 'F', 'A', 'T', '1', '6', 'X' });
            Arrays.fill(volumeLabe, (byte) 0);
            Arrays.fill(bootstrapCode, (byte) 0);
        }

        public BootSector(byte[] boot) {
            System.arraycopy(boot, 0x00, jumpCode, 0, jumpCode.length);
            System.arraycopy(boot, 0x03, oemName, 0, oemName.length);
            byte[] shortTmp = new byte[2];
            byte[] intTmp = new byte[4];
            System.arraycopy(boot, 0x000B, shortTmp, 0, shortTmp.length);
            bytesPerSector = Transfer.bytes2Short(shortTmp);
            sectorsPerCluster = boot[0x000D];
            System.arraycopy(boot, 0x000E, shortTmp, 0, shortTmp.length);
            reservedSectors = Transfer.bytes2Short(shortTmp);
            numberOfFATCopies = boot[0x0010];
            System.arraycopy(boot, 0x0011, shortTmp, 0, shortTmp.length);
            numberOfPossibleRootEntries = Transfer.bytes2Short(shortTmp);
            System.arraycopy(boot, 0x0013, shortTmp, 0, shortTmp.length);
            smallNumberOfSectors = Transfer.bytes2Short(shortTmp);
            mediaDescriptor = boot[0x0015];
            System.arraycopy(boot, 0x0016, shortTmp, 0, shortTmp.length);
            sectorsPerFAT = Transfer.bytes2Short(shortTmp);
            System.arraycopy(boot, 0x0018, shortTmp, 0, shortTmp.length);
            sectorsPerTrack = Transfer.bytes2Short(shortTmp);
            System.arraycopy(boot, 0x001A, shortTmp, 0, shortTmp.length);
            numberOfHeads = Transfer.bytes2Short(shortTmp);
            System.arraycopy(boot, 0x001C, intTmp, 0, intTmp.length);
            hiddenSectors = Transfer.bytesToInt(intTmp);
            System.arraycopy(boot, 0x0020, intTmp, 0, intTmp.length);
            largeNumberOfSectors = Transfer.bytesToInt(intTmp);
            driveNumber = boot[0x0024];
            reserved = boot[0x0025];
            extendedBootSignature = boot[0x0026];
            System.arraycopy(boot, 0x0027, intTmp, 0, intTmp.length);
            volumeSerialNumber = Transfer.bytesToInt(intTmp);
            System.arraycopy(boot, 0x002B, volumeLabe, 0, volumeLabe.length);
            System.arraycopy(boot, 0x0036, fileSystemType, 0, fileSystemType.length);
            System.arraycopy(boot, 0x003E, bootstrapCode, 0, bootstrapCode.length);
            System.arraycopy(boot, 0x01FE, shortTmp, 0, shortTmp.length);
            bootSectorSignature = Transfer.bytes2Short(shortTmp);
        }

        public BootSector setOemName(byte[] oemName) {
            if(oemName.length > this.oemName.length) {
                throw new IllegalArgumentException("系统名称不能超过4字节");
            }
            System.arraycopy(oemName, 0, this.oemName, 0, oemName.length);
            return this;
        }

        public String getOemName() {
            return new String(oemName).trim();
        }

        public BootSector setFileSystemType(byte[] fileSystemType) {
            if(fileSystemType.length > this.fileSystemType.length) {
                throw new IllegalArgumentException("文件系统类型不能超过8字符");
            }
            System.arraycopy(fileSystemType, 0, this.fileSystemType, 0, fileSystemType.length);
            return this;
        }

        public int getMaxRootEntries() {
            return numberOfPossibleRootEntries * bytesPerSector / 32;
        }

        public byte[] toBytes() {
            byte[] boot = new byte[512];
            System.arraycopy(jumpCode, 0, boot, 0x0000, jumpCode.length);
            System.arraycopy(oemName, 0, boot, 0x0003, oemName.length);
            System.arraycopy(Transfer.short2Bytes(bytesPerSector), 0, boot, 0x000B, 2);
            boot[0x000D] = sectorsPerCluster;
            System.arraycopy(Transfer.short2Bytes(reservedSectors), 0, boot, 0x000E, 2);
            boot[0x0010] = numberOfFATCopies;
            System.arraycopy(Transfer.short2Bytes(numberOfPossibleRootEntries), 0, boot, 0x0011, 2);
            System.arraycopy(Transfer.short2Bytes(smallNumberOfSectors), 0, boot, 0x0013, 2);
            boot[0x0015] = mediaDescriptor;
            System.arraycopy(Transfer.short2Bytes(sectorsPerFAT), 0, boot, 0x0016, 2);
            System.arraycopy(Transfer.short2Bytes(sectorsPerTrack), 0, boot, 0x0018, 2);
            System.arraycopy(Transfer.short2Bytes(numberOfHeads), 0, boot, 0x001A, 2);
            System.arraycopy(Transfer.intToBytes(hiddenSectors), 0, boot, 0x001C, 4);
            System.arraycopy(Transfer.intToBytes(largeNumberOfSectors), 0, boot, 0x0020, 4);
            boot[0x0024] = driveNumber;
            boot[0x0025] = reserved;
            boot[0x0026] = extendedBootSignature;
            System.arraycopy(Transfer.intToBytes(volumeSerialNumber), 0, boot, 0x0027, 4);
            System.arraycopy(volumeLabe, 0, boot, 0x002B, volumeLabe.length);
            System.arraycopy(fileSystemType, 0, boot, 0x0036, fileSystemType.length);
            System.arraycopy(bootstrapCode, 0, boot, 0x003E, bootstrapCode.length);
            System.arraycopy(Transfer.short2Bytes(bootSectorSignature), 0, boot, 0x01FE, 2);
            return boot;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class DirectoryEntry {
        @Builder.Default
        byte[] fileName = new byte[8];
        @Builder.Default
        byte[] fileNameExt = new byte[3];
        byte attribute;
        @Builder.Default
        byte reservedForWindowsNT = 0;
        @Builder.Default
        byte creation = 0;
        int creationTimeStamp;
        short lastAccessDateStamp;
        @Builder.Default
        short reservedForFAT32 = 0;
        int lastWriteTimeStamp;
        short startingCluster;
        int fileSize;

        public DirectoryEntry(byte[] entry) {
            fileName = new byte[8];
            System.arraycopy(entry, 0x00, fileName, 0, fileName.length);
            fileNameExt = new byte[3];
            System.arraycopy(entry, 0x08, fileNameExt, 0, fileNameExt.length);
            attribute = entry[0x0B];
            reservedForWindowsNT = entry[0x0C];
            creation = entry[0x0D];
            byte[] intTmp = new byte[4];
            System.arraycopy(entry, 0x0E, intTmp, 0, intTmp.length);
            creationTimeStamp = Transfer.bytesToInt(intTmp);
            byte[] shortTmp = new byte[2];
            System.arraycopy(entry, 0x12, shortTmp, 0, shortTmp.length);
            lastAccessDateStamp = Transfer.bytes2Short(shortTmp);
            System.arraycopy(entry, 0x14, shortTmp, 0, shortTmp.length);
            reservedForFAT32 = Transfer.bytes2Short(shortTmp);
            System.arraycopy(entry, 0x16, intTmp, 0, intTmp.length);
            lastWriteTimeStamp = Transfer.bytesToInt(intTmp);
            System.arraycopy(entry, 0x1A, shortTmp, 0, shortTmp.length);
            startingCluster = Transfer.bytes2Short(shortTmp);
            System.arraycopy(entry, 0x1C, intTmp, 0, intTmp.length);
            fileSize = Transfer.bytesToInt(intTmp);
        }

        public DirectoryEntry setFileName(String fileName) {
            if(fileName.length() > this.fileName.length) {
                throw new IllegalArgumentException("文件名不能超过8字节");
            }
            if(!checkAscII(fileName)) {
                throw new IllegalArgumentException("非法的文件名，文件名包含A-Z, 0-1, #, $, %, &, ', (, ), -, @之外的字符");
            }
            this.fileName = fileName.getBytes();
            return this;
        }

        public DirectoryEntry setFileNameExt(String fileNameExt) {
            if(fileNameExt.length() > this.fileNameExt.length) {
                throw new IllegalArgumentException("文件名不能超过3字节");
            }
            if(!checkAscII(fileNameExt)) {
                throw new IllegalArgumentException("非法的文件扩展名，文件扩展名包含A-Z, 0-1, #, $, %, &, ', (, ), -, @之外的字符");
            }
            this.fileNameExt = fileNameExt.getBytes();
            return this;
        }

        public String getLaseWriteTime() {
            return new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date(lastWriteTimeStamp * 1000L));
        }

        public boolean isDir() {
            return (attribute & DIR_ATTR) == DIR_ATTR;
        }

        public boolean isFile() {
            return (attribute & DIR_ATTR) != DIR_ATTR;
        }

        public boolean isReadOnly() {
            return (attribute & READ_ONLY_ATTR) == READ_ONLY_ATTR;
        }

        public String getFullName() {
            String name = new String(fileName).trim();
            String ext = new String(fileNameExt).trim();
            if(ext.isEmpty()) {
                return name;
            } else {
                return name + "." + ext;
            }
        }

        public String getHumanReadableFileSize() {
            char[] readable = new char[5];
            Arrays.fill(readable, ' ');

            int fileSize = this.fileSize;
            int unitIdx = 0;
            while (fileSize > 1024) {
                fileSize = fileSize / 1024;
                unitIdx++;
            }
            String fileSizeStr = String.valueOf(fileSize);
            System.arraycopy(String.valueOf(fileSize).toCharArray(), 0, readable, readable.length - fileSizeStr.length() - 1, fileSizeStr.length());
            System.arraycopy(FILE_SIZE_UNITS, unitIdx, readable, readable.length - 1, 1);
            return new String(readable);
        }

        public boolean isEmpty() {
            return new String(fileName).trim().isEmpty();
        }

        public boolean equals(DirectoryEntry entry) {
            return getFullName().equals(entry.getFullName());
        }

        public byte[] toBytes() {
            byte[] entry = new byte[32];
            System.arraycopy(fileName, 0, entry, 0x00, fileName.length);
            System.arraycopy(fileNameExt, 0, entry, 0x08, fileNameExt.length);
            entry[0x0B] = attribute;
            entry[0x0C] = reservedForWindowsNT;
            entry[0x0D] = creation;
            System.arraycopy(Transfer.intToBytes(creationTimeStamp), 0, entry, 0x0E, 4);
            System.arraycopy(Transfer.short2Bytes(lastAccessDateStamp), 0, entry, 0x12, 2);
            System.arraycopy(Transfer.short2Bytes(reservedForFAT32), 0, entry, 0x14, 2);
            System.arraycopy(Transfer.intToBytes(lastWriteTimeStamp), 0, entry, 0x16, 4);
            System.arraycopy(Transfer.short2Bytes(startingCluster), 0, entry, 0x1A, 2);
            System.arraycopy(Transfer.intToBytes(fileSize), 0, entry, 0x1C, 4);
            return entry;
        }

        private boolean checkAscII(String name) {
            String regex = "[A-Za-z0-1#$%&'()\\-@]" + "{" + name.length() + "}+";
            return name.matches(regex);
        }
    }
}
