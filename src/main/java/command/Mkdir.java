package command;

import dirven.DiskDriven;
import picocli.CommandLine;

@CommandLine.Command(name = "mkdir", mixinStandardHelpOptions = true, description = "make a directory")
public class Mkdir implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The directory path")
    private String dirPath;

    @Override
    public void run() {
        try {
            DiskDriven.makeDirectory(DiskDriven.getAbsolutePath(dirPath));
        } catch (Exception e) {
            System.out.println("mkdir: " + e.getMessage());
        }
    }
}
