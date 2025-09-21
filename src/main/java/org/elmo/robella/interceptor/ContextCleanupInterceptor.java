package org.elmo.robella.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.elmo.robella.context.RequestContextHolder;
import org.springframework.lang.NonNull;

@Component
@Slf4j
public class ContextCleanupInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                              @NonNull Object handler, Exception ex) throws Exception {
        RequestContextHolder.clear();
        log.debug("ThreadLocal context cleared for request: {}", request.getRequestURI());
    }
}