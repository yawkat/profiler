package at.yawk.profiler.attach;

/**
 * @author yawkat
 */
public interface VmDescriptor {
    AttachmentProvider getProvider();

    String getName();

    int getPid() throws UnsupportedOperationException;

    Session attach() throws AttachmentException;
}
