package at.yawk.profiler.web;

import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HttpSession {
    private static final String[] STATUS_DESCRIPTIONS;

    static {
        STATUS_DESCRIPTIONS = new String[501];
        Arrays.fill(STATUS_DESCRIPTIONS, "");
        for (NanoHTTPD.Response.Status status : NanoHTTPD.Response.Status.values()) {
            STATUS_DESCRIPTIONS[status.getRequestStatus()] = status.getDescription();
        }
    }

    private final NanoHTTPD.IHTTPSession handle;

    NanoHTTPD.Response response = new NanoHTTPD.Response("");

    public void status(int code) {
        String message = code < STATUS_DESCRIPTIONS.length ? "" : STATUS_DESCRIPTIONS[code];
        status(code, message);
    }

    public void status(int code, String message) {
        try {
            // reflective set because there's no IStatus setter
            Field status = NanoHTTPD.Response.class.getDeclaredField("status");
            status.setAccessible(true);
            status.set(response, new NanoHTTPD.Response.IStatus() {
                @Override
                public int getRequestStatus() {
                    return code;
                }

                @Override
                public String getDescription() {
                    return code + " " + message;
                }
            });
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public void data(String mime, String data) {
        data(mime, data.getBytes(StandardCharsets.UTF_8));
    }

    public void data(String mime, byte[] data) {
        data(mime, new ByteArrayInputStream(data));
    }

    public void data(String mime, InputStream data) {
        response.setData(data);
        response.setMimeType(mime);
    }

    public void header(String key, String value) {
        response.addHeader(key, value);
    }

    public String header(String key) {
        return handle.getHeaders().get(key);
    }
}
