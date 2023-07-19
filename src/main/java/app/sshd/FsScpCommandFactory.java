package app.sshd;

import app.command.Mkdir;
import app.command.base.Base;
import org.apache.sshd.common.scp.ScpFileOpener;
import org.apache.sshd.common.scp.ScpSourceStreamResolver;
import org.apache.sshd.common.scp.ScpTargetStreamResolver;
import org.apache.sshd.common.scp.ScpTransferEventListener;
import org.apache.sshd.common.scp.helpers.LocalFileScpTargetStreamResolver;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.scp.ScpCommandFactory;
import picocli.CommandLine;
import utils.InputParser;
import utils.StreamUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class FsScpCommandFactory extends ScpCommandFactory {

    public static final String TMP_DIR_PATH = System.getProperty("java.io.tmpdir");

    public FsScpCommandFactory() {
        super();
        addEventListener(new MyServerSideScpTransferEventListener());
        setScpFileOpener(new MyScpFileOpener());
    }

    public static class MyServerSideScpTransferEventListener implements ScpTransferEventListener {

        @Override
        public void endFileEvent(Session session, FileOperation op, Path file, long length, Set<PosixFilePermission> perms,
                Throwable thrown) throws IOException {
            FileInputStream fis = new FileInputStream(file.toFile());

            // fileName中剔除TMP_DIR_PATH，取到真正的绝对路径
            String filePath = file.toAbsolutePath().toString().replace(TMP_DIR_PATH, "/");

            String dirPath = InputParser.getFileParentPath(filePath);
            // 创建目录
            CommandLine commandLine = new CommandLine(new Mkdir("/"));
            commandLine.execute(dirPath, "-p");

            // 使用inputStream进行覆盖重定向操作，生成对应的文件
            Base redirectCommand = new Base(filePath);
            redirectCommand.setRedirectPath(filePath);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StreamUtil.copyStream(fis, out);
            redirectCommand.setOut(out);
            redirectCommand.redirect();
            out.close();

            // 删除临时区file
            Files.delete(file.toAbsolutePath());
        }
    }

    public static class MyScpFileOpener implements ScpFileOpener {

        @Override
        public InputStream openRead(Session session, Path path, long length, Set<PosixFilePermission> permissions, OpenOption... options) {
            // 暂不实现
            return null;
        }

        @Override
        public ScpSourceStreamResolver createScpSourceStreamResolver(Session session, Path path) {
            // 暂不实现
            return null;
        }

        @Override
        public OutputStream openWrite(Session session, Path path, long length, Set<PosixFilePermission> permissions, OpenOption... options)
                throws IOException {
            // 实现具体的写入逻辑
            return Files.newOutputStream(path, options);
        }

        @Override
        public ScpTargetStreamResolver createScpTargetStreamResolver(Session session, Path path) throws IOException {
            // 创建一个自定义的 ScpTargetStreamResolver，并返回它
            return new LocalFileScpTargetStreamResolver(path, this);
        }

        @Override
        public Path resolveIncomingReceiveLocation(Session session, Path path, boolean recursive, boolean shouldBeDir, boolean preserve) {
            return Paths.get(TMP_DIR_PATH, path.toString());
        }
    }

}
