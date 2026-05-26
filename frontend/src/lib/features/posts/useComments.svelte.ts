import { postService } from '$lib/services';
import { authStore } from '$lib/stores/auth.store.svelte';
import { authGate } from '$lib/stores/auth-gate.store.svelte';
import type { Comment } from '$lib/types';

interface ReplyState {
  replies: Comment[];
  loaded: boolean;
  loading: boolean;
  expanded: boolean;
  composerOpen: boolean;
  draft: string;
  submitting: boolean;
}

function emptyReplyState(): ReplyState {
  return {
    replies: [],
    loaded: false,
    loading: false,
    expanded: false,
    composerOpen: false,
    draft: '',
    submitting: false,
  };
}

export class UseComments {
  comments = $state<Comment[]>([]);
  loaded = $state(false);
  expanded = $state(false);
  submitting = $state(false);
  newComment = $state('');
  deletingId = $state<string | null>(null);
  private replyStates = $state<Map<string, ReplyState>>(new Map());

  constructor(private postId: string) {}

  getReplyState(commentId: string): ReplyState {
    return this.replyStates.get(commentId) ?? emptyReplyState();
  }

  private setReplyState(commentId: string, patch: Partial<ReplyState>): void {
    const current = this.replyStates.get(commentId) ?? emptyReplyState();
    this.replyStates = new Map(this.replyStates).set(commentId, { ...current, ...patch });
  }

  async toggle(): Promise<void> {
    this.expanded = !this.expanded;
    if (this.expanded && !this.loaded) await this.load();
  }

  async load(): Promise<void> {
    if (this.loaded) return;
    try {
      this.comments = await postService.getComments(this.postId);
    } catch {
      // silent
    } finally {
      this.loaded = true;
    }
  }

  async submit(): Promise<void> {
    if (!authStore.isAuthenticated) {
      authGate.open('comment');
      return;
    }
    const text = this.newComment.trim();
    if (!text || this.submitting) return;
    this.submitting = true;
    try {
      const c = await postService.addComment(this.postId, { content: text });
      this.comments = [c, ...this.comments];
      this.newComment = '';
    } catch {
      // silent
    } finally {
      this.submitting = false;
    }
  }

  async deleteComment(commentId: string): Promise<void> {
    if (this.deletingId !== null) return;
    this.deletingId = commentId;
    try {
      await postService.deleteComment(this.postId, commentId);
      this.comments = this.comments.filter((c) => c.id !== commentId);
      const next = new Map(this.replyStates);
      next.delete(commentId);
      this.replyStates = next;
    } catch {
      // silent
    } finally {
      this.deletingId = null;
    }
  }

  async toggleReplies(commentId: string): Promise<void> {
    const rs = this.getReplyState(commentId);
    if (!rs.loaded) {
      this.setReplyState(commentId, { loading: true });
      try {
        const replies = await postService.getReplies(this.postId, commentId);
        this.setReplyState(commentId, { replies, loaded: true, loading: false, expanded: true });
      } catch {
        this.setReplyState(commentId, { loading: false, loaded: true, expanded: true });
      }
    } else {
      this.setReplyState(commentId, { expanded: !rs.expanded });
    }
  }

  openReplyComposer(commentId: string): void {
    this.setReplyState(commentId, { composerOpen: true });
  }

  closeReplyComposer(commentId: string): void {
    this.setReplyState(commentId, { composerOpen: false, draft: '' });
  }

  setReplyDraft(commentId: string, value: string): void {
    this.setReplyState(commentId, { draft: value });
  }

  async submitReply(commentId: string): Promise<void> {
    const rs = this.getReplyState(commentId);
    const text = rs.draft.trim();
    if (!text || rs.submitting) return;
    this.setReplyState(commentId, { submitting: true });
    try {
      const reply = await postService.addReply(this.postId, commentId, { content: text });
      const current = this.getReplyState(commentId);
      this.setReplyState(commentId, {
        replies: [...current.replies, reply],
        draft: '',
        composerOpen: false,
        submitting: false,
        loaded: true,
        expanded: true,
      });
    } catch {
      this.setReplyState(commentId, { submitting: false });
    }
  }

  async deleteReply(commentId: string, replyId: string): Promise<void> {
    try {
      await postService.deleteComment(this.postId, replyId);
      const rs = this.getReplyState(commentId);
      this.setReplyState(commentId, { replies: rs.replies.filter((r) => r.id !== replyId) });
    } catch {
      // silent
    }
  }
}
