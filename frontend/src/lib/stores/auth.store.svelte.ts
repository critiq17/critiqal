import type { User } from '$lib/types';
import { isTelegramMiniApp, cloudStorage } from '$lib/telegram';
import { apiClient, setInMemoryToken } from '$lib/api/client';

interface AuthState {
	user: User | null;
	isLoading: boolean;
}

function createAuthStore() {
	let state = $state<AuthState>({ user: null, isLoading: true });

	function getStoredUser(): User | null {
		if (typeof window === 'undefined') return null;
		const raw = localStorage.getItem('auth_user');
		if (!raw) return null;
		try {
			return JSON.parse(raw) as User;
		} catch {
			return null;
		}
	}

	async function init(): Promise<void> {
		let token: string | null = null;
		let userRaw: string | null = null;

		if (isTelegramMiniApp()) {
			token = await cloudStorage.get('auth_token');
			userRaw = await cloudStorage.get('auth_user');
		} else {
			if (typeof window !== 'undefined') {
				token = localStorage.getItem('auth_token');
				userRaw = localStorage.getItem('auth_user');
			}
		}

		if (token !== null) {
			setInMemoryToken(token);

			let user: User | null = null;

			if (userRaw !== null) {
				try {
					user = JSON.parse(userRaw) as User;
				} catch {
					user = null;
				}
			}

			if (user === null) {
				try {
					user = await apiClient.get<User>('/api/auth/me', true);
				} catch {
					user = null;
				}
			}

			state = { user, isLoading: false };
		} else {
			state = { user: null, isLoading: false };
		}
	}

	function setUser(user: User | null): void {
		state = { ...state, user };
		if (typeof window !== 'undefined') {
			if (user) {
				localStorage.setItem('auth_user', JSON.stringify(user));
			} else {
				localStorage.removeItem('auth_user');
				localStorage.removeItem('auth_token');
			}
		}
	}

	async function login(user: User, token: string): Promise<void> {
		setInMemoryToken(token);

		if (isTelegramMiniApp()) {
			await cloudStorage.set('auth_token', token);
			await cloudStorage.set('auth_user', JSON.stringify(user));
		} else {
			if (typeof window !== 'undefined') {
				localStorage.setItem('auth_token', token);
				localStorage.setItem('auth_user', JSON.stringify(user));
			}
		}

		// Mirror-write to localStorage so the sync getStoredToken path always works
		if (typeof window !== 'undefined') {
			localStorage.setItem('auth_token', token);
			localStorage.setItem('auth_user', JSON.stringify(user));
		}

		setUser(user);
	}

	async function logout(): Promise<void> {
		setInMemoryToken(null);

		if (isTelegramMiniApp()) {
			await cloudStorage.remove('auth_token');
			await cloudStorage.remove('auth_user');
		}

		// Always clear localStorage regardless of environment
		if (typeof window !== 'undefined') {
			localStorage.removeItem('auth_token');
			localStorage.removeItem('auth_user');
		}

		setUser(null);
	}

	function updateUser(user: User): void {
		setUser(user);
	}

	return {
		get user() {
			return state.user;
		},
		get isLoading() {
			return state.isLoading;
		},
		get isAuthenticated() {
			return state.user !== null;
		},
		init,
		login,
		logout,
		updateUser
	};
}

export const authStore = createAuthStore();
