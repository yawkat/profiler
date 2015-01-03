package at.yawk.profiler.web.heapdump;

import at.yawk.profiler.web.App;
import at.yawk.profiler.web.Aspect;
import at.yawk.profiler.web.ContextHandler;
import at.yawk.profiler.web.Page;
import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class HeapDumpContext extends Aspect {
    private final App app;
    private final Path path;

    private ContextHandler handler;

    @Override
    public synchronized ContextHandler makeContextHandler(String path) {
        if (this.handler != null) { return this.handler; }
        ContextHandler handler = super.makeContextHandler(path);
        this.handler = handler;
        return handler;
    }

    @Page(pattern = "delete")
    public void delete() throws IOException {
        app.getHeapDumpManager().deleteHeapDump(path);
    }
}
