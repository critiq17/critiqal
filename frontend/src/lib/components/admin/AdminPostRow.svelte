<script lang="ts">
  import type { Post } from '$lib/types';
  import Avatar from '$lib/ui/Avatar.svelte';

  interface Props {
    post: Post;
  }

  let { post }: Props = $props();

  const author = $derived(post.author);

  const formattedDate = $derived(
    new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(post.createdAt)
    )
  );

  const excerpt = $derived(
    post.content.length > 200 ? `${post.content.slice(0, 200)}…` : post.content
  );
</script>

<article class="row">
  <Avatar src={author.avatarUrl} name={author.name ?? author.username} size={40} />
  <div class="body">
    <div class="head">
      <span class="name">{author.name ?? author.username}</span>
      <span class="username">@{author.username}</span>
      <span class="dot" aria-hidden="true">·</span>
      <time datetime={post.createdAt}>{formattedDate}</time>
    </div>
    <p class="content">{excerpt}</p>
  </div>
</article>

<style>
  .row {
    display: flex;
    gap: 0.75rem;
    padding: 0.875rem 0.75rem;
    border-bottom: 1px solid var(--color-border);
  }
  .body {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    min-width: 0;
    flex: 1;
  }
  .head {
    display: flex;
    align-items: baseline;
    gap: 0.375rem;
    flex-wrap: wrap;
    font-size: 0.8125rem;
  }
  .name {
    color: var(--color-text-primary);
    font-weight: 600;
  }
  .username,
  .dot,
  time {
    color: var(--color-text-secondary);
  }
  .content {
    margin: 0;
    color: var(--color-text-primary);
    font-size: 0.9375rem;
    line-height: 1.45;
    white-space: pre-wrap;
    word-break: break-word;
  }
</style>
