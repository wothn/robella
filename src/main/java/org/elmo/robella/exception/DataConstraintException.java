package org.elmo.robella.exception;

/**
 * 数据约束异常
 * 当数据违反约束条件时抛出（如唯一性约束、外键约束等）
 */
public class DataConstraintException extends ValidationException {
    
    public DataConstraintException(String constraintName) {
        super(ErrorCode.DATA_CONSTRAINT_VIOLATION);
        addDetail("constraint", constraintName);
    }
    
    public DataConstraintException(String constraintName, String details) {
        super(ErrorCode.DATA_CONSTRAINT_VIOLATION);
        addDetail("constraint", constraintName);
        addDetail("details", details);
    }
}