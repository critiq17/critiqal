import { apiClient } from '$lib/api/client';

class ViewTracker {
  private readonly tracked = new Set<number>();
  private readonly timers = new Map<Element, ReturnType<typeof setTimeout>>();
  private observer: IntersectionObserver | null = null;
  private readonly entries = new Map<Element, { postId: number; isAuthenticated: boolean }>();

  private getObserver(): IntersectionObserver {
    if (!this.observer) {
      this.observer = new IntersectionObserver(
        (intersections) => {
          for (const entry of intersections) {
            const meta = this.entries.get(entry.target);
            if (!meta) continue;

            if (entry.isIntersecting) {
              this.scheduleTrack(entry.target, meta.postId, meta.isAuthenticated);
            } else {
              this.cancelTimer(entry.target);
            }
          }
        },
        { threshold: 0.6 }
      );
    }
    return this.observer;
  }

  private scheduleTrack(el: Element, postId: number, isAuthenticated: boolean): void {
    if (this.tracked.has(postId) || !isAuthenticated || this.timers.has(el)) return;

    const timer = setTimeout(() => {
      this.timers.delete(el);
      if (this.tracked.has(postId) || !isAuthenticated) return;
      this.tracked.add(postId);
      apiClient.get(`/api/posts/${postId}`).catch(() => {});
    }, 1500);

    this.timers.set(el, timer);
  }

  private cancelTimer(el: Element): void {
    const timer = this.timers.get(el);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.timers.delete(el);
    }
  }

  observe(el: HTMLElement, postId: number, isAuthenticated: boolean): () => void {
    if (!isAuthenticated) return () => {};

    this.entries.set(el, { postId, isAuthenticated });
    this.getObserver().observe(el);

    return () => {
      this.cancelTimer(el);
      this.getObserver().unobserve(el);
      this.entries.delete(el);
    };
  }
}

export const viewTracker = new ViewTracker();
