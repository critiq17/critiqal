import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { PageResponse } from '$lib/types';
import type {
	CommunityEvent,
	CreateEventRequest,
	EventAttendee,
	RsvpStatus
} from '$lib/types/event';

// Service for event endpoints
export const eventService = {
	getUpcoming(page = 0, size = 20): Promise<PageResponse<CommunityEvent>> {
		return apiClient.get<PageResponse<CommunityEvent>>(
			`${API.events.list}?page=${page}&size=${size}`
		);
	},
	getMine(page = 0, size = 20): Promise<PageResponse<CommunityEvent>> {
		return apiClient.get<PageResponse<CommunityEvent>>(
			`${API.events.mine}?page=${page}&size=${size}`
		);
	},
	getById(id: string): Promise<CommunityEvent> {
		return apiClient.get<CommunityEvent>(API.events.byId(id));
	},
	getAttendees(id: string, page = 0, size = 50): Promise<PageResponse<EventAttendee>> {
		return apiClient.get<PageResponse<EventAttendee>>(
			`${API.events.attendees(id)}?page=${page}&size=${size}`
		);
	},
	canCreate(): Promise<{ canCreateEvents: boolean }> {
		return apiClient.get<{ canCreateEvents: boolean }>(API.events.permissions);
	},
	create(req: CreateEventRequest): Promise<CommunityEvent> {
		return apiClient.post<CommunityEvent>(API.events.list, req);
	},
	publish(id: string): Promise<CommunityEvent> {
		return apiClient.post<CommunityEvent>(API.events.publish(id), {});
	},
	cancel(id: string): Promise<CommunityEvent> {
		return apiClient.post<CommunityEvent>(API.events.cancel(id), {});
	},
	rsvp(id: string, status: RsvpStatus): Promise<CommunityEvent> {
		return apiClient.post<CommunityEvent>(API.events.rsvp(id), { status });
	},
	cancelRsvp(id: string): Promise<void> {
		return apiClient.delete(API.events.rsvp(id));
	}
};
