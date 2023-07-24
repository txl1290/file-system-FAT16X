package utils;

import fs.fat.MixedEntry;
import fs.io.File;
import fs.protocol.FAT16X;
import fs.protocol.VFATX;

import java.io.ByteArrayOutputStream;
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

    public static List<MixedEntry> bytesToMixEntries(byte[] data, List<Byte> leftData) {
        List<MixedEntry> entries = new ArrayList<>();
        leftData.clear();
        for (int i = 0; i < data.length; i += FAT16X.ENTRY_SIZE) {
            byte[] entryData = new byte[FAT16X.ENTRY_SIZE];
            VFATX.LfnEntry[] lfnEntries = null;
            FAT16X.DirectoryEntry directoryEntry;
            System.arraycopy(data, i, entryData, 0, FAT16X.ENTRY_SIZE);

            if(FsHelper.isEmpty(entryData)) {
                break;
            }

            byte attribute = entryData[11];
            if(attribute == VFATX.LFN_ENTRY_ATTRIBUTE) {
                lfnEntries = new VFATX.LfnEntry[VFATX.LFN_ENTRY_COUNT];
            }

            // 还原长文件名结构
            int index = 0;
            boolean isComplete = true;
            while (attribute == VFATX.LFN_ENTRY_ATTRIBUTE) {
                lfnEntries[index++] = new VFATX.LfnEntry(entryData);
                if(i + FAT16X.ENTRY_SIZE >= data.length) {
                    isComplete = false;
                    break;
                }
                i += FAT16X.ENTRY_SIZE;
                System.arraycopy(data, i, entryData, 0, FAT16X.ENTRY_SIZE);
                attribute = entryData[11];
            }

            // 磁盘块上的数据不足以还原长文件名，留着下次读取
            if(isComplete) {
                directoryEntry = new FAT16X.DirectoryEntry(entryData);
                MixedEntry entry = new MixedEntry(lfnEntries, directoryEntry);
                entries.add(entry);
            } else {
                for (int j = data.length - FAT16X.ENTRY_SIZE * index; j < data.length; j++) {
                    leftData.add(data[j]);
                }
            }
        }
        return entries;
    }

    public static byte[] mixEntriesToBytes(List<MixedEntry> entries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (MixedEntry entry : entries) {
                baos.write(entry.toBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public static int short2Int(short a) {
        return a & 0xffff;
    }

    public static List<File> convertMixEntriesToFiles(List<MixedEntry> entries) {
        List<File> files = new ArrayList<>();
        for (MixedEntry entry : entries) {
            files.add(convertMixEntryToFile(entry));
        }
        return files;
    }

    public static File convertMixEntryToFile(MixedEntry entry) {
        return File.builder()
                .name(entry.getFullName().trim())
                .isDirectory(entry.isDir())
                .isFile(entry.isFile())
                .isReadOnly(entry.isReadOnly())
                .fileSize(entry.getFileSize())
                .lastWriteTimeStamp(entry.getLastWriteTimeStamp())
                .build();
    }
}
