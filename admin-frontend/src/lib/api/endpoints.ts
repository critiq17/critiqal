export const API = {
	admin: {
		login: '/api/admin/auth/login',
		login2fa: '/api/admin/auth/2fa',
		logout: '/api/admin/auth/logout',
		me: '/api/admin/me',
		usersSearch: (q: string, page: number, size: number) =>
			`/api/admin/users/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`,
		user: (id: string) => `/api/admin/users/${id}`,
		postsSearch: (q: string, page: number, size: number) =>
			`/api/admin/posts/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`,
		badges: '/api/admin/badges',
		grantBadge: (id: string) => `/api/admin/users/${id}/badges`,
		revokeBadge: (id: string, code: string) => `/api/admin/users/${id}/badges/${code}`,
	},
} as const;
