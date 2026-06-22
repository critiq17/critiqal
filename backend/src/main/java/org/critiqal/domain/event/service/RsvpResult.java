package org.critiqal.domain.event.service;

import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.RsvpStatus;

public record RsvpResult(Event event, RsvpStatus status) {}
