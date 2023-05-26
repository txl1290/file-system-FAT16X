package fs.io;

import fs.fat.Fd;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileOutputStream extends OutputStream {

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

    @Override
    public void write(int b) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) b;
        writeBytes(bytes, 0, 1, append);
    }

    private void writeBytes(byte[] b, int off, int len, boolean append) {
        synchronized(fd) {
            if(append) {
                int offset = fd.getEntry().getFileSize();
                File.fs.write(fd, b, offset, len);
            } else {
                // 重置文件大小为偏移量
                fd.getEntry().setFileSize(off);
                File.fs.write(fd, b, off, len);
            }
        }
    }

    @Override
    public void close() {
        File.fs.close(fd);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        writeBytes(b, off, len, append);
    }

    public void write(String s) {
        write(s.getBytes(StandardCharsets.UTF_8), 0, s.length());
    }
}
