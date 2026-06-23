// Shared state for the mini-app Events tab. The list, detail overlay, and
// create overlay all read and mutate through here so an RSVP, a publish, a
// cancel, or a new event stays consistent across every surface without
// re-fetching. Updates are immutable — every mutation builds new arrays.

import { eventService } from '$lib/services/event.service';
import type { CommunityEvent } from '$lib/types/event';

const PAGE_SIZE = 40;

class MobileEventsStore {
	upcoming = $state<CommunityEvent[]>([]);
	upcomingLoading = $state(false);
	upcomingError = $state<string | null>(null);
	private upcomingLoaded = false;

	mine = $state<CommunityEvent[]>([]);
	mineLoading = $state(false);
	mineError = $state<string | null>(null);
	private mineLoaded = false;

	async loadUpcoming(force = false): Promise<void> {
		if (this.upcomingLoaded && !force) return;
		this.upcomingLoading = true;
		this.upcomingError = null;
		try {
			const page = await eventService.getUpcoming(0, PAGE_SIZE);
			this.upcoming = page.content;
			this.upcomingLoaded = true;
		} catch (e) {
			this.upcomingError = e instanceof Error ? e.message : 'Failed to load events';
		} finally {
			this.upcomingLoading = false;
		}
	}

	async loadMine(force = false): Promise<void> {
		if (this.mineLoaded && !force) return;
		this.mineLoading = true;
		this.mineError = null;
		try {
			const page = await eventService.getMine(0, PAGE_SIZE);
			this.mine = page.content;
			this.mineLoaded = true;
		} catch (e) {
			this.mineError = e instanceof Error ? e.message : 'Failed to load your events';
		} finally {
			this.mineLoading = false;
		}
	}

	// A freshly created event always belongs to the host's "Mine" list and, if
	// published, joins the upcoming feed too. Both lists stay sorted by start.
	onCreated(event: CommunityEvent): void {
		this.mine = sortByStart([event, ...this.mine.filter((e) => e.id !== event.id)]);
		if (event.status === 'PUBLISHED' || event.status === 'LIVE') {
			this.upcoming = sortByStart([event, ...this.upcoming.filter((e) => e.id !== event.id)]);
		}
	}

	// Replace an event in place wherever it appears (publish, cancel, RSVP).
	replace(event: CommunityEvent): void {
		this.mine = this.mine.map((e) => (e.id === event.id ? event : e));
		if (event.status === 'CANCELLED' || event.status === 'ENDED' || event.status === 'DRAFT') {
			this.upcoming = this.upcoming.filter((e) => e.id !== event.id);
		} else {
			const exists = this.upcoming.some((e) => e.id === event.id);
			this.upcoming = exists
				? this.upcoming.map((e) => (e.id === event.id ? event : e))
				: this.upcoming;
		}
	}
}

function sortByStart(events: CommunityEvent[]): CommunityEvent[] {
	return [...events].sort(
		(a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime()
	);
}

export const mobileEventsStore = new MobileEventsStore();
