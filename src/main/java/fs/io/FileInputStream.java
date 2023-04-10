package fs.io;

import fs.fat.FAT16X;
import fs.fat.Fd;
import utils.Transfer;

import java.util.List;

public class FileInputStream {

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
    }

    public void close() {
        File.fs.close(fd);
    }

    public String read() {
        int size = fd.getEntry().getFileSize();
        byte[] buf = new byte[size];
        File.fs.read(fd, buf, size);
        return new String(buf);
    }

    public List<File> listFiles() {
        List<FAT16X.DirectoryEntry> entries = File.fs.listFiles(fd);
        return Transfer.convertEntriesToFiles(entries);
    }
}
