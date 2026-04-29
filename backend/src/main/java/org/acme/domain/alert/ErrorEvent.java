package org.acme.domain.alert;

public record ErrorEvent(String title, String details) {
    public static ErrorEvent of(String title, Throwable ex) {
        return new ErrorEvent(title,
                ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
}
