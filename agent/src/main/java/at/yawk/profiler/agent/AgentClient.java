package at.yawk.profiler.agent;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@AgentClass
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AgentClient {
    @Getter private final String options;
    @Getter private final Instrumentation instrumentation;

    private DatagramSocket socket;
    private InetSocketAddress remoteAddress;

    private final Map<Class<? extends Module>, Module> modulesByType = new ConcurrentHashMap<>();
    private final Map<String, Module> modulesByName = new ConcurrentHashMap<>();

    void init() {
        try {
            init0();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    private void init0() throws IOException, ReflectiveOperationException {
        loadModule(ClassBootstrap.class);
        loadModule(ModuleBootstrap.class);

        Properties properties = new Properties();
        properties.load(new StringReader(options));

        String address = properties.getProperty(Constants.PROPERTY_HOST);
        int port = Integer.parseInt(properties.getProperty(Constants.PROPERTY_PORT));

        remoteAddress = new InetSocketAddress(address, port);
        InetSocketAddress ourAddress = new InetSocketAddress("127.0.0.1", 0);

        socket = new DatagramSocket(ourAddress);

        send(Constants.CHANNEL_BOUND, socket.getLocalSocketAddress());

        int receiveBufSize = socket.getReceiveBufferSize();
        DatagramPacket dp = new DatagramPacket(new byte[receiveBufSize], receiveBufSize);
        while (true) {
            socket.receive(dp);

            ObjectInputStream ois = new AgentObjectInputStream(new ByteArrayInputStream(dp.getData()), this);
            String name = ois.readUTF();
            if (name.equals(Constants.CHANNEL_EXIT)) {
                break;
            }

            try {
                Object o = ois.readObject();
                modulesByName.get(name).receive(o);
            } catch (Throwable e) {
                log(AgentClient.class, e);
            }
        }
        socket.close();
        modulesByName.values().forEach(Module::shutdown);
    }

    Module loadModule(Class<? extends Module> moduleClass)
            throws ReflectiveOperationException {
        Constructor<? extends Module> constructor = moduleClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Module module = constructor.newInstance();
        module.agent = this;
        modulesByType.put(moduleClass, module);
        modulesByName.put(module.name, module);

        module.init();

        return module;
    }

    final void send(String channel, Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeUTF(channel);
        oos.writeObject(object);
        byte[] serialized = bos.toByteArray();
        DatagramPacket dp = new DatagramPacket(serialized, serialized.length);
        dp.setSocketAddress(remoteAddress);
        socket.send(dp);
    }

    public void log(Class<?> on, Throwable throwable) {
        // todo: forward to agent owner
        throwable.printStackTrace();
    }

    @SuppressWarnings("unchecked")
    public <M extends Module> M getModule(Class<M> type) {
        return (M) modulesByType.get(type);
    }
}
