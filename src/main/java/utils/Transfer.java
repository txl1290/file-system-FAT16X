package utils;

import protocol.FAT16X;

import java.util.ArrayList;
import java.util.List;

public class Transfer {

    public static byte[] short2Bytes(short a) {
        byte[] b = new byte[2];
        b[1] = (byte) (a & 0xff);
        b[0] = (byte) (a >> 8 & 0xff);
        return b;
    }

    public static short bytes2Short(byte[] b) {
        return (short) (((b[0] & 0xff) << 8) | b[1] & 0xff);
    }

    public static short bytes2Short(byte b1, byte b2) {
        return (short) (((b1 & 0xff) << 8) | b2 & 0xff);
    }

    public static byte[] intToBytes(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    public static int bytesToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static List<FAT16X.DirectoryEntry> bytesToEntries(byte[] data) {
        List<FAT16X.DirectoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < data.length; i += FAT16X.ENTRY_SIZE) {
            byte[] entryData = new byte[FAT16X.ENTRY_SIZE];
            System.arraycopy(data, i, entryData, 0, FAT16X.ENTRY_SIZE);
            FAT16X.DirectoryEntry entry = new FAT16X.DirectoryEntry(entryData);
            if(entry.isEmpty()) {
                break;
            }
            entries.add(entry);
        }
        return entries;
    }
}
