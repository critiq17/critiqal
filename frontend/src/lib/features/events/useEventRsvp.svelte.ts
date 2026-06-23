import { eventService } from '$lib/services/event.service';
import type { CommunityEvent, RsvpStatus } from '$lib/types/event';

/**
 * Optimistic RSVP toggle — mirrors the pattern of UseLike: update state first,
 * reconcile with the server response, roll back on failure.
 */
export class UseEventRsvp {
	status = $state<RsvpStatus | null>(null);
	count = $state(0);
	pending = $state(false);

	private eventId: string;

	constructor(eventId: string, initial: CommunityEvent) {
		this.eventId = eventId;
		this.status = initial.viewerRsvp;
		this.count = initial.attendeeCount;
	}

	async setGoing(): Promise<void> {
		await this.run('GOING');
	}

	async setInterested(): Promise<void> {
		await this.run('INTERESTED');
	}

	async clear(): Promise<void> {
		const prevStatus = this.status;
		const prevCount = this.count;
		if (prevStatus === 'GOING') this.count = Math.max(0, this.count - 1);
		this.status = null;
		this.pending = true;
		try {
			await eventService.cancelRsvp(this.eventId);
		} catch {
			this.status = prevStatus;
			this.count = prevCount;
		} finally {
			this.pending = false;
		}
	}

	private async run(next: RsvpStatus): Promise<void> {
		const prevStatus = this.status;
		const prevCount = this.count;
		const wasGoing = prevStatus === 'GOING';
		const nowGoing = next === 'GOING';
		if (nowGoing && !wasGoing) this.count += 1;
		if (!nowGoing && wasGoing) this.count = Math.max(0, this.count - 1);
		this.status = next;
		this.pending = true;
		try {
			const updated = await eventService.rsvp(this.eventId, next);
			this.status = updated.viewerRsvp;
			this.count = updated.attendeeCount;
		} catch {
			this.status = prevStatus;
			this.count = prevCount;
		} finally {
			this.pending = false;
		}
	}
}
