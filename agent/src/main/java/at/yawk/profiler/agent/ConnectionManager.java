package at.yawk.profiler.agent;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
class ConnectionManager {
    private final DatagramSocket socket;

    private final Object remoteAddressMutex = new Object();
    private InetSocketAddress remoteAddress;

    private final Map<String, Consumer<?>> listeners = new ConcurrentHashMap<>();

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        synchronized (remoteAddressMutex) {
            remoteAddressMutex.notifyAll();
        }
    }

    public void awaitConnected() throws InterruptedException {
        synchronized (remoteAddressMutex) {
            remoteAddressMutex.wait();
        }
    }

    @SuppressWarnings("unchecked")
    void listen() throws IOException {
        int receiveBufferSize = socket.getReceiveBufferSize();
        DatagramPacket dp = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
        while (!Thread.interrupted()) {
            socket.receive(dp);

            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dp.getData()));
            String name = ois.readUTF();
            try {
                Object o = ois.readObject();

                Consumer listener = listeners.get(name);
                if (listener != null) {
                    listener.accept(o);
                } else {
                    log.warn("No handler registered for packet name {}", name);
                }
            } catch (ClassNotFoundException e) {
                log.error("Failed to read object", e);
            }
        }
        socket.close();
    }

    void send(String channel, Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeUTF(channel);
        oos.writeObject(object);

        byte[] serialized = bos.toByteArray();
        DatagramPacket packet = new DatagramPacket(serialized, serialized.length);
        if (remoteAddress == null) {
            log.warn("Not sending packet in channel {}, no remote address set!", channel);
            return;
        }
        packet.setSocketAddress(remoteAddress);
        socket.send(packet);
    }

    void addListener(String channel, Consumer<?> consumer) {
        listeners.put(channel, consumer);
    }
}
