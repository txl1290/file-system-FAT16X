package app;

import app.command.base.Base;
import app.terminal.HistoryCompleter;
import fs.io.File;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import utils.InputParser;
import utils.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

public class FsShell implements Command, Runnable {

    public static final String FS_NAME = "mos";

    File currentDir = new File("/");

    private InputStream in;
    private OutputStream out;
    private OutputStream err;

    private ExitCallback callback;

    private String username = "root";

    Thread sshThread;

    @Override
    public void run() {
        try {
            DefaultHistory history = new DefaultHistory();

            if(sshThread == null) {
                Scanner scanner = new Scanner(in);
                StreamUtil.writeOutputStream(out, "\r" + username + "@" + FS_NAME + ":" + currentDir.getPath() + "$ ");
                while (scanner.hasNextLine()) {
                    String input = scanner.nextLine();
                    execCommand(input);
                    StreamUtil.writeOutputStream(out, "\r" + username + "@" + FS_NAME + ":" + currentDir.getPath() + "$ ");
                }
            } else {
                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(TerminalBuilder.builder().streams(in, out).encoding("UTF-8").build())
                        .history(history)
                        .completer(new HistoryCompleter(history))
                        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                        .build();

                while (true) {
                    String prefix = "\r" + username + "@" + FS_NAME + ":" + currentDir.getPath() + "$ ";
                    try {
                        String input = lineReader.readLine(prefix);
                        execCommand(input);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void execCommand(String input) throws IOException {
        input = input.trim();
        if(input.isEmpty()) {
            return;
        }

        String command = InputParser.getCommand(input);
        String[] args = InputParser.getArgs(input);
        try {
            if("pwd".equalsIgnoreCase(command)) {
                StreamUtil.writeOutputStream(out, currentDir.getPath() + "\n");
            } else if("cd".equalsIgnoreCase(command)) {
                executeCd(args);
            } else if("exit".equalsIgnoreCase(command)) {
                StreamUtil.writeOutputStream(out, "Bye~\n");
                callback.onExit(0);
            } else {
                if("ll".equalsIgnoreCase(command)) {
                    command = "Ls";
                    args = InputParser.getArgs(input + " -l");
                }
                Class<?> clazz = Class.forName("app.command." + command);
                if(clazz.isAnnotationPresent(CommandLine.Command.class)) {
                    CommandLine commandLine = new CommandLine(clazz.getDeclaredConstructor(String.class).newInstance(currentDir.getPath()));

                    // 命令使用错误提示
                    commandLine.setParameterExceptionHandler((ex, parameters) -> {
                        StreamUtil.writeOutputStream(err, ex.getMessage() + "\n");
                        StreamUtil.writeOutputStream(out, commandLine.getUsageMessage());
                        return CommandLine.ExitCode.SOFTWARE;
                    });
                    commandLine.execute(args);

                    // 获取命令执行结果
                    String commandOutput = ((Base) commandLine.getCommand()).getOut();
                    if(!"clear".equalsIgnoreCase(command)) {
                        commandOutput = commandOutput.isEmpty() ? "" : commandOutput + "\n";
                    }
                    StreamUtil.writeOutputStream(out, commandOutput);
                    StreamUtil.writeOutputStream(err, ((Base) commandLine.getCommand()).getErr());
                }
            }
        } catch (ClassNotFoundException e) {
            String errMsg = "command " + command + " not found : "
                    + "you can use 'cd', 'echo', 'ls', 'mkdir', 'pwd', 'touch', 'cat', 'll' app.command to operate the file system or use 'exit' app.command to exit the terminal\n";
            StreamUtil.writeOutputStream(err, errMsg);
        } catch (Exception e) {
            StreamUtil.writeOutputStream(err, e.getMessage() + "\n");
        }
    }

    private void executeCd(String[] args) throws IOException {
        if(args.length == 0) {
            currentDir = new File("/");
        } else {
            String dirPath = getAbsolutePath(args[0]);
            File dir = new File(dirPath, true, false);
            if(dir.exist()) {
                currentDir = dir;
            } else {
                StreamUtil.writeOutputStream(err, "cd: " + dirPath + ": No such directory\n");
            }
        }
    }

    private String getAbsolutePath(String path) {
        if(InputParser.isAbsolutePath(path)) {
            return InputParser.trimPath(path);
        } else {
            return InputParser.trimPath(currentDir.getPath() + "/" + path);
        }
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        this.in = inputStream;
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.out = outputStream;
    }

    @Override
    public void setErrorStream(OutputStream outputStream) {
        this.err = outputStream;
    }

    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.callback = exitCallback;
    }

    @Override
    public void start(ChannelSession channelSession, Environment environment) {
        username = channelSession.getSession().getUsername();
        sshThread = new Thread(this);
        sshThread.start();
    }

    @Override
    public void destroy(ChannelSession channelSession) {
        sshThread.interrupt();
        channelSession.close(false);
    }
}
