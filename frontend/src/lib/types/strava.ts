export interface StravaConnection {
	athleteId: number;
	username: string;
	firstname: string;
	lastname: string;
	city: string;
	avatarUrl: string;
	connectedAt: string;
}

export interface StravaActivity {
	id: number;
	name: string;
	type: string;
	distanceKm: number;
	movingTimeSeconds: number;
	elevationGain: number;
	startDate: string;
	avgHeartRate: number;
	avgPaceMinPerKm: number;
}
