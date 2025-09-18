package org.elmo.robella.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    GUEST("GUEST"),
    USER("USER"),
    ADMIN("ADMIN"),
    ROOT("ROOT");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Role fromValue(String value) {
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return USER; // Default to USER if invalid value
    }

    /**
     * 检查当前角色是否具有所需角色的权限
     * @param requiredRole
     * @return
     */
    public boolean hasPermission(Role requiredRole) {
        return this.ordinal() >= requiredRole.ordinal();
    }

    public boolean isRoot() {
        return this == ROOT;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isUser() {
        return this == USER;
    }

    public boolean isGuest() {
        return this == GUEST;
    }
}