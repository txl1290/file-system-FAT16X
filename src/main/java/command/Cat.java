package command;

import dirven.DiskDriven;
import picocli.CommandLine;
import protocol.FAT16X;

@CommandLine.Command(name = "cat", mixinStandardHelpOptions = true, description = "show the file's content")
public class Cat implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    @CommandLine.Option(names = ">", description = "The redirect path")
    private String redirectPath;

    @Override
    public void run() {
        try {
            FAT16X.DirectoryEntry fileEntry = DiskDriven.findEntry(DiskDriven.getAbsolutePath(path));
            if(fileEntry == null) {
                throw new Exception("file not found" + path);
            }

            byte[] content = DiskDriven.readFileContent(fileEntry);
            if(redirectPath == null) {
                System.out.println(new String(content));
            } else {
                DiskDriven.writeFileContent(redirectPath, content);
            }
        } catch (Exception e) {
            System.out.println("cat: " + e.getMessage());
        }
    }
}
