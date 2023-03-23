package command;

import command.base.Base;
import dirven.DiskDriven;
import picocli.CommandLine;
import protocol.FAT16X;

@CommandLine.Command(name = "cat", mixinStandardHelpOptions = true, description = "show the file's content")
public class Cat extends Base {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    @Override
    protected String executeCommand() {
        FAT16X.DirectoryEntry fileEntry = DiskDriven.findEntry(DiskDriven.getAbsolutePath(path));
        if(fileEntry == null) {
            throw new IllegalArgumentException("file not found " + path);
        }

        byte[] content = DiskDriven.readFileContent(fileEntry);
        return new String(content);
    }
}
