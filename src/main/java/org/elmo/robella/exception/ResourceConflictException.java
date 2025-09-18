package org.elmo.robella.exception;

/**
 * 资源冲突异常
 * 当资源已存在或冲突时抛出
 */
public class ResourceConflictException extends BusinessLogicException {
    
    public ResourceConflictException(String resourceType) {
        super(ErrorCode.RESOURCE_CONFLICT, resourceType);
        addDetail("resourceType", resourceType);
    }
    
    public ResourceConflictException(String resourceType, String field, Object value) {
        super(ErrorCode.RESOURCE_CONFLICT, resourceType);
        addDetail("resourceType", resourceType);
        addDetail("conflictField", field);
        addDetail("conflictValue", value);
    }
    
    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.CONFLICT;
    }
}