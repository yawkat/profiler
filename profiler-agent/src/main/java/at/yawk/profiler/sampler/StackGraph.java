package at.yawk.profiler.sampler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
public class StackGraph {
    private final boolean oneNodePerMethod;

    private Map<String, Node> nodes;
    @Getter private Node root;

    public StackGraph(boolean oneNodePerMethod) {
        this.oneNodePerMethod = oneNodePerMethod;
        clear();
    }

    synchronized void push(StackTraceElement[] stack) {
        root.push(stack, stack.length - 1);
    }

    public synchronized void clear() {
        nodes = oneNodePerMethod ? new HashMap<>() : null;
        root = new Node("", null, null);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public class Node {
        @Getter private final String id;
        @Getter private final String className;
        @Getter private final String methodName;
        private final Map<String, Child> children = new HashMap<>();
        @Getter private int selfTime;
        @Getter private int totalTime;

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
                String desc = className + "~" + methodName;
                Child child = children.get(desc);
                if (child == null) {
                    child = new Child();
                    if (oneNodePerMethod) {
                        //noinspection Convert2MethodRef
                        child.target = nodes.computeIfAbsent(desc, d -> new Node(desc, className, methodName));
                    } else {
                        child.target = new Node(desc, className, methodName);
                    }
                    children.put(desc, child);
                }
                child.enterCount++;
                child.target.push(stack, start - 1);
            }
        }

        public Collection<Child> children() {
            return Collections.unmodifiableCollection(children.values());
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Child {
        @Getter Node target;
        @Getter int enterCount;
    }
}
