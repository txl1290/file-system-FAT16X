package fs.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class VFATX {

    public static final int LFN_ENTRY_NAME_SIZE = 30;

    public static final int LFN_ENTRY_SIZE = 32;

    public static final int LFN_ENTRY_COUNT = 3;

    public static final byte LFN_ENTRY_ATTRIBUTE = 0x0F;

    public static final byte LAST_LFN_ENTRY_ORDINAL = 0x40;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LfnEntry {
        public byte ordinalField;
        @Builder.Default
        byte[] part1 = new byte[10];
        @Builder.Default
        public byte attribute = 0x0F;
        @Builder.Default
        byte[] part2 = new byte[20];

        public LfnEntry(byte[] data) {
            ordinalField = data[0];
            part1 = new byte[10];
            System.arraycopy(data, 1, part1, 0, 10);
            attribute = data[11];
            part2 = new byte[20];
            System.arraycopy(data, 12, part2, 0, 20);
        }

        public boolean isDeleted() {
            return (ordinalField & 0x80) == 0x80;
        }

        public boolean isLast() {
            return (ordinalField & 0x40) == 0x40;
        }

        public String getEntryName() {
            byte[] name = new byte[LFN_ENTRY_NAME_SIZE];
            System.arraycopy(part1, 0, name, 0, part1.length);
            System.arraycopy(part2, 0, name, 10, part2.length);
            return new String(name).trim();
        }

        public byte[] toBytes() {
            byte[] data = new byte[LFN_ENTRY_SIZE];
            data[0] = ordinalField;
            System.arraycopy(part1, 0, data, 1, part1.length);
            data[11] = attribute;
            System.arraycopy(part2, 0, data, 12, part2.length);
            return data;
        }
    }
}
