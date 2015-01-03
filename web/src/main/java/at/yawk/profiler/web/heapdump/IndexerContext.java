package at.yawk.profiler.web.heapdump;

import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.TypeData;
import at.yawk.profiler.web.App;
import at.yawk.profiler.web.Aspect;
import at.yawk.profiler.web.Page;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
class IndexerContext extends Aspect {
    private final App app;
    private final Path path;
    private final Indexer indexer;

    private final JsonProgressCounter tagScanProgress = new JsonProgressCounter(1 << 10);
    @Getter private volatile boolean complete = false;

    void scanTags() {
        indexer.execute(() -> indexer.scanTypeIndex(tagScanProgress))
                .addListener(() -> {
                    tagScanProgress.end();
                    complete = true;
                });
    }

    @Page(pattern = "tag_progress")
    public JsonObject getProgress() {
        return tagScanProgress.toJson();
    }

    @Page(pattern = "top_classes", renderedBy = "heapdump/top_classes")
    public TypeList getTopClasses() {
        return new TypeList(0, 50);
    }

    @Page(pattern = "top_classes-(\\d+)-(\\d+)", renderedBy = "heapdump/top_classes")
    public TypeList getTopClasses(String startStr, String countStr) {
        return new TypeList(Integer.parseInt(startStr), Integer.parseInt(countStr));
    }

    private class TypeList {
        final int start;
        final int prevPageStart;
        final int nextPageStart;
        final int pageSize;
        final List<?> types;

        private TypeList(int start, int pageSize) {
            this.start = start;
            this.prevPageStart = start - pageSize;
            this.nextPageStart = start + pageSize;
            this.pageSize = pageSize;
            //noinspection Convert2MethodRef
            this.types = indexer.getTypeIndex().stream()
                    .sorted(Comparator.comparingInt(td -> -td.getInstanceCount()))
                    .skip(start)
                    .limit(pageSize)
                    .map(data -> new Type(data))
                    .collect(Collectors.toList());
        }
    }

    private class Type {
        String name;
        String instanceCount;
        String memoryUsage;
        String memoryUsageScaled;

        public Type(TypeData data) {
            name = data.getName();
            instanceCount = breakUpToString(data.getInstanceCount());
            memoryUsage = breakUpToString(data.getMemoryUsage());
            memoryUsageScaled = bytesToString(data.getMemoryUsage());
        }
    }

    private static String breakUpToString(long uint) {
        if (uint <= 0) {
            return String.valueOf(0);
        }
        StringBuilder result = new StringBuilder();
        int charIndex = 0;
        while (uint > 0) {
            result.insert(0, uint % 10);
            charIndex++;
            if ((charIndex % 3) == 0) {
                result.insert(0, ' ');
            }
            uint /= 10;
        }
        return result.toString();
    }

    static String bytesToString(long bytes) {
        String[] suffixes = { "B", "KB", "MB", "GB", "TB", "PB", "EB" };
        int suffixIndex = 0;
        double bytesD = bytes;
        while (bytesD > 1024 && suffixIndex < suffixes.length) {
            bytesD /= 1024;
            suffixIndex++;
        }
        StringBuilder res = new StringBuilder();
        if (suffixIndex == 0) {
            res.append(bytes);
        } else if (bytesD < 10) {
            res.append(Math.round(bytesD * 100) / 100F);
        } else if (bytesD < 100) {
            res.append(Math.round(bytesD * 10) / 10F);
        } else {
            res.append(Math.round(bytesD));
        }
        return res.append(suffixes[suffixIndex]).toString();
    }
}
