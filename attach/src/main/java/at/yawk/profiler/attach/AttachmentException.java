package at.yawk.profiler.attach;

/**
 * @author yawkat
 */
public class AttachmentException extends RuntimeException {
    public AttachmentException(String message) {
        super(message);
    }

    public AttachmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public AttachmentException(Throwable cause) {
        super(cause);
    }
}
