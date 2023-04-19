package network;

import java.io.IOException;

public interface IServer {

    void listen(int port) throws IOException;

    void accept();

    void close() throws IOException;
}
