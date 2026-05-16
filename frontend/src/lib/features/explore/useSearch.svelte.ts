import { postService, userService } from '$lib/services';
import type { Post, User } from '$lib/types';

export type ExploreTab = 'posts' | 'people';

const DEBOUNCE_MS = 300;

export class UseSearch {
  query = $state('');
  tab = $state<ExploreTab>('posts');

  posts = $state<Post[]>([]);
  postsPage = $state(0);
  postsHasNext = $state(false);
  postsLoading = $state(false);
  postsLoadingMore = $state(false);
  postsError = $state<string | null>(null);

  users = $state<User[]>([]);
  usersState = $state<'idle' | 'loading' | 'loaded' | 'error'>('idle');
  usersError = $state('');

  followStates = $state(new Map<string, boolean>());

  private loadedQuery: string | null = null;
  private loadedTab: ExploreTab | null = null;
  private debounceTimer: ReturnType<typeof setTimeout> | undefined;

  async loadPosts(q: string): Promise<void> {
    this.postsLoading = true;
    this.postsError = null;
    this.posts = [];
    this.postsPage = 0;
    this.postsHasNext = false;
    try {
      const res = q.trim() ? await postService.search(q.trim(), 0) : await postService.getFeed(0);
      this.posts = res.content;
      this.postsHasNext = res.hasNext;
    } catch (err) {
      this.postsError = err instanceof Error ? err.message : 'Something went wrong.';
    } finally {
      this.postsLoading = false;
    }
  }

  removePost(id: string): void {
    this.posts = this.posts.filter((p) => p.id !== id);
  }

  async loadMorePosts(): Promise<void> {
    if (!this.postsHasNext || this.postsLoadingMore) return;
    this.postsLoadingMore = true;
    const q = this.query;
    try {
      const nextPage = this.postsPage + 1;
      const res = q.trim()
        ? await postService.search(q.trim(), nextPage)
        : await postService.getFeed(nextPage);
      this.posts = [...this.posts, ...res.content];
      this.postsPage = nextPage;
      this.postsHasNext = res.hasNext;
    } catch {
      // non-fatal
    } finally {
      this.postsLoadingMore = false;
    }
  }

  async fetchResults(q: string, tab: ExploreTab): Promise<void> {
    if (this.loadedQuery === q && this.loadedTab === tab) return;

    if (tab === 'posts') {
      await this.loadPosts(q);
      if (!this.postsError) {
        this.loadedQuery = q;
        this.loadedTab = tab;
      }
    } else {
      this.usersState = 'loading';
      this.usersError = '';
      try {
        this.users = await userService.search(q.trim());
        this.loadedQuery = q;
        this.loadedTab = tab;
        this.usersState = 'loaded';
      } catch (err: unknown) {
        this.usersError = err instanceof Error ? err.message : 'Something went wrong.';
        this.usersState = 'error';
      }
    }
  }

  scheduleSearch(q: string, tab: ExploreTab, immediate = false): void {
    clearTimeout(this.debounceTimer);
    if (immediate) {
      this.fetchResults(q, tab);
    } else {
      this.debounceTimer = setTimeout(() => this.fetchResults(q, tab), DEBOUNCE_MS);
    }
  }

  async toggleFollow(user: User): Promise<void> {
    const isFollowing = this.followStates.get(user.id) ?? false;
    this.followStates = new Map(this.followStates).set(user.id, !isFollowing);
    try {
      if (isFollowing) {
        await userService.unfollow(user.id);
      } else {
        await userService.follow(user.id);
      }
    } catch {
      this.followStates = new Map(this.followStates).set(user.id, isFollowing);
    }
  }
}
