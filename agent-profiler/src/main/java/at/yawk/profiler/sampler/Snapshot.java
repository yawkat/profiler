package at.yawk.profiler.sampler;

import java.io.Serializable;
import java.util.Map;
import lombok.Getter;

/**
 * @author yawkat
 */
public class Snapshot implements Serializable {
    @Getter Map<ThreadIdentity, StackTraceElement[]> stackTraces;

    @Getter
    public static class ThreadIdentity implements Serializable {
        long id;
        String name;
        String group;

        private void internStrings() {
            name = name.intern();
            group = group.intern();
        }
    }

    void internStrings() {
        for (Map.Entry<ThreadIdentity, StackTraceElement[]> entry : stackTraces.entrySet()) {
            entry.getKey().internStrings();
            StackTraceElement[] elements = new StackTraceElement[entry.getValue().length];
            for (int i = 0; i < entry.getValue().length; i++) {
                StackTraceElement old = entry.getValue()[i];
                elements[i] = new StackTraceElement(
                        old.getClassName().intern(),
                        old.getMethodName().intern(),
                        old.getFileName() == null ? null : old.getFileName().intern(),
                        old.getLineNumber()
                );
            }
            entry.setValue(elements);
        }
    }
}
