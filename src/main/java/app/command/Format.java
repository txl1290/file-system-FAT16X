package app.command;

import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;

@CommandLine.Command(name = "format", mixinStandardHelpOptions = true, description = "format the file system")
public class Format extends Base {

    public Format(String curDir) {
        super(curDir);
    }

    @Override
    public void run() {
        File.fs.format();
        out = "file system format success";
    }
}
