package at.yawk.profiler.web;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
class Server extends NanoHTTPD {
    private static final Gson GSON = new Gson();

    @Setter private Handler rootHandler = new ContextHandler("");

    public Server(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        log.debug("Requested {}", session.getUri());

        HttpSession wrapper = new HttpSession(session);

        boolean printError = false;

        String path = session.getUri();
        if (path.endsWith("/")) { path += "index.html"; }
        try {
            if (!rootHandler.serve(wrapper, path)) {
                wrapper.response.setStatus(Response.Status.NOT_FOUND);
                printError = true;
            }
        } catch (Throwable t) {
            log.warn("Error while serving request to " + session.getUri(), t);
            wrapper.response.setStatus(Response.Status.INTERNAL_ERROR);
            printError = true;
        }

        if (printError) {
            Response.IStatus status = wrapper.response.getStatus();
            Map<String, Object> statusMap = ImmutableMap.of(
                    "code", status.getRequestStatus(),
                    "message", status.getDescription().substring(status.getDescription().indexOf(' ') + 1)
            );
            if (path.endsWith(".html")) {
                try {
                    wrapper.data("text/html",
                                 TemplateManager.getInstance().compile("error", statusMap));
                } catch (IOException e) {
                    log.warn("Error while serving error page to " + session.getUri());
                }
            } else {
                wrapper.data("application/json", GSON.toJson(statusMap));
            }
        }

        return wrapper.response;
    }
}
