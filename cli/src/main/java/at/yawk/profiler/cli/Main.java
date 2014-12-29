package at.yawk.profiler.cli;

import at.yawk.logging.jul.FormatterBuilder;
import at.yawk.logging.jul.Loggers;
import at.yawk.profiler.agent.Agent;
import at.yawk.profiler.attach.AttachmentProvider;
import at.yawk.profiler.attach.Session;
import at.yawk.profiler.attach.VmDescriptor;
import at.yawk.profiler.attach.sun.SunAttachmentProvider;
import at.yawk.profiler.graph.GraphRenderer;
import at.yawk.profiler.graph.GraphvizRenderer;
import at.yawk.profiler.graph.InteractiveSvgRenderer;
import at.yawk.profiler.sampler.Sampler;
import at.yawk.profiler.sampler.StackGraph;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class Main {
    private final OptionParser parser = new OptionParser();

    private ForkJoinPool executor;
    private OptionSet options;
    private Agent agent;

    public static void main(String[] args) throws IOException, InterruptedException {
        new Main().run(args);
    }

    private void run(String[] args) throws IOException, InterruptedException {
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            System.err.println("Failed to parse arguments");
            throw die(true);
        }

        prepareLogging();

        Session vm = selectVM();

        log.info("Attaching agent...");
        agent = Agent.attach(vm);

        boolean doneAnything = false;

        try {
            executor = new ForkJoinPool(
                    4,
                    pool -> {
                        ForkJoinWorkerThread thread = new ForkJoinWorkerThread(pool) {};
                        thread.setDaemon(true);
                        return thread;
                    },
                    (t, e) -> log.error("Exception in thread " + t, e),
                    false
            );

            doneAnything = sample();

            executor.awaitQuiescence(1, TimeUnit.DAYS);
        } finally {
            try {
                agent.close();
            } catch (Throwable e) {
                log.error("Failed to close agent", e);
            }
        }

        if (!doneAnything) {
            System.err.println("Nothing to do!");
            throw die(true);
        }
    }

    /**
     * @return never returns, but can be used to end method execution
     */
    private Error die(boolean withHelp) throws IOException {
        if (withHelp) { parser.printHelpOn(System.err); }
        System.exit(-1);
        return null;
    }

    // logging

    private void prepareLogging() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        FormatterBuilder.create().build(handler);

        Logger rootLogger = Logger.getLogger("");
        Loggers.replaceHandlers(rootLogger, handler);
        rootLogger.setLevel(Level.INFO);

        Logger.getLogger("at.yawk.profiler").setLevel(Level.FINE);
    }

    //// VM selection

    OptionSpec<Integer> pid =
            parser.accepts("pid", "PID of the monitored VM")
                    .withRequiredArg()
                    .ofType(int.class);

    OptionSpec<String> name =
            parser.accepts("name", "Name of the monitored VM")
                    .withRequiredArg()
                    .ofType(String.class)
                    .defaultsTo("");

    private Session selectVM() throws IOException {
        AttachmentProvider provider = SunAttachmentProvider.getInstance();
        VmDescriptor desc;
        if (options.has(pid)) {
            desc = provider.resolveProcess(pid.value(options));
        } else {
            String name = this.name.value(options);
            List<VmDescriptor> vms = provider.getRunningDescriptors()
                    .stream()
                    .filter(e -> {
                        String vmName = ManagementFactory.getRuntimeMXBean().getName();
                        // filter out our VM
                        return !vmName.contains(String.valueOf(e.getPid()));
                    })
                    .filter(e -> e.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
            if (vms.size() == 1) {
                desc = vms.get(0);
            } else if (vms.isEmpty()) {
                System.err.println("No VM found.");
                throw die(false);
            } else {
                System.err.printf("%s candidates:%n", vms.size());
                vms.stream()
                        .sorted(Comparator.comparingInt(VmDescriptor::getPid))
                        .forEach(vm -> System.err.printf("%6d %s%n", vm.getPid(), vm.getName()));
                if (options.has(name)) {
                    throw die(false);
                } else {
                    throw die(true);
                }
            }
        }

        log.info("Connecting to {}...", desc.getPid());

        return desc.attach();
    }

    //// sampling

    OptionSpec<Void> sample =
            parser.accepts("sample", "Run the sampler");
    OptionSpec<Double> sampleDuration =
            parser.accepts("sample-duration", "Sampling duration in seconds")
                    .requiredIf(sample)
                    .withRequiredArg()
                    .ofType(double.class);
    OptionSpec<File> sampleOutput =
            parser.accepts("sample-output", "Output SVG file for the sampler graph")
                    .requiredIf(sample)
                    .withRequiredArg()
                    .ofType(File.class);

    OptionSpec<Void> sampleDisableCross =
            parser.accepts("sample-disable-cross",
                           "Disable stepping back in the graph when hitting the same method (make graph non-cyclic)");

    OptionSpec<Void> sampleGraphNoInteractive =
            parser.accepts("sample-svg-no-interactive", "Disable sampling SVG interactivity");
    OptionSpec<String> sampleGraphInteractiveSvgPan =
            parser.accepts("sample-svg-interactive-svgpan-url", "URL for the SVGPan script used for SVG interactivity")
                    .withRequiredArg()
                    .ofType(String.class);

    private boolean sample() {
        if (!options.has(sample)) { return false; }

        executor.execute(() -> {
            Sampler sampler = new Sampler(agent);
            log.info("Sampler starting");
            sampler.start();
            try {
                Thread.sleep((long) (sampleDuration.value(options) * 1000));
            } catch (InterruptedException e) {
                return;
            }
            sampler.stop();
            log.info("Sampler stopped, computing stack graph");

            boolean oneNodePerMethod = !options.has(sampleDisableCross);
            StackGraph stackGraph = sampler.getSnapshots().computeStackGraph(oneNodePerMethod);

            log.info("Writing SVG");

            GraphRenderer renderer = new GraphvizRenderer();
            if (!options.has(sampleGraphNoInteractive)) {
                if (options.has(sampleGraphInteractiveSvgPan)) {
                    renderer = new InteractiveSvgRenderer(renderer, sampleGraphInteractiveSvgPan.value(options));
                } else {
                    renderer = new InteractiveSvgRenderer(renderer);
                }
            }
            String svg = renderer.renderSvg(stackGraph);

            try {
                Files.write(sampleOutput.value(options).toPath(), svg.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Error while writing SVG", e);
            }
        });
        return true;
    }
}
