package at.yawk.profiler.agent;

import at.yawk.profiler.attach.Session;
import java.io.IOException;
import java.io.StringWriter;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Properties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
class AgentConnector {
    private static final String HOST = "127.0.0.1";

    private final Agent provider;

    private final Session session;
    private final Path agentJarPath;

    @Getter private ConnectionManager connectionManager;

    void connect() throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(new InetSocketAddress(HOST, 0));
        connectionManager = new ConnectionManager(socket);

        listen(socket);
        launch(session, socket);

        connectionManager.awaitConnected();
    }

    private void launch(Session session, DatagramSocket socket) throws IOException {
        Properties args = new Properties();
        args.setProperty(Constants.PROPERTY_HOST, HOST);
        args.setProperty(Constants.PROPERTY_PORT, String.valueOf(socket.getLocalPort()));
        StringWriter sw = new StringWriter();
        args.store(sw, null);
        String options = sw.toString();

        Thread loadThread = new Thread(() -> {
            session.loadAgent(agentJarPath, options);
            try {
                provider.close();
            } catch (Exception e) {
                log.error("Failed to close agent provider after agent was complete", e);
            }
        });
        loadThread.setName("Agent loader thread");
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void listen(DatagramSocket socket) {
        //noinspection Convert2MethodRef
        connectionManager.addListener(
                Constants.CHANNEL_BOUND,
                (InetSocketAddress to) -> connectionManager.setRemoteAddress(to)
        );

        Thread listenThread = new Thread(() -> {
            try {
                connectionManager.listen();
            } catch (IOException e) {
                log.error("Error while listening for agent", e);
            }
        });
        listenThread.setName("Agent connection listener");
        listenThread.setDaemon(true);
        listenThread.start();
    }
}
