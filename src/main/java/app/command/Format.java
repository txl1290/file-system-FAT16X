package app.command;

import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "format", mixinStandardHelpOptions = true, description = "format the file system")
public class Format extends Base {

    public Format(String curDir) {
        super(curDir);
    }

    @Override
    public void run() {
        File.fs.format();
        try {
            writeOut("file system format success");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
