import app.FsServer;
import app.FsShell;

import java.io.IOException;

public class FsTerminal {

    private static final int PORT = 2022;

    public static void main(String[] args) throws IOException {
        FsServer server = new FsServer();
        server.listen(PORT);

        FsShell shell = new FsShell();

        shell.setInputStream(System.in);
        shell.setOutputStream(System.out);
        shell.setErrorStream(System.out);
        shell.setExitCallback((status, msg) -> System.exit(status));

        shell.run();

        server.close();
    }
}
