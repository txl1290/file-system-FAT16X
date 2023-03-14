package command;

import dirven.DiskDriven;
import picocli.CommandLine;
import protocol.FAT16X;
import utils.InputParser;

import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "ls", mixinStandardHelpOptions = true, description = "show the files and dirs in a designated path")
public class Ls implements Runnable {

    @CommandLine.Option(names = "-l", description = "Show the details of the files and dirs")
    private boolean showDetails;

    @CommandLine.Option(names = ">", description = "The redirect path")
    private String redirectPath;

    /**
     * The content to be redirected
     */
    private StringBuilder content = new StringBuilder();

    @Override
    public void run() {
        try {
            String absolutePath = DiskDriven.getAbsolutePath(DiskDriven.getCurrentPath());
            if(InputParser.isRoot(absolutePath)) {
                listRootDir();
            } else {
                FAT16X.DirectoryEntry entry = DiskDriven.findEntry(absolutePath);
                if(entry == null) {
                    throw new IllegalArgumentException("No such file or directory: " + DiskDriven.getCurrentPath());
                }

                if(entry.isDir()) {
                    listDir(entry);
                } else {
                    listFile(entry);
                }
            }

            if(redirectPath == null) {
                System.out.println(content.toString());
            } else {
                DiskDriven.outputRedirect(content.toString().getBytes(StandardCharsets.UTF_8), redirectPath);
            }
        } catch (Exception e) {
            System.out.println("ls: " + e.getMessage());
        }
    }

    private void listDir(FAT16X.DirectoryEntry dir) {
        FAT16X.DirectoryEntry[] entries = DiskDriven.readDirEntries(dir);
        listDir(entries);
    }

    private void listDir(FAT16X.DirectoryEntry[] entries) {
        for (int i = 0; i < entries.length; i++) {
            FAT16X.DirectoryEntry entry = entries[i];
            String colorName = entry.getFullName();

            // 文件夹终端颜色
            if(redirectPath == null && entry.isDir()) {
                colorName = "\033[34m" + entry.getFullName() + "\033[0m";
            }
            String fileDetail = colorName;

            if(showDetails) {
                StringBuilder builder = new StringBuilder();
                if(entry.isDir()) {
                    builder.append("d");
                } else {
                    builder.append("-");
                }
                if(entry.isReadOnly()) {
                    builder.append("r-x");
                } else {
                    builder.append("rwx");
                }
                builder.append("\t").append(entry.getHumanReadableFileSize()).append("\t").append(entry.getLaseWriteTime()).append("\t")
                        .append(colorName);
                fileDetail = builder.toString();
            }
            
            if(i == entries.length - 1) {
                content.append(fileDetail);
            } else {
                content.append(fileDetail + "\n");
            }
        }
    }

    private void listFile(FAT16X.DirectoryEntry file) {
        content.append("\033[0m" + file.getFullName());
    }

    private void listRootDir() {
        FAT16X.DirectoryEntry[] entries = DiskDriven.getDisk().getFs().getRootDirectory();
        listDir(entries);
    }
}
