package org.elmo.robella.model.common;

public enum Role {
    GUEST(0),
    USER(1),
    ADMIN(10),
    ROOT(100);

    private final int value;

    Role(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Role fromValue(int value) {
        for (Role role : Role.values()) {
            if (role.value == value) {
                return role;
            }
        }
        return USER; // Default to USER if invalid value
    }

    public static Role fromValue(String value) {
        try {
            return fromValue(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return USER; // Default to USER if invalid format
        }
    }
}