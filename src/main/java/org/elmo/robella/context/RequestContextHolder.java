package org.elmo.robella.context;

import org.elmo.robella.model.entity.VendorModel;

import lombok.Builder;
import lombok.Data;

@Data
public class RequestContextHolder {
    private static final ThreadLocal<RequestContext> contextHolder = new ThreadLocal<>();

    public static void setContext(RequestContext context) {
        contextHolder.set(context);
    }

    public static RequestContext getContext() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }

    @Data
    @Builder
    public static class RequestContext {
        private String requestId;
        private Long userId;
        private Long apiKeyId;
        private String username;
        private String role;
        private String token;
        private String endpointType;
        private String modelKey;
        private Long providerId;
        private VendorModel vendorModel;


    }
}