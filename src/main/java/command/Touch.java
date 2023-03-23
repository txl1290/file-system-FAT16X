package command;

import command.base.Base;
import dirven.DiskDriven;
import picocli.CommandLine;

@CommandLine.Command(name = "touch", mixinStandardHelpOptions = true, description = "create a file")
public class Touch extends Base {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    @Override
    protected String executeCommand() {
        DiskDriven.createFile(DiskDriven.getAbsolutePath(path), 0);
        return "";
    }
}
