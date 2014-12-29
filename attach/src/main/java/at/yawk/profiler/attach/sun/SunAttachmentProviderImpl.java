package at.yawk.profiler.attach.sun;

import at.yawk.profiler.attach.AttachmentException;
import at.yawk.profiler.attach.AttachmentProvider;
import at.yawk.profiler.attach.VmDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yawkat
 */
class SunAttachmentProviderImpl implements AttachmentProvider {
    @Override
    public VmDescriptor resolveProcess(int pid) throws AttachmentException {
        for (VmDescriptor descriptor : getRunningDescriptors()) {
            if (descriptor.getPid() == pid) {
                return descriptor;
            }
        }
        throw new AttachmentException("VM not found");
    }

    @Override
    public Collection<VmDescriptor> getRunningDescriptors() throws AttachmentException, UnsupportedOperationException {
        List<VmDescriptor> descriptors = new ArrayList<>();

        for (AttachProvider provider : AttachProvider.providers()) {
            descriptors.addAll(
                    provider.listVirtualMachines().stream()
                            .map(SunDescriptor::new)
                            .collect(Collectors.toList())
            );
        }

        return descriptors;
    }

}
