package app;

import app.application.BaseApplication;
import app.application.Executor;
import app.command.base.Base;
import app.exceptions.CommandException;
import app.terminal.HistoryCompleter;
import fs.io.File;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import utils.InputParser;
import utils.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

public class FsShell implements Command, Runnable {

    public static final String FS_NAME = "mos";

    private InputStream in;
    private OutputStream out;
    private OutputStream err;

    private ExitCallback callback;

    private String username = "root";
    private Thread sshThread;

    private ThreadPoolExecutor threadPoolExecutor;

    private Executor executor = new Executor();

    private File currentPath = new File("/");

    private app.application.Scanner appScanner = new app.application.Scanner();

    {
        threadPoolExecutor = new ThreadPoolExecutor(10, 20, 60, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(100));
    }

    @Override
    public void run() {
        try {
            DefaultHistory history = new DefaultHistory();
            FsShellContext.setCurrentPath(new File("/"));

            appScanner.scanApp();

            if(sshThread == null) {
                Scanner scanner = new Scanner(in);
                StreamUtil.writeOutputStream(out, "\r" + username + "@" + FS_NAME + ":" + FsShellContext.getCurrentPath().getPath() + "$ ");
                while (scanner.hasNextLine()) {
                    String input = scanner.nextLine();
                    execChannelCommand(input);
                    StreamUtil.writeOutputStream(out, "\r" + username + "@" + FS_NAME + ":" + FsShellContext.getCurrentPath().getPath() + "$ ");
                }
            } else {
                Terminal terminal = TerminalBuilder.builder().streams(in, out).encoding("UTF-8").build();
                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .history(history)
                        .completer(new HistoryCompleter(history))
                        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                        .build();
                this.out = terminal.output();

                while (true) {
                    String prefix = "\r" + username + "@" + FS_NAME + ":" + FsShellContext.getCurrentPath().getPath() + "$ ";
                    try {
                        String input = lineReader.readLine(prefix);
                        execChannelCommand(input);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理带管道的命令输入
     */
    private void execChannelCommand(String input) throws IOException {
        List<String> commandInputs = InputParser.parseChannel(input);
        Map<Integer, OutputStream> outputStreamMap = new HashMap<>();
        Map<Integer, InputStream> inputStreamMap = new HashMap<>();
        Map<Integer, Boolean> channelStatusMap = new HashMap<>();

        for (int i = 0; i < commandInputs.size(); i++) {
            PipedOutputStream outputStream = new PipedOutputStream();
            if(i > 0) {
                PipedInputStream inputStream = new PipedInputStream((PipedOutputStream) outputStreamMap.get(i - 1));
                inputStreamMap.put(i, inputStream);
            } else {
                inputStreamMap.put(i, new PipedInputStream());
            }

            if(i == commandInputs.size() - 1) {
                outputStreamMap.put(i, out);
            } else {
                outputStreamMap.put(i, outputStream);
            }

            channelStatusMap.put(i, false);
        }

        CountDownLatch latch = new CountDownLatch(commandInputs.size() - 1);

        try {
            for (int i = 0; i < commandInputs.size(); i++) {
                String commandInput = commandInputs.get(i);

                if(i == commandInputs.size() - 1) {
                    if(i > 0) {
                        while (Boolean.FALSE.equals(channelStatusMap.get(i - 1))) {
                            Thread.sleep(100);
                        }
                    }
                    execCommand(commandInput.trim(), outputStreamMap.get(i), inputStreamMap.get(i));
                } else {
                    int finalI = i;
                    threadPoolExecutor.execute(() -> {
                        FsShellContext.setCurrentPath(currentPath);
                        try {
                            if(finalI > 0) {
                                while (Boolean.FALSE.equals(channelStatusMap.get(finalI - 1))) {
                                    Thread.sleep(100);
                                }
                            }
                            execCommand(commandInput.trim(), outputStreamMap.get(finalI), inputStreamMap.get(finalI));
                            channelStatusMap.put(finalI, true);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            latch.countDown();
                            try {
                                outputStreamMap.get(finalI).close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            // 等待所有线程执行完毕
            latch.await();
        } catch (CommandException | InterruptedException e) {
            StreamUtil.writeOutputStream(err, e.getMessage() + "\n");
        } finally {
            for (InputStream inputStream : inputStreamMap.values()) {
                inputStream.close();
            }
        }
    }

    /**
     * 执行命令
     *
     * @param input 输入的命令
     * @param outputStream 上次命令的输出流
     * @throws IOException
     */
    private void execCommand(String input, OutputStream outputStream, InputStream inputStream) throws IOException {
        String command = InputParser.getCommand(input);
        String[] args = InputParser.getArgs(input);
        try {
            input = handleNoSpaceRedirect(input.trim());
            if(input.isEmpty()) {
                return;
            }

            if("pwd".equalsIgnoreCase(command)) {
                outputStream.write(FsShellContext.getCurrentPath().getPath().getBytes());
                outputStream.write("\n".getBytes());
            } else if("cd".equalsIgnoreCase(command)) {
                executeCd(args);
            } else if("exit".equalsIgnoreCase(command)) {
                StreamUtil.writeOutputStream(out, "Bye~\n");
                callback.onExit(0);
            } else {
                if(appScanner.isApp(command)) {
                    BaseApplication app = appScanner.getApplication(command);
                    executor.execute(app, args, outputStream, inputStream);
                    outputStream.write("\n".getBytes());
                } else {
                    if("ll".equalsIgnoreCase(command)) {
                        command = "ls";
                        args = InputParser.getArgs(input + " -l");
                    }
                    executeExternalCommand(command, args, outputStream, inputStream);
                }
            }
        } catch (ClassNotFoundException e) {
            String errMsg = "command " + command + " not found : "
                    + "you can use 'cd', 'echo', 'ls', 'mkdir', 'pwd', 'touch', 'cat', 'll' app.command to operate the file system or use 'exit' app.command to exit the terminal";
            throw new CommandException(errMsg);
        } catch (Exception e) {
            throw new CommandException(e.getMessage());
        }
    }

    private void executeExternalCommand(String command, String[] args, OutputStream outputStream, InputStream inputStream)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // command首字母大写
        command = command.substring(0, 1).toUpperCase() + command.substring(1);
        Class<?> clazz = Class.forName("app.command." + command);
        if(clazz.isAnnotationPresent(CommandLine.Command.class)) {
            CommandLine commandLine = new CommandLine(
                    clazz.getDeclaredConstructor().newInstance());

            ((Base) commandLine.getCommand()).setIn(inputStream);
            ((Base) commandLine.getCommand()).setOut(outputStream);
            ((Base) commandLine.getCommand()).setErr(outputStream);

            // 命令使用错误提示
            commandLine.setParameterExceptionHandler((ex, parameters) -> {
                StreamUtil.writeOutputStream(err, ex.getMessage() + "\n");
                StreamUtil.writeOutputStream(out, commandLine.getUsageMessage());
                return CommandLine.ExitCode.SOFTWARE;
            });
            commandLine.execute(args);
        }
    }

    private void executeCd(String[] args) {
        if(args.length == 0) {
            FsShellContext.setCurrentPath(new File("/"));
        } else {
            String dirPath = InputParser.getAbsolutePath(args[0]);
            File dir = new File(dirPath, true, false);
            if(dir.exist()) {
                FsShellContext.setCurrentPath(dir);
            } else {
                throw new CommandException("cd: " + dirPath + ": No such directory");
            }
        }
        currentPath = FsShellContext.getCurrentPath();
    }

    /**
     * 处理没有空格的重定向
     */
    private String handleNoSpaceRedirect(String input) {
        StringBuilder modifiedCommand = new StringBuilder();
        StringBuilder currentArg = new StringBuilder();
        boolean insideQuotes = false;
        boolean redirectDetected = false;
        boolean appendRedirect = false;
        boolean lastCharIsRedirect = false;

        // 如果引号是单数，说明引号没有闭合，抛出异常
        if(input.chars().filter(ch -> ch == '"').count() % 2 == 1 || input.chars().filter(ch -> ch == '\'').count() % 2 == 1) {
            throw new IllegalArgumentException("zsh: quotation mark not closed");
        }

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if(currentChar == '"') {
                insideQuotes = !insideQuotes;
            }

            if(!insideQuotes && currentChar == '>') {
                if(lastCharIsRedirect) {
                    if(appendRedirect) {
                        throw new IllegalArgumentException("zsh: parse error near `>`");
                    } else {
                        appendRedirect = true;
                    }
                    continue;
                } else if(!redirectDetected) {
                    redirectDetected = true;
                    lastCharIsRedirect = true;
                    continue;
                }
            } else {
                lastCharIsRedirect = false;
            }

            if(redirectDetected) {
                currentArg.append(currentChar);
            } else {
                modifiedCommand.append(currentChar);
            }
        }

        if(redirectDetected) {
            modifiedCommand.append(appendRedirect ? " >> " : " > ").append(currentArg.toString().trim());
        }

        return modifiedCommand.toString();
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
