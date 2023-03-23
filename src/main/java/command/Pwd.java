package command;

import command.base.Base;
import dirven.DiskDriven;
import picocli.CommandLine;

@CommandLine.Command(name = "pwd", mixinStandardHelpOptions = true, description = "show current path")
public class Pwd extends Base {

    @Override
    protected String executeCommand() {
        return DiskDriven.getCurrentPath();
    }
}
