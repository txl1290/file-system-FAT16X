package utils;

import dirven.DiskDriven;
import protocol.FAT16X;

import java.util.Arrays;

public class FsHelper {

    public static int getSectorIdx(int clusterIdx, int offset) {
        FAT16X.BootSector bootSector = DiskDriven.getDisk().getFs().getBootSector();
        int addr = clusterIdx * bootSector.getSectorsPerCluster() * bootSector.getBytesPerSector() + offset;
        if(clusterIdx == 0) {
            // 首个cluster需要加上BootSector大小
            addr += DiskDriven.getDisk().sectorSize();
        }
        return addr / DiskDriven.getDisk().sectorSize();
    }

    public static byte[] concat(byte[] first, int offset, byte[] second) {
        System.arraycopy(second, 0, first, offset, second.length);
        return first;
    }

    public static FAT16X.DirectoryEntry[] addEntry(FAT16X.DirectoryEntry[] first, FAT16X.DirectoryEntry second) {
        FAT16X.DirectoryEntry[] result = Arrays.copyOf(first, first.length + 1);
        result[first.length] = second;
        return result;
    }

    /**
     * 判断是否是空数据区，规则：如果全部是0x00的话，则说明这片区域没写过值
     */
    public static boolean isEmpty(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if(data[i] != FAT16X.EMPTY_BYTE) {
                return false;
            }
        }
        return true;
    }
}
