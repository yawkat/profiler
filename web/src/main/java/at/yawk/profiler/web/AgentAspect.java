package at.yawk.profiler.web;

import lombok.Getter;

/**
 * @author yawkat
 */
public abstract class AgentAspect extends Aspect {
    @Getter AgentWrapper agent;
}
