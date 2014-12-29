package at.yawk.profiler.web;

import lombok.Getter;

/**
 * @author yawkat
 */
public abstract class AppAspect extends Aspect {
    @Getter App app;
}
