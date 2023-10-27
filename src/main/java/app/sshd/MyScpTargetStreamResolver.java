package app.sshd;

import org.apache.sshd.common.scp.ScpFileOpener;
import org.apache.sshd.common.scp.ScpTargetStreamResolver;
import org.apache.sshd.common.scp.ScpTimestamp;
import org.apache.sshd.common.session.Session;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class MyScpTargetStreamResolver implements ScpTargetStreamResolver {

    protected final Path path;

    protected final ScpFileOpener opener;

    public MyScpTargetStreamResolver(Path path, ScpFileOpener opener) {
        this.path = path;
        this.opener = opener;
    }

    @Override 
    public OutputStream resolveTargetStream(Session session, String s, long l, Set<PosixFilePermission> set, OpenOption... openOptions)
            throws IOException {
        // 接收流，写入到path对应的FATFS文件中
        Path filePath = path.resolve(s);
        return opener.openWrite(session, filePath, l, set, openOptions);
    }

    @Override 
    public Path getEventListenerFilePath() {
        return null;
    }

    @Override 
    public void postProcessReceivedData(String s, boolean b, Set<PosixFilePermission> set, ScpTimestamp scpTimestamp) {

    }
}
