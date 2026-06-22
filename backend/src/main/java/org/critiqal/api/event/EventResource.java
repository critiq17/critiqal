package org.critiqal.api.event;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.critiqal.api.CurrentUser;
import org.critiqal.api.event.request.CreateEventRequest;
import org.critiqal.api.event.request.RsvpRequest;
import org.critiqal.api.event.request.UpdateEventRequest;
import org.critiqal.api.event.response.AttendeeDTO;
import org.critiqal.api.event.response.EventDTO;
import org.critiqal.api.security.RequireVerifiedEmail;
import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.RsvpStatus;
import org.critiqal.domain.event.service.CreateEventCommand;
import org.critiqal.domain.event.service.EventPermissionService;
import org.critiqal.domain.event.service.EventService;
import org.critiqal.domain.event.service.UpdateEventCommand;
import org.critiqal.domain.shared.pagination.Page;
import org.critiqal.domain.shared.pagination.PageRequest;

import java.util.Map;
import java.util.UUID;

@Path("/api/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    private final EventService eventService;
    private final EventPermissionService permission;
    private final CurrentUser currentUser;

    public EventResource(EventService eventService,
                         EventPermissionService permission,
                         CurrentUser currentUser) {
        this.eventService = eventService;
        this.permission = permission;
        this.currentUser = currentUser;
    }

    @GET
    public Page<EventDTO> upcoming(@BeanParam PageRequest pageRequest) {
        var page = eventService.getUpcoming(pageRequest.page(), pageRequest.size());
        return enrich(page);
    }

    @GET
    @Path("/mine")
    @Authenticated
    public Page<EventDTO> mine(@BeanParam PageRequest pageRequest) {
        var page = eventService.getByHost(currentUser.id(), pageRequest.page(), pageRequest.size());
        return enrich(page);
    }

    @GET
    @Path("/permissions")
    public Map<String, Boolean> permissions() {
        UUID uid = currentUser.idOrNull();
        return Map.of("canCreateEvents", uid != null && permission.canCreateEvents(uid));
    }

    @GET
    @Path("/{id}")
    public EventDTO getOne(@PathParam("id") UUID id) {
        var event = eventService.getById(id);
        return toDto(event);
    }

    @GET
    @Path("/{id}/attendees")
    public Page<AttendeeDTO> attendees(@PathParam("id") UUID id, @BeanParam PageRequest pageRequest) {
        return eventService.getAttendees(id, pageRequest.page(), pageRequest.size())
                .map(AttendeeDTO::from);
    }

    @POST
    @Authenticated
    @RequireVerifiedEmail
    public Response create(CreateEventRequest req) {
        var cmd = new CreateEventCommand(
                req.title(), req.description(), req.coverImageUrl(),
                req.locationType(), req.locationValue(),
                req.startsAt(), req.endsAt(), req.capacity(),
                Boolean.TRUE.equals(req.publishNow()));
        var event = eventService.createEvent(currentUser.id(), cmd);
        return Response.status(Response.Status.CREATED).entity(toDto(event)).build();
    }

    @PATCH
    @Path("/{id}")
    @Authenticated
    @RequireVerifiedEmail
    public EventDTO update(@PathParam("id") UUID id, UpdateEventRequest req) {
        var cmd = new UpdateEventCommand(
                req.title(), req.description(), req.coverImageUrl(),
                req.locationType(), req.locationValue(),
                req.startsAt(), req.endsAt(), req.capacity());
        var event = eventService.updateEvent(id, currentUser.id(), cmd);
        return toDto(event);
    }

    @POST
    @Path("/{id}/publish")
    @Authenticated
    @RequireVerifiedEmail
    public EventDTO publish(@PathParam("id") UUID id) {
        return toDto(eventService.publishEvent(id, currentUser.id()));
    }

    @POST
    @Path("/{id}/cancel")
    @Authenticated
    @RequireVerifiedEmail
    public EventDTO cancel(@PathParam("id") UUID id) {
        return toDto(eventService.cancelEvent(id, currentUser.id()));
    }

    @DELETE
    @Path("/{id}")
    @Authenticated
    public Response delete(@PathParam("id") UUID id) {
        eventService.deleteEvent(id, currentUser.id());
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/rsvp")
    @Authenticated
    @RequireVerifiedEmail
    public EventDTO rsvp(@PathParam("id") UUID id, RsvpRequest req) {
        RsvpStatus status = req != null && req.status() != null ? req.status() : RsvpStatus.GOING;
        var result = eventService.rsvp(id, currentUser.id(), status);
        return EventDTO.from(result.event(), result.status(),
                result.event().isManageableBy(currentUser.id()));
    }

    @DELETE
    @Path("/{id}/rsvp")
    @Authenticated
    public Response cancelRsvp(@PathParam("id") UUID id) {
        eventService.cancelRsvp(id, currentUser.id());
        return Response.noContent().build();
    }

    // ---- helpers ----

    private EventDTO toDto(Event event) {
        UUID viewer = currentUser.idOrNull();
        var rsvp = eventService.viewerStatus(event.id, viewer).orElse(null);
        return EventDTO.from(event, rsvp, viewer != null && event.isManageableBy(viewer));
    }

    private Page<EventDTO> enrich(Page<Event> page) {
        UUID viewer = currentUser.idOrNull();
        return page.map(event -> {
            var rsvp = viewer != null
                    ? eventService.viewerStatus(event.id, viewer).orElse(null)
                    : null;
            return EventDTO.from(event, rsvp, viewer != null && event.isManageableBy(viewer));
        });
    }
}
