export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'LIVE' | 'ENDED' | 'CANCELLED';
export type EventLocationType = 'DISCORD' | 'TELEGRAM' | 'IRL' | 'EXTERNAL';
export type RsvpStatus = 'GOING' | 'INTERESTED';

export interface EventHost {
  id: string;
  username: string;
  name: string | null;
  avatarUrl: string | null;
}

export interface CommunityEvent {
  id: string;
  host: EventHost;
  title: string;
  description: string | null;
  coverImageUrl: string | null;
  locationType: EventLocationType;
  locationValue: string | null;
  startsAt: string;
  endsAt: string | null;
  capacity: number | null;
  attendeeCount: number;
  status: EventStatus;
  discordEventId: string | null;
  discordEventUrl: string | null;
  createdAt: string;
  viewerRsvp: RsvpStatus | null;
  canManage: boolean;
}

export interface EventAttendee {
  userId: string;
  username: string;
  name: string | null;
  avatarUrl: string | null;
  status: RsvpStatus;
}

export interface CreateEventRequest {
  title: string;
  description?: string;
  coverImageUrl?: string;
  locationType: EventLocationType;
  locationValue?: string;
  startsAt: string;
  endsAt?: string;
  capacity?: number;
  publishNow?: boolean;
}
