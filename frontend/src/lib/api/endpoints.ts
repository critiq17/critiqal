export const API = {
	auth: {
		register: '/api/auth/register',
		login: '/api/auth/login'
	},
	posts: {
		feed: '/api/posts',
		search: '/api/posts/search',
		byId: (id: number) => `/api/posts/${id}`,
		comments: (postId: number) => `/api/posts/${postId}/comments`,
		comment: (postId: number, commentId: number) => `/api/posts/${postId}/comments/${commentId}`,
		reactions: (postId: number) => `/api/posts/${postId}/reactions`
	},
	users: {
		profile: (username: string) => `/api/users/${encodeURIComponent(username)}`,
		search: '/api/users/search',
		me: '/api/users/me',
		posts: (username: string) => `/api/users/${encodeURIComponent(username)}/posts`,
		follow: (id: number) => `/api/users/${id}/follow`,
		followers: (id: number) => `/api/users/${id}/followers`,
		following: (id: number) => `/api/users/${id}/following`
	},
	media: {
		avatar: '/api/media/avatar',
		postPhoto: (postId: number) => `/api/media/posts/${postId}/photo`
	}
} as const;
