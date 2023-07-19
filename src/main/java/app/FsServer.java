package app;

import app.sshd.FsScpCommandFactory;
import network.IServer;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.io.BuiltinIoServiceFactoryFactories;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;

import java.io.IOException;
import java.nio.file.Paths;

public class FsServer implements IServer {

    SshServer sshServer;

    @Override
    public void listen(int port) throws IOException {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        sshServer.setKeyPairProvider(myHostKeyProvider());
        sshServer.setPasswordAuthenticator((username, password, session) -> true);
        sshServer.setIoServiceFactoryFactory(BuiltinIoServiceFactoryFactories.NIO2.create());
        sshServer.getProperties().put(SshServer.IDLE_TIMEOUT, 1000 * 60 * 60 * 24);
        sshServer.setCommandFactory(new FsScpCommandFactory());
        sshServer.start();
        sshServer.setShellFactory(new MyShellFactory());
    }

    @Override
    public void accept() {
        // 使用sshd默认的acceptor，不需要实现
    }

    @Override
    public void close() throws IOException {
        sshServer.close();
    }

    private SimpleGeneratorHostKeyProvider myHostKeyProvider() {
        SimpleGeneratorHostKeyProvider provider = new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser"));
        provider.setAlgorithm(KeyUtils.RSA_ALGORITHM);
        provider.setKeySize(2048);
        sshServer.setKeyPairProvider(provider);
        return provider;
    }

    public static class MyShellFactory implements ShellFactory {
        @Override
        public Command createShell(ChannelSession channel) {
            return new FsShell();
        }
    }
}
