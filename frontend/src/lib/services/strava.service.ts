import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { StravaConnection, StravaActivity } from '$lib/types';

interface StravaConnectResponse {
	url: string;
}

export const stravaService = {
	getConnectUrl(): Promise<StravaConnectResponse> {
		return apiClient.get<StravaConnectResponse>(API.strava.connect, true);
	},

	getConnection(): Promise<StravaConnection | null> {
		return apiClient.get<StravaConnection | null>(API.strava.connection, true);
	},

	disconnect(): Promise<void> {
		return apiClient.delete<void>(API.strava.connection, true);
	},

	getActivities(limit = 5): Promise<StravaActivity[]> {
		return apiClient.get<StravaActivity[]>(
			`${API.strava.activities}?limit=${limit}`,
			true
		);
	},

	getPublicConnection(userId: number): Promise<StravaConnection | null> {
		return apiClient.get<StravaConnection | null>(API.strava.public(userId));
	}
};
