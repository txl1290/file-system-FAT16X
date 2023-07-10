package fs.fat;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Fd {

    private MixedEntry entry;

    private int currentCluster;
    private int currentSector;
    private int offset;

    private Fd parentFd;

    @Builder.Default
    private boolean closed = false;

    public void close() {

    }

    public boolean valid() {
        return !closed;
    }
}
