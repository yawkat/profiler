package at.yawk.profiler.attach;

import java.nio.file.Path;
import java.util.Properties;

/**
 * @author yawkat
 */
public interface Session extends AutoCloseable {
    Properties getSystemProperties() throws AttachmentException;

    Properties getAgentProperties() throws AttachmentException;

    void loadAgent(Path jarPath) throws AttachmentException;

    void loadAgent(Path jarPath, String options) throws AttachmentException;

    @Override
    void close() throws AttachmentException;
}
