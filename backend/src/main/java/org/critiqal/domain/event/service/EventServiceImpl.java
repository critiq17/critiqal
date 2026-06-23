package org.critiqal.domain.event.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.activity.ActivityEvent;
import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.EventAttendance;
import org.critiqal.domain.event.EventLocationType;
import org.critiqal.domain.event.EventStatus;
import org.critiqal.domain.event.RsvpStatus;
import org.critiqal.domain.event.event.EventCancelledEvent;
import org.critiqal.domain.event.event.EventEndedEvent;
import org.critiqal.domain.event.event.EventPublishedEvent;
import org.critiqal.domain.event.repository.EventAttendanceRepository;
import org.critiqal.domain.event.repository.EventRepository;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.shared.exception.ForbiddenException;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.user.service.UserService;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EventServiceImpl implements EventService {

    private static final Logger log = Logger.getLogger(EventServiceImpl.class);
    private static final int TITLE_MAX = 140;
    private static final int DESC_MAX = 4000;
    private static final Duration DEFAULT_DURATION = Duration.ofHours(2);

    private final EventRepository eventRepo;
    private final EventAttendanceRepository attendanceRepo;
    private final EventPermissionService permission;
    private final UserService userService;
    private final jakarta.enterprise.event.Event<EventPublishedEvent> publishedEvent;
    private final jakarta.enterprise.event.Event<EventCancelledEvent> cancelledEvent;
    private final jakarta.enterprise.event.Event<EventEndedEvent> endedEvent;
    private final jakarta.enterprise.event.Event<ActivityEvent> activityEvent;

    public EventServiceImpl(EventRepository eventRepo,
                            EventAttendanceRepository attendanceRepo,
                            EventPermissionService permission,
                            UserService userService,
                            jakarta.enterprise.event.Event<EventPublishedEvent> publishedEvent,
                            jakarta.enterprise.event.Event<EventCancelledEvent> cancelledEvent,
                            jakarta.enterprise.event.Event<EventEndedEvent> endedEvent,
                            jakarta.enterprise.event.Event<ActivityEvent> activityEvent) {
        this.eventRepo = eventRepo;
        this.attendanceRepo = attendanceRepo;
        this.permission = permission;
        this.userService = userService;
        this.publishedEvent = publishedEvent;
        this.cancelledEvent = cancelledEvent;
        this.endedEvent = endedEvent;
        this.activityEvent = activityEvent;
    }

    @Override
    @Transactional
    public Event createEvent(UUID hostId, CreateEventCommand cmd) {
        if (!permission.canCreateEvents(hostId)) {
            throw new ForbiddenException("You have not earned the right to create events yet");
        }
        validate(cmd.title(), cmd.description(), cmd.startsAt(), cmd.endsAt(), cmd.capacity());

        var host = userService.getById(hostId);
        var event = new Event();
        event.host = host;
        event.title = cmd.title().strip();
        event.description = normalize(cmd.description());
        event.coverImageUrl = cmd.coverImageUrl();
        event.locationType = cmd.locationType() != null ? cmd.locationType() : EventLocationType.DISCORD;
        event.locationValue = cmd.locationValue();
        event.startsAt = cmd.startsAt();
        event.endsAt = resolveEnd(cmd.startsAt(), cmd.endsAt());
        event.capacity = cmd.capacity();
        event.status = EventStatus.DRAFT;

        event = eventRepo.save(event);

        if (cmd.publishNow()) {
            event = doPublish(event);
        }
        return event;
    }

    @Override
    @Transactional
    public Event updateEvent(UUID eventId, UUID requesterId, UpdateEventCommand cmd) {
        var event = requireManageable(eventId, requesterId);
        if (event.status == EventStatus.ENDED || event.status == EventStatus.CANCELLED) {
            throw new DomainException("Cannot edit a finished event");
        }
        validate(cmd.title(), cmd.description(), cmd.startsAt(), cmd.endsAt(), cmd.capacity());

        event.title = cmd.title().strip();
        event.description = normalize(cmd.description());
        event.coverImageUrl = cmd.coverImageUrl();
        event.locationType = cmd.locationType() != null ? cmd.locationType() : event.locationType;
        event.locationValue = cmd.locationValue();
        event.startsAt = cmd.startsAt();
        event.endsAt = resolveEnd(cmd.startsAt(), cmd.endsAt());
        event.capacity = cmd.capacity();
        event.updatedAt = Instant.now();
        return event;
    }

    @Override
    @Transactional
    public Event publishEvent(UUID eventId, UUID requesterId) {
        var event = requireManageable(eventId, requesterId);
        if (event.status != EventStatus.DRAFT) {
            throw new DomainException("Only a draft can be published");
        }
        return doPublish(event);
    }

    private Event doPublish(Event event) {
        event.status = EventStatus.PUBLISHED;
        event.updatedAt = Instant.now();
        publishedEvent.fireAsync(new EventPublishedEvent(event.id));
        return event;
    }

    @Override
    @Transactional
    public Event cancelEvent(UUID eventId, UUID requesterId) {
        var event = requireManageable(eventId, requesterId);
        if (event.status == EventStatus.CANCELLED) {
            return event; // idempotent: a repeat cancel is a no-op, not an error
        }
        if (event.status == EventStatus.ENDED) {
            throw new DomainException("Event is already finished");
        }
        event.status = EventStatus.CANCELLED;
        event.updatedAt = Instant.now();
        cancelledEvent.fireAsync(new EventCancelledEvent(event.id));
        return event;
    }

    @Override
    @Transactional
    public void deleteEvent(UUID eventId, UUID requesterId) {
        var event = requireManageable(eventId, requesterId);
        if (event.status != EventStatus.DRAFT) {
            throw new DomainException("Only a draft can be deleted; cancel the event instead");
        }
        event.delete();
    }

    @Override
    public Event getById(UUID eventId) {
        return eventRepo.findByIdOptional(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Override
    public Page<Event> getUpcoming(int page, int size) {
        var content = eventRepo.findUpcoming(page, size);
        return Page.of(content, page, size, eventRepo.countUpcoming());
    }

    @Override
    public Page<Event> getByHost(UUID hostId, int page, int size) {
        var content = eventRepo.findByHost(hostId, page, size);
        return Page.of(content, page, size, eventRepo.countByHost(hostId));
    }

    @Override
    public Page<EventAttendance> getAttendees(UUID eventId, int page, int size) {
        getById(eventId); // 404 if missing
        var content = attendanceRepo.findByEvent(eventId, page, size);
        return Page.of(content, page, size, attendanceRepo.countByEvent(eventId));
    }

    @Override
    @Transactional
    public RsvpResult rsvp(UUID eventId, UUID userId, RsvpStatus status) {
        var event = getById(eventId);
        if (!event.isOpenForRsvp()) {
            throw new DomainException("RSVP is closed for this event");
        }
        var existing = attendanceRepo.findByEventAndUser(eventId, userId).orElse(null);
        boolean wasGoing = existing != null && existing.status == RsvpStatus.GOING;
        boolean nowGoing = status == RsvpStatus.GOING;

        if (nowGoing && !wasGoing && isFull(event)) {
            throw new DomainException("Event is full");
        }

        if (existing == null) {
            var attendance = new EventAttendance();
            attendance.event = event;
            attendance.user = userService.getById(userId);
            attendance.status = status;
            attendanceRepo.save(attendance);
        } else {
            existing.status = status;
        }

        if (nowGoing && !wasGoing) {
            eventRepo.incrementAttendees(eventId, +1);
        } else if (!nowGoing && wasGoing) {
            eventRepo.incrementAttendees(eventId, -1);
        }
        return new RsvpResult(event, status);
    }

    @Override
    @Transactional
    public void cancelRsvp(UUID eventId, UUID userId) {
        var existing = attendanceRepo.findByEventAndUser(eventId, userId).orElse(null);
        if (existing == null) {
            return;
        }
        boolean wasGoing = existing.status == RsvpStatus.GOING;
        attendanceRepo.delete(eventId, userId);
        if (wasGoing) {
            eventRepo.incrementAttendees(eventId, -1);
        }
    }

    @Override
    public Optional<RsvpStatus> viewerStatus(UUID eventId, UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return attendanceRepo.findByEventAndUser(eventId, userId).map(a -> a.status);
    }

    // ---- lifecycle ----

    @Override
    @Transactional
    public int startDueEvents() {
        var now = Instant.now();
        var due = eventRepo.findByStatusStartingBefore(EventStatus.PUBLISHED, now);
        for (var event : due) {
            event.status = EventStatus.LIVE;
            event.updatedAt = now;
        }
        if (!due.isEmpty()) {
            log.infof("EventLifecycle: %d events went LIVE", due.size());
        }
        return due.size();
    }

    @Override
    @Transactional
    public int endDueEvents() {
        var now = Instant.now();
        var due = eventRepo.findByStatusEndingBefore(
                List.of(EventStatus.LIVE, EventStatus.PUBLISHED), now);
        for (var event : due) {
            endEvent(event, now);
        }
        if (!due.isEmpty()) {
            log.infof("EventLifecycle: %d events ENDED", due.size());
        }
        return due.size();
    }

    private void endEvent(Event event, Instant now) {
        event.status = EventStatus.ENDED;
        event.updatedAt = now;

        // host buff
        activityEvent.fireAsync(new ActivityEvent(event.host.id, ActivityEvent.ActivityType.EVENT_HOSTED));
        // attendee buffs (those who said GOING)
        for (var attendance : attendanceRepo.findGoingByEvent(event.id)) {
            activityEvent.fireAsync(
                    new ActivityEvent(attendance.user.id, ActivityEvent.ActivityType.EVENT_ATTENDED));
        }
        endedEvent.fireAsync(new EventEndedEvent(event.id));
    }

    // ---- helpers ----

    private Event requireManageable(UUID eventId, UUID requesterId) {
        var event = getById(eventId);
        if (!event.isManageableBy(requesterId)) {
            throw new ForbiddenException("Not your event");
        }
        return event;
    }

    private boolean isFull(Event event) {
        return event.capacity != null && event.attendeeCount >= event.capacity;
    }

    private Instant resolveEnd(Instant start, Instant end) {
        if (end != null) {
            return end;
        }
        return start.plus(DEFAULT_DURATION);
    }

    private void validate(String title, String description, Instant startsAt, Instant endsAt, Integer capacity) {
        if (title == null || title.isBlank()) {
            throw new DomainException("Title is required");
        }
        if (title.strip().length() > TITLE_MAX) {
            throw new DomainException("Title too long");
        }
        if (description != null && description.length() > DESC_MAX) {
            throw new DomainException("Description too long");
        }
        if (startsAt == null) {
            throw new DomainException("Start time is required");
        }
        if (endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new DomainException("End time must be after start");
        }
        if (capacity != null && capacity <= 0) {
            throw new DomainException("Capacity must be positive");
        }
    }

    private String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.replace("\r\n", "\n").replace("\r", "\n").strip();
        return s.replaceAll("\n{3,}", "\n\n");
    }
}
