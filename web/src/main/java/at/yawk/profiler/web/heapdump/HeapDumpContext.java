package at.yawk.profiler.web.heapdump;

import at.yawk.hdr.MemoryCachedFilePool;
import at.yawk.hdr.StreamPool;
import at.yawk.hdr.index.Indexer;
import at.yawk.profiler.web.App;
import at.yawk.profiler.web.Aspect;
import at.yawk.profiler.web.ContextHandler;
import at.yawk.profiler.web.Page;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
class HeapDumpContext extends Aspect {
    private final App app;
    private final Path path;
    private final HeapDumpContextManager manager;

    private ContextHandler handler;
    private Indexer indexer;
    private ExecutorService executor;
    private IndexerContext indexerContext;
    private ContextHandler indexerContextHandler;

    @Override
    public synchronized ContextHandler makeContextHandler(String path) {
        if (this.handler != null) { return this.handler; }
        ContextHandler handler = super.makeContextHandler(path);
        this.handler = handler;
        return handler;
    }

    @Page(pattern = "delete")
    public void delete() throws IOException {
        closeIndexer();
        app.getHeapDumpManager().deleteHeapDump(path);
    }

    @Page(pattern = "index", renderedBy = "heapdump/index")
    public Object info() throws IOException {
        return new Info();
    }

    @Page(pattern = "close")
    public synchronized void closeIndexer() {
        log.info("Shutting down indexer on {}", path);
        executor.shutdownNow();
        executor = null;
        indexer = null;
        indexerContext = null;
        handler.removeContext(indexerContextHandler);
        indexerContextHandler = null;
        manager.removeFromStrongCache(this);
    }

    @Page(pattern = "load")
    public synchronized void loadIndexer() throws Exception {
        StreamPool pool = new MemoryCachedFilePool(path.toFile());
        if (executor == null) {
            int parallelism = Runtime.getRuntime().availableProcessors() * 2;
            AtomicInteger threadCounter = new AtomicInteger();
            executor = new ThreadPoolExecutor(
                    parallelism,
                    parallelism,
                    10,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        Thread thread = new Thread(r);
                        thread.setName("Thread pool thread #" + threadCounter.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
            );
        }
        indexer = new Indexer(pool, executor);
        try {
            indexer.scanRootIndex();
        } catch (Exception e) {
            indexer = null;
            throw e;
        }
        indexerContext = new IndexerContext(app, path, indexer);
        indexerContext.scanTags();
        indexerContextHandler = indexerContext.makeContextHandler("");
        handler.addContext(indexerContextHandler);
        manager.addToStrongCache(this);
    }

    private class Info {
        String name = path.getFileName().toString();
        long size;
        String sizeScaled;
        boolean hasRootIndex = indexer != null && indexer.getHeader() != null;

        private Info() throws IOException {
            size = Files.size(path);
            this.sizeScaled = FileUtils.byteCountToDisplaySize(size);
            if (hasRootIndex) {
                frIndexer();
            }
        }

        String version;
        long time;
        String timeFormatted;
        int identifierSize;
        int stringCount;
        int stringBytes;
        String stringBytesScaled;
        boolean hasNoTagIndex = true; // using negative here so it defaults to false on other pages

        void frIndexer() {
            version = indexer.getHeader().version;
            time = indexer.getHeader().time;
            timeFormatted = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_DATE_TIME);
            identifierSize = indexer.getHeader().identifierSize;
            stringCount = indexer.getStringIndex().size();
            stringBytes = indexer.getStringIndex().sizeBytes();
            stringBytesScaled = FileUtils.byteCountToDisplaySize(stringBytes);
            hasNoTagIndex = !indexerContext.isComplete();
        }
    }
}
