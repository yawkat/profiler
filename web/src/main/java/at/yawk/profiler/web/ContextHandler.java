package at.yawk.profiler.web;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class ContextHandler implements Handler {
    private final String path;

    private final List<Handler> handlers = new ArrayList<>();

    public boolean serve(HttpSession session, String contextPath) {
        if (!contextPath.startsWith(this.path)) { return false; }

        String path = contextPath.substring(this.path.length());
        for (Handler handler : handlers) {
            if (handler.serve(session, path)) {
                return true;
            }
        }
        return false;
    }

    public void addContext(Handler child) {
        handlers.add(child);
    }

    public void removeContext(Handler child) {
        handlers.remove(child);
    }
}
