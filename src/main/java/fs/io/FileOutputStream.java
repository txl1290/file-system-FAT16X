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
            write(s, offset, s.length());
        } else {
            // 清空文件
            fd.getEntry().setFileSize(0);
            write(s, 0, s.length());
        }
    }

    private void write(String s, int off, int len) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);

        int startCluster = fd.getEntry().getStartingCluster();

        // 计算偏移的cluster、sector和offset
        int sectorOffset = off / File.fs.sectorSize();
        int offset = off % File.fs.sectorSize();
        int clusterOffset = sectorOffset / File.fs.sectorsPerCluster();
        int nextCluster = startCluster;
        while (clusterOffset > 0) {
            // 需要找到下一个cluster
            nextCluster = File.fs.getNextCluster(nextCluster);
            if(nextCluster == -1) {
                // 偏移量为文件尾
                sectorOffset = File.fs.sectorsPerCluster() - 1;
                offset = File.fs.sectorSize();
                break;
            } else {
                fd.setCurrentCluster(nextCluster);
                clusterOffset--;
                sectorOffset -= File.fs.sectorsPerCluster();
            }
        }
        fd.setCurrentSector(fd.getCurrentCluster() * File.fs.sectorsPerCluster() + sectorOffset);
        fd.setOffset(offset);
        File.fs.write(fd, b, len);
    }
}
