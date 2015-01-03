package at.yawk.profiler.web.heapdump;

import at.yawk.profiler.web.*;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * @author yawkat
 */
@Component
@ContextPath(value = "heapdump/")
public class HeapDumpContextManager extends AppAspect {
    private final Map<Path, SoftReference<Handler>> contextCache = new HashMap<>();
    private final Map<Path, HeapDumpContext> strongContextCache = new HashMap<>();

    private synchronized Handler getDumpHandler(Path dumpPath) {
        SoftReference<Handler> context = contextCache.get(dumpPath);
        if (context != null) {
            Handler handler = context.get();
            if (handler != null) { return handler; }
        }

        HeapDumpContext ctx = new HeapDumpContext(getApp(), dumpPath);
        ContextHandler handler = ctx.makeContextHandler(dumpPath.getFileName().toString() + "/");
        SoftReference<Handler> reference = new SoftReference<>(handler);
        contextCache.put(dumpPath, reference);
        return handler;
    }

    @Override
    public ContextHandler makeContextHandler(String path) {
        ContextHandler contextHandler = super.makeContextHandler(path);
        contextHandler.addContext((session, p) -> {
            Matcher matcher = HeapDumpManager.DUMP_PATTERN.matcher(p);
            if (matcher.find() && matcher.start() == 0) {
                String group = matcher.group();
                Path dumpPath = getApp().getHeapDumpManager().resolveDump(group);
                if (Files.exists(dumpPath)) {
                    Handler handler = getDumpHandler(dumpPath);
                    return handler.serve(session, p);
                }
            }
            return false;
        });
        return contextHandler;
    }

    @Page(pattern = "list", renderedBy = "heapdump/list")
    public Object listHeapdumps() throws IOException {
        return getApp()
                .getHeapDumpManager()
                .listHeapDumps()
                .stream()
                .map(p -> p.getFileName().toString())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
