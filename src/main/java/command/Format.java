package command;

import dirven.DiskDriven;
import picocli.CommandLine;

@CommandLine.Command(name = "format", mixinStandardHelpOptions = true, description = "format the file system")
public class Format implements Runnable {

    @Override
    public void run() {
        DiskDriven.format();
        System.out.println("file system format success");
    }
}
