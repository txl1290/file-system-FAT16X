package command;

import command.base.Base;
import dirven.DiskDriven;
import picocli.CommandLine;

@CommandLine.Command(name = "mkdir", mixinStandardHelpOptions = true, description = "make a directory")
public class Mkdir extends Base {

    @CommandLine.Parameters(index = "0", description = "The directory path")
    private String dirPath;

    @Override
    protected String executeCommand() {
        DiskDriven.makeDirectory(DiskDriven.getAbsolutePath(dirPath));
        return "";
    }
}
