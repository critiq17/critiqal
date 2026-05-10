export const API = {
  auth: {
    register: '/api/auth/register',
    login: '/api/auth/login',
    login2fa: '/api/auth/login/2fa',
  },
  posts: {
    feed: '/api/posts',
    search: '/api/posts/search',
    byId: (id: string) => `/api/posts/${id}`,
    comments: (postId: string) => `/api/posts/${postId}/comments`,
    comment: (postId: string, commentId: string) => `/api/posts/${postId}/comments/${commentId}`,
    replies: (postId: string, commentId: string) =>
      `/api/posts/${postId}/comments/${commentId}/replies`,
    reactions: (postId: string) => `/api/posts/${postId}/reactions`,
  },
  users: {
    profile: (username: string) => `/api/users/${encodeURIComponent(username)}`,
    search: '/api/users/search',
    me: '/api/users/me',
    posts: (username: string) => `/api/users/${encodeURIComponent(username)}/posts`,
    follow: (id: string) => `/api/users/${id}/follow`,
    followers: (id: string) => `/api/users/${id}/followers`,
    following: (id: string) => `/api/users/${id}/following`,
    followingFeed: '/api/users/notifications/posts',
  },
  media: {
    avatar: '/api/media/avatar',
    postPhotos: (postId: string) => `/api/media/posts/${postId}/photos`,
    postPhoto: (postId: string, photoId: string) => `/api/media/posts/${postId}/photos/${photoId}`,
  },
  strava: {
    connect: '/api/integrations/strava/connect',
    connection: '/api/integrations/strava',
    activities: '/api/integrations/strava/activities',
    public: (userId: string) => `/api/integrations/strava/public/${userId}`,
  },
} as const;
