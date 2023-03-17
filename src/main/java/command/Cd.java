package command;

import dirven.DiskDriven;
import picocli.CommandLine;
import utils.InputParser;

@CommandLine.Command(name = "cd", mixinStandardHelpOptions = true, description = "enter into a directory")
public class Cd implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The directory path")
    private String dirPath;

    @Override
    public void run() {
        try {
            String absolutePath = DiskDriven.getAbsolutePath(dirPath);
            if(!InputParser.isRoot(absolutePath) && DiskDriven.findEntry(absolutePath) == null) {
                throw new IllegalArgumentException("No such file or directory: " + dirPath);
            }
            DiskDriven.setCurrentPath(absolutePath);
        } catch (Exception e) {
            System.out.println("cd: " + e.getMessage());
        }
    }
}
