package fs.fat;

import fs.protocol.FAT16X;
import fs.protocol.VFATX;
import lombok.Builder;
import lombok.Data;

import java.nio.charset.StandardCharsets;

@Data
@Builder
public class MixedEntry {

    private VFATX.LfnEntry[] lfnEntries;
    private FAT16X.DirectoryEntry directoryEntry;

    public MixedEntry(VFATX.LfnEntry[] lfnEntries, FAT16X.DirectoryEntry directoryEntry) {
        this.lfnEntries = lfnEntries;
        this.directoryEntry = directoryEntry;
    }

    public MixedEntry setFileName(String fileName) {
        if(fileName.length() > VFATX.LFN_ENTRY_COUNT * VFATX.LFN_ENTRY_NAME_SIZE) {
            throw new IllegalArgumentException("File name too long");
        }

        if(directoryEntry.isLongFileName(fileName)) {
            lfnEntries = new VFATX.LfnEntry[VFATX.LFN_ENTRY_COUNT];
            byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < lfnEntries.length; i++) {
                int startIndex = i * VFATX.LFN_ENTRY_NAME_SIZE;
                byte[] part1 = new byte[10];
                byte[] part2 = new byte[20];
                if(startIndex < fileNameBytes.length) {
                    System.arraycopy(fileNameBytes, startIndex, part1, 0, Math.min(10, fileNameBytes.length - startIndex));
                }
                if(startIndex + 10 < fileNameBytes.length) {
                    System.arraycopy(fileNameBytes, startIndex + 10, part2, 0, Math.min(20, fileNameBytes.length - startIndex - 10));
                }

                VFATX.LfnEntry lfnEntry = VFATX.LfnEntry.builder()
                        .ordinalField((byte) (i + 1))
                        .part1(part1)
                        .attribute(VFATX.LFN_ENTRY_ATTRIBUTE)
                        .part2(part2)
                        .build();
                if(i == lfnEntries.length - 1) {
                    lfnEntry.setOrdinalField(VFATX.LAST_LFN_ENTRY_ORDINAL);
                }
                lfnEntries[VFATX.LFN_ENTRY_COUNT - i - 1] = lfnEntry;
            }
        } else {
            directoryEntry.setFileName(fileName);
        }
        return this;
    }

    public MixedEntry setFileNameExt(String extension) {
        directoryEntry.setFileNameExt(extension);
        return this;
    }

    public String getFullName() {
        if(lfnEntries == null) {
            return directoryEntry.getFullName();
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = lfnEntries.length - 1; i >= 0; i--) {
                sb.append(lfnEntries[i].getEntryName());
            }
            String ext = new String(directoryEntry.getFileNameExt()).trim();
            if(!ext.isEmpty()) {
                sb.append(".").append(ext);
            }
            return sb.toString();
        }
    }

    public boolean isDir() {
        return directoryEntry.isDir();
    }

    public boolean isFile() {
        return directoryEntry.isFile();
    }

    public boolean isReadOnly() {
        return directoryEntry.isReadOnly();
    }

    public int getFileSize() {
        return directoryEntry.getFileSize();
    }

    public int getLastWriteTimeStamp() {
        return directoryEntry.getLastWriteTimeStamp();
    }

    public short getStartingCluster() {
        return directoryEntry.getStartingCluster();
    }

    public void setStartingCluster(short startingCluster) {
        directoryEntry.setStartingCluster(startingCluster);
    }

    public void setFileSize(int fileSize) {
        directoryEntry.setFileSize(fileSize);
    }

    public void setLastAccessDateStamp(short lastAccessDateStamp) {
        directoryEntry.setLastAccessDateStamp(lastAccessDateStamp);
    }

    public void setLastWriteTimeStamp(int lastWriteTimeStamp) {
        directoryEntry.setLastWriteTimeStamp(lastWriteTimeStamp);
    }

    public byte[] toBytes() {
        if(lfnEntries == null) {
            return directoryEntry.toBytes();
        } else {
            byte[] data = new byte[4 * FAT16X.ENTRY_SIZE];
            for (int i = 0; i < lfnEntries.length; i++) {
                System.arraycopy(lfnEntries[i].toBytes(), 0, data, i * VFATX.LFN_ENTRY_SIZE, VFATX.LFN_ENTRY_SIZE);
            }
            System.arraycopy(directoryEntry.toBytes(), 0, data, lfnEntries.length * VFATX.LFN_ENTRY_SIZE,
                    FAT16X.ENTRY_SIZE);
            return data;
        }
    }
}
