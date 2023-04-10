package app;

import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;
import utils.InputParser;

import java.util.Scanner;

public class FsApp {

    private final static String USERNAME = "mos";

    File currentDir = new File("/");

    private String in;
    private String out;
    private String err;

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(USERNAME + "：");
            in = scanner.nextLine().trim();
            if(in.isEmpty()) {
                continue;
            }

            out = "";
            err = "";

            String command = InputParser.getCommand(in);
            String[] args = InputParser.getArgs(in);
            try {
                if("pwd".equalsIgnoreCase(command)) {
                    out = currentDir.getPath();
                } else if("cd".equalsIgnoreCase(command)) {
                    executeCd(args);
                } else if("exit".equalsIgnoreCase(command)) {
                    out = "Bye!";
                    printResult();
                    break;
                } else {
                    if("ll".equalsIgnoreCase(command)) {
                        command = "Ls";
                        args = InputParser.getArgs(in + " -l");
                    }
                    Class<?> clazz = Class.forName("app.command." + command);
                    if(clazz.isAnnotationPresent(CommandLine.Command.class)) {
                        CommandLine commandLine = new CommandLine(clazz.getDeclaredConstructor(String.class).newInstance(currentDir.getPath()));
                        commandLine.execute(args);

                        // 获取命令执行结果
                        out = ((Base) commandLine.getCommand()).getOut();
                        err = ((Base) commandLine.getCommand()).getErr();
                    }
                }
            } catch (ClassNotFoundException e) {
                err = "command " + command + " not found : "
                        + "you can use 'cd', 'echo', 'ls', 'mkdir', 'pwd', 'touch', 'cat', 'll' app.command to operate the file system or use 'exit' app.command to exit the terminal";
            } catch (Exception e) {
                err = e.getMessage();
            }
            printResult();
        }
    }

    private void executeCd(String[] args) {
        if(args.length == 0) {
            currentDir = new File("/");
        } else {
            String dirPath = getAbsolutePath(args[0]);
            File dir = new File(dirPath, true, false);
            if(dir.exist()) {
                currentDir = dir;
            } else {
                err = "cd: " + dirPath + ": No such directory";
            }
        }
    }

    private void printResult() {
        if(!err.isEmpty()) {
            System.out.println(err);
        } else if(!out.isEmpty()) {
            System.out.println(out);
        }
    }

    private String getAbsolutePath(String path) {
        if(InputParser.isAbsolutePath(path)) {
            return InputParser.trimPath(path);
        } else {
            return InputParser.trimPath(currentDir.getPath() + "/" + path);
        }
    }
}
