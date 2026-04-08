import type { User } from '$lib/types';

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

	function init(): void {
		const storedUser = getStoredUser();
		state = { user: storedUser, isLoading: false };
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

	function login(user: User, token: string): void {
		if (typeof window !== 'undefined') {
			localStorage.setItem('auth_token', token);
		}
		setUser(user);
	}

	function logout(): void {
		setUser(null);
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
		logout
	};
}

export const authStore = createAuthStore();
