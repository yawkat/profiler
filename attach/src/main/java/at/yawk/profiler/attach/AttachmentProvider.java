package at.yawk.profiler.attach;

import java.util.Collection;

/**
 * @author yawkat
 */
public interface AttachmentProvider {
    VmDescriptor resolveProcess(int pid) throws AttachmentException;

    Collection<VmDescriptor> getRunningDescriptors() throws AttachmentException, UnsupportedOperationException;
}
