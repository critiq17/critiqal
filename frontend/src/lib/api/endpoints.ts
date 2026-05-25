export const API = {
  auth: {
    register: '/api/auth/register',
    login: '/api/auth/login',
    login2fa: '/api/auth/login/2fa',
    logout: '/api/auth/logout',
    me: '/api/auth/me',
    setEmail: '/api/auth/email',
    verifyEmail: '/api/auth/email/verify',
    recovery: {
      request: '/api/auth/recovery/request',
      reset: '/api/auth/recovery/reset',
      useCode: '/api/auth/recovery/code',
      codesCount: '/api/auth/recovery/codes/count',
      regenerateCodes: '/api/auth/recovery/codes/regenerate',
    },
    twoFactor: {
      setup: '/api/auth/2fa/setup',
      confirm: '/api/auth/2fa/confirm',
      disable: '/api/auth/2fa',
      status: '/api/auth/2fa/status',
    },
    sessions: {
      list: '/api/auth/sessions',
      revoke: (id: string) => `/api/auth/sessions/${id}`,
    },
  },
  posts: {
    feed: '/api/posts',
    search: '/api/posts/search',
    byId: (id: string) => `/api/posts/${id}`,
    comments: (postId: string) => `/api/posts/${postId}/comments`,
    comment: (postId: string, commentId: string) => `/api/posts/${postId}/comments/${commentId}`,
    replies: (postId: string, commentId: string) =>
      `/api/posts/${postId}/comments/${commentId}/replies`,
    likes: (postId: string) => `/api/posts/${postId}/likes`,
    commentLikes: (postId: string, commentId: string) =>
      `/api/posts/${postId}/comments/${commentId}/likes`,
  },
  users: {
    profile: (username: string) => `/api/users/${encodeURIComponent(username)}`,
    search: '/api/users/search',
    me: '/api/users/me',
    posts: (username: string) => `/api/users/${encodeURIComponent(username)}/posts`,
    follow: (id: string) => `/api/users/${id}/follow`,
    followers: (id: string) => `/api/users/${id}/followers`,
    following: (id: string) => `/api/users/${id}/following`,
    stats: (id: string) => `/api/users/${id}/stats`,
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
