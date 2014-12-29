package at.yawk.profiler.web.sampling;

import at.yawk.profiler.sampler.Sampler;
import at.yawk.profiler.sampler.Snapshot;
import at.yawk.profiler.sampler.SnapshotCollector;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * @author yawkat
 */
class ForestHandler implements SnapshotCollector {
    private FNode root = new FNode("root", "");
    @Getter private boolean running = false;

    synchronized void start(Sampler sampler) {
        sampler.start(this);
        running = true;
    }

    synchronized void stop(Sampler sampler) {
        sampler.stop(this);
        running = false;
    }

    @Override
    public synchronized void push(Snapshot snapshot) {
        snapshot.getStackTraces().forEach((thread, trace) -> {
            FNode child = root.getChild(String.valueOf(thread.getId()), thread.getId() + ": " + thread.getName());
            child.index = thread.getId();
            child.push(trace, trace.length - 1);
        });
    }

    synchronized void clear() {
        root.clear();
    }

    synchronized JsonElement dump() {
        JsonTreeWriter writer = new JsonTreeWriter();
        try {
            root.dump(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return writer.get();
    }

    private class FNode {
        final String id;
        final String displayName;
        long index = 0;
        Map<String, FNode> children = new HashMap<>();
        int totalTime;
        int selfTime;

        private FNode(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        private void push(StackTraceElement[] stack, int start) {
            while (start > 0) {
                StackTraceElement here = stack[start];
                StackTraceElement next = stack[start - 1];
                if (here.getClassName().equals(next.getClassName()) &&
                    here.getMethodName().equals(next.getMethodName())) {
                    start--;
                } else {
                    break;
                }
            }

            totalTime++;
            if (start < 0) {
                selfTime++;
            } else {
                String className = stack[start].getClassName();
                String methodName = stack[start].getMethodName();
                String desc = id + ">" + className + "~" + methodName;
                String dname = className + "." + methodName;
                FNode child = getChild(desc, dname);
                child.push(stack, start - 1);
            }
        }

        private FNode getChild(String desc, String displayName) {
            String id = hash(desc);
            return children.computeIfAbsent(id, c -> new FNode(id, displayName));
        }

        private void clear() {
            children.clear();
        }

        private void dump(JsonWriter into) throws IOException {
            into.beginObject();
            into.name("id");
            into.value(id);
            into.name("name");
            into.value(displayName);
            into.name("self_time");
            into.value(selfTime);
            into.name("total_time");
            into.value(totalTime);

            into.name("children");
            into.beginArray();
            children.entrySet()
                    .stream()
                    .sorted(Comparator.<Map.Entry<String, FNode>>comparingLong(e -> e.getValue().index)
                                    .thenComparingInt(e -> -e.getValue().totalTime)
                    )
                    .forEach(child -> {
                        try {
                            child.getValue().dump(into);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            into.endArray();
            into.endObject();
        }
    }

    private String hash(String id) {
        byte[] md5Bytes = Hashing.md5().hashString(id, StandardCharsets.UTF_8).asBytes();
        return Base64.getEncoder().encodeToString(md5Bytes);
    }
}
