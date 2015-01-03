package at.yawk.profiler.web.heapdump;

import at.yawk.hdr.index.Indexer;
import at.yawk.profiler.web.App;
import at.yawk.profiler.web.Aspect;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class IndexerContext extends Aspect {
    private final App app;
    private final Path path;
    private final Indexer indexer;
}
