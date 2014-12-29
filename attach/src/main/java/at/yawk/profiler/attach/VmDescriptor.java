package at.yawk.profiler.attach;

/**
 * @author yawkat
 */
public interface VmDescriptor {
    String getName();

    int getPid() throws UnsupportedOperationException;

    Session attach() throws AttachmentException;
}
