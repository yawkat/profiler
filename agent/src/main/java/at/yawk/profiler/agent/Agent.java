package at.yawk.profiler.agent;

import at.yawk.profiler.attach.AttachmentException;
import at.yawk.profiler.attach.Session;
import at.yawk.profiler.attach.VmDescriptor;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Agent implements AutoCloseable {
    private final Session session;
    private final boolean closeSessionWhenDone;

    private Set<String> loadedClasses = Collections.synchronizedSet(new HashSet<>());
    private Path agentJarPath;
    private AgentConnector connector;

    public static Agent attach(Session session) throws IOException, InterruptedException {
        return attach(session, false);
    }

    public static Agent attach(VmDescriptor descriptor)
            throws IOException, InterruptedException, AttachmentException {
        return attach(descriptor.attach(), true);
    }

    private static Agent attach(Session session, boolean closeSessionWhenDone)
            throws IOException, InterruptedException {
        Agent agent = new Agent(session, closeSessionWhenDone);
        agent.doAttach();
        return agent;
    }

    private void doAttach() throws IOException, InterruptedException {
        agentJarPath = Files.createTempFile("agent", ".jar");
        Files.delete(agentJarPath);

        Reflections reflections = new Reflections("at.yawk.profiler.agent");
        try (JarBuilder builder = JarBuilder.create(agentJarPath)) {
            for (Class<?> c : reflections.getTypesAnnotatedWith(AgentClass.class)) {
                builder.addClass(c);
            }
            builder.manifest(AgentClient.class.getResource("MANIFEST.MF"));
        }

        connector = new AgentConnector(this, session, agentJarPath);
        connector.connect();
    }

    @Override
    public void close() throws Exception {
        try {
            connector.getConnectionManager().send("", null);
        } catch (IOException ignored) {}
        Files.deleteIfExists(agentJarPath);
        if (closeSessionWhenDone) {
            session.close();
        }
    }

    public <S> void send(Class<? extends Module<S, ?>> module, S packet) {
        try {
            connector.getConnectionManager().send(module.getName(), packet);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public <R> void addListener(Class<? extends Module<?, R>> module, Consumer<R> listener) {
        connector.getConnectionManager().addListener(module.getName(), listener);
    }

    public void loadClass(Class<?> clazz) {
        String name = clazz.getName();

        if (!loadedClasses.add(name)) { return; }

        for (Class<?> declared : clazz.getDeclaredClasses()) {
            loadClass(declared);
        }

        log.debug("Loading class {}", name);

        String fileName = name.replace('.', '/') + ".class";
        byte[] bytes;
        try {
            bytes = ByteStreams.toByteArray(clazz.getClassLoader().getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        send(ClassBootstrap.class, new ClassBootstrap.ClassDataWrapper(name, bytes));
    }

    public void loadModule(Class<? extends Module<?, ?>> moduleClass) {
        loadClass(moduleClass);
        log.debug("Loading module {}", moduleClass);
        send(ModuleBootstrap.class, moduleClass.getName());
    }
}
