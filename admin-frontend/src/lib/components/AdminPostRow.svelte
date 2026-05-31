<script lang="ts">
	import type { Post } from '$lib/types';

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

	const initial = $derived((author.name ?? author.username).charAt(0).toUpperCase());
</script>

<article class="row">
	<div class="avatar" aria-hidden="true">
		{#if author.avatarUrl}
			<img src={author.avatarUrl} alt="" />
		{:else}
			<span class="initial">{initial}</span>
		{/if}
	</div>
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
	.avatar {
		width: 40px;
		height: 40px;
		border-radius: var(--radius-full);
		background: var(--color-surface-raised);
		overflow: hidden;
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.avatar img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}
	.initial {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-secondary);
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
