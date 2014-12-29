package at.yawk.profiler.web;

import at.yawk.logging.jul.FormatterBuilder;
import at.yawk.logging.jul.Loggers;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

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

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            parser.printHelpOn(System.err);
            System.exit(-1);
            return;
        }

        Server server = new Server(host.value(options), port.value(options));

        App app = new App();
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
}
