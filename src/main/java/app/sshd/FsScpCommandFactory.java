package app.sshd;

import app.command.Mkdir;
import app.command.base.Base;
import fs.fat.Fd;
import fs.io.File;
import fs.io.FileOutputStream;
import org.apache.sshd.client.scp.DefaultScpStreamResolver;
import org.apache.sshd.common.scp.ScpFileOpener;
import org.apache.sshd.common.scp.ScpSourceStreamResolver;
import org.apache.sshd.common.scp.ScpTargetStreamResolver;
import org.apache.sshd.common.scp.ScpTimestamp;
import org.apache.sshd.common.scp.ScpTransferEventListener;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FsScpCommandFactory extends ScpCommandFactory {

    public FsScpCommandFactory() {
        super();
        setScpFileOpener(new MyScpFileOpener());
    }

    public static class MyScpFileOpener implements ScpFileOpener {

        @Override
        public Path resolveOutgoingFilePath(Session session, Path localPath, LinkOption... options) throws IOException {
            Fd fd = File.fs.findFd(localPath.toString());
            if(fd == null) {
                throw new IOException(localPath + ": no such file or directory");
            } else {
                return localPath;
            }
        }

        @Override
        public boolean sendAsRegularFile(Session session, Path path, LinkOption... options) {
            Fd fd = File.fs.findFd(path.toString());
            return fd.getEntry().isFile();
        }

        @Override
        public boolean sendAsDirectory(Session session, Path path, LinkOption... options) {
            Fd fd = File.fs.findFd(path.toString());
            return fd.getEntry().isDir();
        }

        @Override
        public Set<PosixFilePermission> getLocalFilePermissions(Session session, Path path, LinkOption... options) {
            return EnumSet.allOf(PosixFilePermission.class);
        }

        @Override
        public DirectoryStream<Path> getLocalFolderChildren(Session session, Path path) {
            return new MyDirectoryStream(path);
        }

        @Override
        public InputStream openRead(Session session, Path path, long length, Set<PosixFilePermission> permissions, OpenOption... options) {
            // 暂不实现
            return null;
        }

        @Override
        public ScpSourceStreamResolver createScpSourceStreamResolver(Session session, Path path) {
            fs.io.FileInputStream inputStream = new fs.io.FileInputStream(new File(path.toString()));
            ScpTimestamp time = new ScpTimestamp(System.currentTimeMillis(), System.currentTimeMillis());
            return new DefaultScpStreamResolver(path.getFileName().toString(), path, EnumSet.noneOf(PosixFilePermission.class), time,
                    inputStream.getSize(), inputStream, "scp");
        }

        @Override
        public OutputStream openWrite(Session session, Path path, long length, Set<PosixFilePermission> permissions, OpenOption... options) {
            // 实现具体的写入逻辑
            String dirPath = InputParser.getFileParentPath(path.toString());
            // 创建目录
            CommandLine commandLine = new CommandLine(new Mkdir("/"));
            commandLine.execute(dirPath, "-p");
            
            // 创建文件
            File file = new File(path.toString());
            if(file.exist()) {
                file.remove();
            }
            file.create();
            
            return new FileOutputStream(file);
        }

        @Override
        public ScpTargetStreamResolver createScpTargetStreamResolver(Session session, Path path) throws IOException {
            // 创建一个自定义的 ScpTargetStreamResolver，并返回它
            return new MyScpTargetStreamResolver(path, this);
        }

        @Override
        public Path resolveIncomingReceiveLocation(Session session, Path path, boolean recursive, boolean shouldBeDir, boolean preserve) {
            return path;
        }
        
        @Override
        public Path resolveIncomingFilePath(Session session, Path localPath, String name, boolean preserve, Set<PosixFilePermission> permissions, ScpTimestamp time) {
            return localPath.resolve(name);
        }
        
    }

    public static class MyDirectoryStream implements DirectoryStream<Path> {

        private Path path;

        MyDirectoryStream(Path path) {
            this.path = path;
        }

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub
        }

        @Override
        public Iterator<Path> iterator() {
            File dir = new File(path.toString(), true, false);
            fs.io.FileInputStream inputStream = new fs.io.FileInputStream(dir);
            List<File> files = inputStream.listFiles();

            return new Iterator<Path>() {

                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < files.size();
                }

                @Override
                public Path next() {
                    return Paths.get(files.get(index++).getPath());
                }
            };
        }

    }

}
