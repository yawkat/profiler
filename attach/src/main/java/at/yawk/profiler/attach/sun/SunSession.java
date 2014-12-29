package at.yawk.profiler.attach.sun;

import at.yawk.profiler.attach.AttachmentException;
import at.yawk.profiler.attach.Session;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class SunSession implements Session {
    private final com.sun.tools.attach.VirtualMachine vm;

    @Override
    public Properties getSystemProperties() throws AttachmentException {
        try {
            return vm.getSystemProperties();
        } catch (IOException e) {
            throw new AttachmentException(e);
        }
    }

    @Override
    public Properties getAgentProperties() throws AttachmentException {
        try {
            return vm.getAgentProperties();
        } catch (IOException e) {
            throw new AttachmentException(e);
        }
    }

    @Override
    public void loadAgent(Path jarPath) throws AttachmentException {
        loadAgent(jarPath, null);
    }

    @Override
    public void loadAgent(Path jarPath, String options) throws AttachmentException {
        try {
            vm.loadAgent(jarPath.toAbsolutePath().toString(), options);
        } catch (AgentLoadException | AgentInitializationException | IOException e) {
            throw new AttachmentException(e);
        }
    }

    @Override
    public void close() throws AttachmentException {
        try {
            vm.detach();
        } catch (IOException e) {
            throw new AttachmentException(e);
        }
    }
}
