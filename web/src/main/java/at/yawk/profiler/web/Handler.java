package at.yawk.profiler.web;

/**
 * @author yawkat
 */
public interface Handler {
    boolean serve(HttpSession session, String path);
}
