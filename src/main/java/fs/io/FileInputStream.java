package fs.io;

import fs.fat.Fd;
import fs.fat.MixedEntry;
import utils.Transfer;

import java.io.InputStream;
import java.util.List;

public class FileInputStream extends InputStream {

    private final File file;

    private final Fd fd;

    private final String path;

    public FileInputStream(File file) {
        this.file = file;
        this.path = file.getPath();
        fd = File.fs.open(path);

        // 检查File和Fd的类型是否一致
        if(file.isDirectory() && !fd.getEntry().isDir()) {
            throw new RuntimeException(path + " is not a directory");
        } else if(file.isFile() && !fd.getEntry().isFile()) {
            throw new RuntimeException(path + " is a directory");
        }
        file.setFileSize(fd.getEntry().getFileSize());
    }

    @Override
    public int read() {
        byte[] b = new byte[1];
        File.fs.read(fd, b, 1);
        return b[0];
    }

    private int readBytes(byte[] b, int off, int len) {
        synchronized(fd) {
            File.fs.setFdOffset(fd, off);
            File.fs.read(fd, b, len);
            return b.length;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) {
        return readBytes(b, off, len);
    }

    @Override
    public void close() {
        File.fs.close(fd);
    }

    public List<File> listFiles() {
        List<MixedEntry> entries = File.fs.listFiles(fd);
        return Transfer.convertMixEntriesToFiles(entries);
    }
}
