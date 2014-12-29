package at.yawk.profiler.web;

import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class HttpError extends RuntimeException {
    final int code;
    final Object message;

    public HttpError(Object message) {
        this(500, message);
    }
}
