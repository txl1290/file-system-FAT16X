package app.command;

import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;

@CommandLine.Command(name = "mkdir", mixinStandardHelpOptions = true, description = "make a directory")
public class Mkdir extends Base {

    @CommandLine.Parameters(index = "0", description = "The directory path")
    private String dirPath;

    public Mkdir(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() {
        File dir = new File(getAbsolutePath(dirPath), true, false);
        if(dir.exist()) {
            throw new IllegalArgumentException("mkdir: cannot create directory '" + dirPath + "': File already exists");
        }

        dir.mkdir();
    }
}
