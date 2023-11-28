import app.FsServer;
import app.FsShell;
import app.application.Installer;
import device.Disk;
import fs.io.File;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FsTerminal {

    private static final int PORT = 2022;

    // 设备层 + 文件系统层初始化
    static {
        java.io.File file = new java.io.File("disk");
        try {
            if(!file.exists()) {
                file.createNewFile();
            }

            // 挂载磁盘
            RandomAccessFile fw = new RandomAccessFile(file, "rwd");
            fw.setLength(2L * 1024 * 1024 * 1024);
            Disk disk = new Disk(fw);
            File.fs.mount(disk);

            // 安装内置应用
            Installer installer = new Installer();
            installer.installInnerApp();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        FsServer server = new FsServer();
        server.listen(PORT);

        FsShell shell = new FsShell();
        shell.setInputStream(System.in);
        shell.setOutputStream(System.out);
        shell.setErrorStream(System.out);
        shell.setExitCallback((status, msg) -> System.exit(status));

        shell.run();

        server.close();
    }
}
