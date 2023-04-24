package fs.io;

import fs.fat.Fd;

import java.nio.charset.StandardCharsets;

public class FileOutputStream {

    private final File file;

    private final Fd fd;

    private final String path;

    private final boolean append;

    public FileOutputStream(File file) {
        this(file, false);
    }

    public FileOutputStream(File file, boolean append) {
        this.file = file;
        this.path = file.getPath();
        this.append = append;
        fd = File.fs.open(path);

        // 检查File和Fd的类型是否一致
        if(file.isDirectory() && !fd.getEntry().isDir()) {
            throw new RuntimeException(path + " is not a directory");
        } else if(file.isFile() && !fd.getEntry().isFile()) {
            throw new RuntimeException(path + " is a directory");
        }
    }

    public void close() {
        File.fs.close(fd);
    }

    public void write(String s) {
        if(append) {
            int offset = fd.getEntry().getFileSize();
            File.fs.write(fd, s.getBytes(StandardCharsets.UTF_8), offset, s.length());
        } else {
            // 清空文件
            fd.getEntry().setFileSize(0);
            File.fs.write(fd, s.getBytes(StandardCharsets.UTF_8), 0, s.length());
        }
    }
}
