package org.acme.infra.telegram;

public enum AlertLevel {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR");

    public final String level;
    AlertLevel(String level) { this.level = level; }
}
