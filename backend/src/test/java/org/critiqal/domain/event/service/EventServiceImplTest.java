package org.critiqal.domain.event.service;

import jakarta.enterprise.event.Event;
import org.critiqal.domain.activity.ActivityEvent;
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
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventServiceImplTest {

    private final EventRepository eventRepo = mock(EventRepository.class);
    private final EventAttendanceRepository attendanceRepo = mock(EventAttendanceRepository.class);
    private final EventPermissionService permission = mock(EventPermissionService.class);
    private final UserService userService = mock(UserService.class);
    @SuppressWarnings("unchecked")
    private final Event<EventPublishedEvent> publishedEvent = mock(Event.class);
    @SuppressWarnings("unchecked")
    private final Event<EventCancelledEvent> cancelledEvent = mock(Event.class);
    @SuppressWarnings("unchecked")
    private final Event<EventEndedEvent> endedEvent = mock(Event.class);
    @SuppressWarnings("unchecked")
    private final Event<ActivityEvent> activityEvent = mock(Event.class);

    private EventServiceImpl service;

    private UUID hostId;
    private User host;

    @BeforeEach
    void setUp() {
        service = new EventServiceImpl(eventRepo, attendanceRepo, permission, userService,
                publishedEvent, cancelledEvent, endedEvent, activityEvent);
        hostId = UUID.randomUUID();
        host = new User();
        host.id = hostId;
        host.username = "gigachad";
    }

    private CreateEventCommand validCommand(boolean publish) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        return new CreateEventCommand("Run club", "Let's go", null,
                EventLocationType.DISCORD, "voice-1", start, start.plus(2, ChronoUnit.HOURS),
                null, publish);
    }

    @Test
    void create_deniedWithoutPermission() {
        when(permission.canCreateEvents(hostId)).thenReturn(false);

        assertThatThrownBy(() -> service.createEvent(hostId, validCommand(false)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(eventRepo);
    }

    @Test
    void create_persistsDraft_whenNotPublishing() {
        when(permission.canCreateEvents(hostId)).thenReturn(true);
        when(userService.getById(hostId)).thenReturn(host);
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var event = service.createEvent(hostId, validCommand(false));

        assertThat(event.status).isEqualTo(EventStatus.DRAFT);
        assertThat(event.host).isSameAs(host);
        verify(publishedEvent, never()).fireAsync(any());
    }

    @Test
    void create_publishesAndFiresEvent_whenPublishNow() {
        when(permission.canCreateEvents(hostId)).thenReturn(true);
        when(userService.getById(hostId)).thenReturn(host);
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var event = service.createEvent(hostId, validCommand(true));

        assertThat(event.status).isEqualTo(EventStatus.PUBLISHED);
        verify(publishedEvent).fireAsync(any(EventPublishedEvent.class));
    }

    @Test
    void create_defaultsEndTime_whenMissing() {
        when(permission.canCreateEvents(hostId)).thenReturn(true);
        when(userService.getById(hostId)).thenReturn(host);
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var cmd = new CreateEventCommand("t", "d", null, EventLocationType.IRL, "gym",
                start, null, 10, false);

        var event = service.createEvent(hostId, cmd);

        assertThat(event.endsAt).isEqualTo(start.plus(2, ChronoUnit.HOURS));
    }

    @Test
    void create_rejectsEndBeforeStart() {
        when(permission.canCreateEvents(hostId)).thenReturn(true);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var cmd = new CreateEventCommand("t", "d", null, EventLocationType.DISCORD, "c",
                start, start.minus(1, ChronoUnit.HOURS), null, false);

        assertThatThrownBy(() -> service.createEvent(hostId, cmd))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rsvp_full_throws() {
        var event = published();
        event.capacity = 1;
        event.attendeeCount = 1;
        when(eventRepo.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(attendanceRepo.findByEventAndUser(eq(event.id), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rsvp(event.id, UUID.randomUUID(), RsvpStatus.GOING))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("full");
    }

    @Test
    void rsvp_going_incrementsAttendeesOnce() {
        var event = published();
        UUID user = UUID.randomUUID();
        when(eventRepo.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(attendanceRepo.findByEventAndUser(event.id, user)).thenReturn(Optional.empty());
        when(userService.getById(user)).thenReturn(host);

        service.rsvp(event.id, user, RsvpStatus.GOING);

        verify(eventRepo).incrementAttendees(event.id, +1);
        verify(attendanceRepo).save(any(EventAttendance.class));
    }

    @Test
    void rsvp_switchInterestedToGoing_adjustsCount() {
        var event = published();
        UUID user = UUID.randomUUID();
        var existing = new EventAttendance();
        existing.status = RsvpStatus.INTERESTED;
        when(eventRepo.findByIdOptional(event.id)).thenReturn(Optional.of(event));
        when(attendanceRepo.findByEventAndUser(event.id, user)).thenReturn(Optional.of(existing));

        service.rsvp(event.id, user, RsvpStatus.GOING);

        assertThat(existing.status).isEqualTo(RsvpStatus.GOING);
        verify(eventRepo).incrementAttendees(event.id, +1);
    }

    @Test
    void rsvp_closedEvent_throws() {
        var event = published();
        event.status = EventStatus.ENDED;
        when(eventRepo.findByIdOptional(event.id)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.rsvp(event.id, UUID.randomUUID(), RsvpStatus.GOING))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void cancelRsvp_decrementsOnlyWhenWasGoing() {
        var event = published();
        UUID user = UUID.randomUUID();
        var existing = new EventAttendance();
        existing.status = RsvpStatus.GOING;
        when(attendanceRepo.findByEventAndUser(event.id, user)).thenReturn(Optional.of(existing));
        when(attendanceRepo.delete(event.id, user)).thenReturn(true);

        service.cancelRsvp(event.id, user);

        verify(eventRepo).incrementAttendees(event.id, -1);
    }

    @Test
    void endDueEvents_firesHostAndAttendeeBuffs() {
        var event = published();
        event.status = EventStatus.LIVE;
        var attendee = new EventAttendance();
        attendee.user = new User();
        attendee.user.id = UUID.randomUUID();
        attendee.status = RsvpStatus.GOING;

        when(eventRepo.findByStatusEndingBefore(anyList(), any())).thenReturn(List.of(event));
        when(attendanceRepo.findGoingByEvent(event.id)).thenReturn(List.of(attendee));

        int ended = service.endDueEvents();

        assertThat(ended).isEqualTo(1);
        assertThat(event.status).isEqualTo(EventStatus.ENDED);

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(activityEvent, times(2)).fireAsync(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ActivityEvent::type)
                .containsExactlyInAnyOrder(
                        ActivityEvent.ActivityType.EVENT_HOSTED,
                        ActivityEvent.ActivityType.EVENT_ATTENDED);
        verify(endedEvent).fireAsync(any(EventEndedEvent.class));
    }

    @Test
    void cancelEvent_firesCancelledEvent() {
        var event = published();
        when(eventRepo.findByIdOptional(event.id)).thenReturn(Optional.of(event));

        service.cancelEvent(event.id, hostId);

        assertThat(event.status).isEqualTo(EventStatus.CANCELLED);
        verify(cancelledEvent).fireAsync(any(EventCancelledEvent.class));
    }

    @Test
    void update_byNonHost_forbidden() {
        var event = published();
        when(eventRepo.findByIdOptional(event.id)).thenReturn(Optional.of(event));

        var cmd = new UpdateEventCommand("x", null, null, EventLocationType.DISCORD, null,
                Instant.now().plus(1, ChronoUnit.DAYS), null, null);

        assertThatThrownBy(() -> service.updateEvent(event.id, UUID.randomUUID(), cmd))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void publish_nonDraft_throws() {
        var event = published(); // already PUBLISHED
        when(eventRepo.findByIdOptional(event.id)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publishEvent(event.id, hostId))
                .isInstanceOf(DomainException.class);
    }

    private org.critiqal.domain.event.Event published() {
        var event = new org.critiqal.domain.event.Event();
        event.id = UUID.randomUUID();
        event.host = host;
        event.title = "t";
        event.startsAt = Instant.now().plus(1, ChronoUnit.HOURS);
        event.endsAt = Instant.now().plus(3, ChronoUnit.HOURS);
        event.status = EventStatus.PUBLISHED;
        return event;
    }
}
