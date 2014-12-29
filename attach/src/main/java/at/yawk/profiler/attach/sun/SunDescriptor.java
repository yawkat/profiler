package at.yawk.profiler.attach.sun;

import at.yawk.profiler.attach.AttachmentException;
import at.yawk.profiler.attach.AttachmentProvider;
import at.yawk.profiler.attach.Session;
import at.yawk.profiler.attach.VmDescriptor;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.IOException;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@EqualsAndHashCode
@RequiredArgsConstructor
class SunDescriptor implements VmDescriptor {
    final VirtualMachineDescriptor descriptor;

    @Override
    public AttachmentProvider getProvider() {
        return SunAttachmentProvider.getInstance();
    }

    @Override
    public String getName() {
        return descriptor.displayName();
    }

    @Override
    public int getPid() throws UnsupportedOperationException {
        return Integer.parseInt(descriptor.id());
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Session attach() throws AttachmentException {
        try {
            VirtualMachine vm = descriptor.provider().attachVirtualMachine(descriptor);
            return new SunSession(vm);
        } catch (AttachNotSupportedException | IOException e) {
            throw new AttachmentException(e);
        }
    }
}
