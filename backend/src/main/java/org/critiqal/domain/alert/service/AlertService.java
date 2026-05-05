package org.critiqal.domain.alert.service;

import org.critiqal.infra.telegram.AlertLevel;

public interface AlertService {
    public void send(AlertLevel level, String title, String details);
    public void info(String title, String details);
    public void warn(String title, String details);
    public void error(String title, String details);
    public void error(String title, Throwable ex);
}
