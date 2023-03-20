package command;

import dirven.DiskDriven;
import picocli.CommandLine;

@CommandLine.Command(name = "pwd", mixinStandardHelpOptions = true, description = "show current path")
public class Pwd implements Runnable {

    @CommandLine.Option(names = ">", description = "The redirect path")
    private String redirectPath;

    @Override
    public void run() {
        if(redirectPath == null) {
            System.out.println(DiskDriven.getCurrentPath());
        } else {
            DiskDriven.writeFileContent(redirectPath, DiskDriven.getCurrentPath().getBytes());
        }
    }
}
