package org.elmo.robella.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务异常基类
 * 所有业务异常都应该继承此类
 */
@Getter
public abstract class BaseBusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Object[] messageArgs;
    private final Map<String, Object> details;
    
    protected BaseBusinessException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.formatMessage(messageArgs));
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
        this.details = new HashMap<>();
    }
    
    protected BaseBusinessException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode.formatMessage(messageArgs), cause);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
        this.details = new HashMap<>();
    }
    
    /**
     * 获取HTTP状态码
     * 子类需要实现此方法来定义对应的HTTP状态码
     */
    public abstract HttpStatus getHttpStatus();
    
    /**
     * 获取错误分类
     * 子类需要实现此方法来定义错误分类
     */
    public abstract ErrorCategory getCategory();
    
    /**
     * 添加详细信息
     * @param key 键
     * @param value 值
     * @return 当前异常实例（支持链式调用）
     */
    public BaseBusinessException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
    
    /**
     * 批量添加详细信息
     * @param details 详细信息映射
     * @return 当前异常实例（支持链式调用）
     */
    public BaseBusinessException addDetails(Map<String, Object> details) {
        this.details.putAll(details);
        return this;
    }
    
    /**
     * 获取格式化后的错误消息
     */
    public String getFormattedMessage() {
        return errorCode.formatMessage(messageArgs);
    }
}