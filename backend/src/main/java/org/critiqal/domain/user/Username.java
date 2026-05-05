package org.critiqal.domain.user;

import org.critiqal.domain.shared.exception.DomainException;

import java.util.regex.Pattern;

public record Username(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 30;
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    public Username {
        if (value == null) {
            throw new DomainException("Username is required");
        }

        value = value.trim();

        if (value.isBlank()) {
            throw new DomainException("Username is required");
        }
        if (value.length() < MIN_LENGTH) {
            throw new DomainException("Username must be at least 3 characters");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainException("Username must be at most 30 characters");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainException("Username can only contain letters, numbers, and underscores");
        }
    }

    public static Username of(String value) {
        return new Username(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
