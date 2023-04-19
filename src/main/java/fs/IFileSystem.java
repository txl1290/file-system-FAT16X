package fs;

import fs.fat.FAT16X;
import fs.fat.Fd;

import java.io.IOException;
import java.util.List;

public interface IFileSystem {

    public Fd open(String path);

    public void close(Fd fd);

    public void read(Fd fd, byte[] buf, int len);

    public void write(Fd fd, byte[] buf, int len) throws IOException;

    public List<FAT16X.DirectoryEntry> listFiles(Fd fd);

    public void appendFile(String path, boolean isDir);

    public void removeFile(String path);
}
