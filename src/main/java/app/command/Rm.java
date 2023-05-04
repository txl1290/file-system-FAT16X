package app.command;

import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;

@CommandLine.Command(name = "rm", mixinStandardHelpOptions = true, description = "remove a file or directory")
public class Rm extends Base {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    public Rm(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() {
        File file = new File(getAbsolutePath(path));

        file.remove();
    }

}
