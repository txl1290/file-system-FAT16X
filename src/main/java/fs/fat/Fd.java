package fs.fat;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Fd {

    private FAT16X.DirectoryEntry entry;
    private int currentCluster;
    private int currentSector;
    private int offset;

    private Fd parentFd;

    @Builder.Default
    private boolean closed = false;

    public void close() {
        closed = true;
    }

    public boolean valid() {
        return !closed;
    }
}
