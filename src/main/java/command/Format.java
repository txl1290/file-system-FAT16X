package command;

import command.base.Base;
import dirven.DiskDriven;
import picocli.CommandLine;

@CommandLine.Command(name = "format", mixinStandardHelpOptions = true, description = "format the file system")
public class Format extends Base {

    @Override
    public void run() {
        DiskDriven.format();
        System.out.println("file system format success");
    }
}
