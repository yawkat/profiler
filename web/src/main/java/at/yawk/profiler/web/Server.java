package at.yawk.profiler.web;

import fi.iki.elonen.NanoHTTPD;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
class Server extends NanoHTTPD {
    @Setter private Handler rootHandler = new ContextHandler("");

    public Server(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        log.debug("Requested {}", session.getUri());

        HttpSession wrapper = new HttpSession(session);

        try {
            String path = session.getUri();
            if (path.endsWith("/")) { path += "index.html"; }
            if (!rootHandler.serve(wrapper, path)) {
                wrapper.response.setStatus(Response.Status.NOT_FOUND);
            }
        } catch (Throwable t) {
            log.warn("Error while serving request to " + session.getUri(), t);
            wrapper.response.setStatus(Response.Status.INTERNAL_ERROR);
        }

        return wrapper.response;
    }
}
