package at.yawk.profiler.web.misc;

import at.yawk.profiler.attach.VmDescriptor;
import at.yawk.profiler.web.AppAspect;
import at.yawk.profiler.web.Component;
import at.yawk.profiler.web.HttpError;
import at.yawk.profiler.web.Page;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author yawkat
 */
@Component
public class MiscAppAspect extends AppAspect {
    @Page(pattern = "list", renderedBy = "list")
    public Object listVms() {
        return getRunning()
                .stream()
                .map(DescriptorData::new)
                .sorted(Comparator.comparingInt(d -> d.pid))
                .collect(Collectors.toList());
    }

    private Collection<VmDescriptor> getRunning() {
        return getApp().getDefaultAttachmentProvider().getRunningDescriptors();
    }

    private class DescriptorData {
        String id;
        int pid;
        String name;

        DescriptorData(VmDescriptor descriptor) {
            id = getApp().getId(descriptor);
            pid = descriptor.getPid();
            name = descriptor.getName();
        }
    }

    @Page(pattern = "vm/(.+)/attach")
    public Object attach(String vmId) throws IOException, InterruptedException {
        VmDescriptor descriptor = getRunning().stream()
                .filter(c -> getApp().getId(c).equals(vmId))
                .findAny()
                .orElseThrow(() -> new HttpError(404, "No such VM"));
        getApp().openAgent(descriptor);
        return "";
    }
}
