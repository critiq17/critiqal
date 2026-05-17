import type { StravaConnection, StravaActivity } from '$lib/types';
import { stravaService } from '$lib/services/strava.service';

interface StravaState {
	connection: StravaConnection | null;
	activities: StravaActivity[];
	loading: boolean;
	error: string | null;
}

function createStravaStore() {
	let state = $state<StravaState>({
		connection: null,
		activities: [],
		loading: false,
		error: null
	});

	function setError(error: unknown): void {
		state = {
			...state,
			loading: false,
			error: error instanceof Error ? error.message : 'An unexpected error occurred'
		};
	}

	async function load(): Promise<void> {
		state = { ...state, loading: true, error: null };

		try {
			const connection = await stravaService.getConnection();
			const activities = connection != null
				? await stravaService.getActivities()
				: [];

			state = { connection: connection ?? null, activities, loading: false, error: null };
		} catch (error) {
			setError(error);
		}
	}

	async function connect(): Promise<void> {
		state = { ...state, loading: true, error: null };

		try {
			const { url } = await stravaService.getConnectUrl();
			window.location.href = url;
		} catch (error) {
			setError(error);
		}
	}

	async function disconnect(): Promise<void> {
		state = { ...state, loading: true, error: null };

		try {
			await stravaService.disconnect();
			state = { connection: null, activities: [], loading: false, error: null };
		} catch (error) {
			setError(error);
		}
	}

	async function loadPublic(userId: string): Promise<void> {
		state = { ...state, loading: true, error: null };

		try {
			const connection = await stravaService.getPublicConnection(userId);
			state = {
				...state,
				connection: connection ?? null,
				activities: [],
				loading: false,
				error: null
			};
		} catch (error) {
			setError(error);
		}
	}

	return {
		get connection() {
			return state.connection;
		},
		get activities() {
			return state.activities;
		},
		get loading() {
			return state.loading;
		},
		get error() {
			return state.error;
		},
		load,
		connect,
		disconnect,
		loadPublic
	};
}

export const stravaStore = createStravaStore();
