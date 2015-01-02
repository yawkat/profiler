package at.yawk.profiler.web;

import at.yawk.logging.jul.FormatterBuilder;
import at.yawk.logging.jul.Loggers;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.LoggerFactory;

/**
 * @author yawkat
 */
class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        prepareLogging();

        OptionParser parser = new OptionParser();

        OptionSpec<String> host = parser.accepts("host")
                .withRequiredArg()
                .ofType(String.class)
                .defaultsTo("0.0.0.0");

        OptionSpec<Integer> port = parser.acceptsAll(Arrays.asList("p", "port"))
                .withRequiredArg()
                .ofType(int.class)
                .defaultsTo(8080);

        Path dataHome = getDataHome();
        Path defaultAppHome;
        if (dataHome == null) {
            defaultAppHome = Paths.get("data").toAbsolutePath();
        } else {
            defaultAppHome = dataHome.resolve("yprofiler"); // todo: think up a proper name
        }

        OptionSpec<File> appHome = parser.acceptsAll(Arrays.asList("data-directory"))
                .withRequiredArg()
                .ofType(File.class)
                .defaultsTo(defaultAppHome.toFile());

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            parser.printHelpOn(System.err);
            System.exit(-1);
            return;
        }

        Server server = new Server(host.value(options), port.value(options));

        Path dataDir = appHome.value(options).toPath();
        if (!options.has(appHome) && dataHome == null) {
            LoggerFactory.getLogger(Main.class).warn("Failed to resolve user data directory, using CWD instead ({})",
                                                     dataDir);
        } else {
            LoggerFactory.getLogger(Main.class).info("Using data directory {}", dataDir);
        }
        App app = new App(dataDir);
        app.start(server);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            app.close();
        }));

        // wait forever

        Object o = new Object();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (o) {
            o.wait();
        }
    }

    private static void prepareLogging() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        FormatterBuilder.createTimeDateLevel().build(handler);

        Logger rootLogger = Logger.getLogger("");
        Loggers.replaceHandlers(rootLogger, handler);
        rootLogger.setLevel(Level.INFO);

        Logger.getLogger("at.yawk.profiler").setLevel(Level.FINE);
    }

    private static Path getDataHome() {
        if (SystemUtils.IS_OS_UNIX) {
            String dataHomeStr = System.getenv("XDG_DATA_HOME");
            if (dataHomeStr == null) {
                String userHomeStr = System.getenv("XDG_HOME");
                Path userHome;
                if (userHomeStr == null) {
                    userHome = SystemUtils.getUserHome().toPath();
                } else {
                    userHome = Paths.get(userHomeStr);
                }
                return userHome.resolve(".local/share");
            } else {
                return Paths.get(dataHomeStr);
            }
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            return Paths.get(System.getenv("APPDATA"));
        }
        return null;
    }
}
