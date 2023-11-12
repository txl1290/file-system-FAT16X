package app.command;

import app.application.Installer;
import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "format", mixinStandardHelpOptions = true, description = "format the file system")
public class Format extends Base {

    @Override
    public void run() {
        File.fs.format();
        // 初始化后，安装内置应用
        Installer installer = new Installer();
        installer.installInnerApp();
        try {
            writeOut("file system format success");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
