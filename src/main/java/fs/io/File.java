package fs.io;

import fs.fat.FatFileSystem;
import fs.fat.Fd;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
public class File {

    public static final FatFileSystem fs = new FatFileSystem();

    private static final char[] FILE_SIZE_UNITS = new char[] { 'B', 'K', 'M', 'G', 'T' };

    private final String path;

    private String name;

    private int fileSize;

    int lastWriteTimeStamp;

    private boolean isDirectory = false;

    private boolean isFile = true;

    private boolean isReadOnly = false;

    public File(String path) {
        this.path = path;
    }

    public File(String path, boolean isDirectory, boolean isFile) {
        this.path = path;
        this.isDirectory = isDirectory;
        this.isFile = isFile;
    }

    public boolean exist() {
        Fd fd = fs.findFd(path);
        return fd != null && isDirectory == fd.getEntry().isDir() && isFile == fd.getEntry().isFile();
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isFile() {
        return isFile;
    }

    public void mkdir() {
        if(!isDirectory) {
            throw new RuntimeException("File class is not a directory");
        }
        fs.appendFile(path, true);
    }

    public void create() {
        if(!isFile) {
            throw new RuntimeException("File class is not a file");
        }
        fs.appendFile(path, false);
    }

    public String getHumanReadableFileSize() {
        char[] readable = new char[5];
        Arrays.fill(readable, ' ');

        int size = fileSize;
        int unitIdx = 0;
        while (size > 1024) {
            size = size / 1024;
            unitIdx++;
        }
        String fileSizeStr = String.valueOf(size);
        System.arraycopy(String.valueOf(size).toCharArray(), 0, readable, readable.length - fileSizeStr.length() - 1,
                fileSizeStr.length());
        System.arraycopy(FILE_SIZE_UNITS, unitIdx, readable, readable.length - 1, 1);
        return new String(readable);
    }

    public String getLastWriteTime() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date(lastWriteTimeStamp * 1000L));
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void remove() {
        fs.removeFile(path);
    }
}
