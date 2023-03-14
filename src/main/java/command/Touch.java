package command;

import dirven.DiskDriven;
import picocli.CommandLine;

@CommandLine.Command(name = "touch", mixinStandardHelpOptions = true, description = "create a file")
public class Touch implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    @Override
    public void run() {
        try {
            DiskDriven.createFile(DiskDriven.getAbsolutePath(path), 0);
        } catch (Exception e) {
            System.out.println("touch: " + e.getMessage());
        }
    }
}
