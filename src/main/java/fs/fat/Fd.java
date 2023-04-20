package fs.fat;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Fd {

    private FAT16X.DirectoryEntry entry;

    /**
     * todo：按照stream隔离，现在每次都需要reset才能保证多条流的正确性
     */
    private int currentCluster;
    private int currentSector;
    private int offset;

    private Fd parentFd;

    private int refCount = 0;

    @Builder.Default
    private boolean closed = false;

    public void close() {
        refCount--;
        if(refCount == 0) {
            closed = true;
        }
    }

    public boolean valid() {
        return !closed;
    }

    public void addRef() {
        refCount++;
    }
}
