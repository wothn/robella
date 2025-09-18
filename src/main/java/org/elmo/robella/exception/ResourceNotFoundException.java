package org.elmo.robella.exception;

/**
 * 资源不存在异常
 * 当请求的资源不存在时抛出
 */
public class ResourceNotFoundException extends BusinessLogicException {
    
    public ResourceNotFoundException(String resourceType, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resourceType);
        addDetail("resourceType", resourceType);
        addDetail("resourceId", id);
    }
    
    public ResourceNotFoundException(String resourceType) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resourceType);
        addDetail("resourceType", resourceType);
    }
    
    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.NOT_FOUND;
    }
}