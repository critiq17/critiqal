<script lang="ts">
	interface Props {
		reason: string;
		expiresAt: string; // ISO string or "" for permanent
	}

	let { reason, expiresAt }: Props = $props();

	const permanent = $derived(!expiresAt || expiresAt.trim() === '');

	const untilDate = $derived(() => {
		if (permanent) return null;
		try {
			return new Date(expiresAt).toLocaleDateString(undefined, {
				year: 'numeric',
				month: 'long',
				day: 'numeric',
			});
		} catch {
			return null;
		}
	});
</script>

<div class="ban-card" role="alert" aria-live="assertive">
	<div class="icon-wrap" aria-hidden="true">
		<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
			<circle cx="12" cy="12" r="10" />
			<line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
		</svg>
	</div>

	<div class="text-block">
		<p class="title">Account suspended</p>
		<p class="sub">
			{#if permanent}
				This account has been permanently suspended.
			{:else}
				This account is suspended until <strong>{untilDate()}</strong>.
			{/if}
		</p>
	</div>

	<div class="reason-block">
		<p class="reason-label">Reason</p>
		<p class="reason-text">{reason}</p>
	</div>

	<p class="footer">
		If you believe this is a mistake, contact support.
	</p>
</div>

<style>
	.ban-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1.1rem;
		text-align: center;
		padding: 0.25rem 0;
	}

	.icon-wrap {
		width: 3.25rem;
		height: 3.25rem;
		border-radius: 50%;
		background: color-mix(in srgb, #c92a2a 12%, var(--color-bg, #0c0c0c));
		border: 1px solid color-mix(in srgb, #c92a2a 28%, transparent);
		display: flex;
		align-items: center;
		justify-content: center;
		color: #c92a2a;
		flex-shrink: 0;
	}

	.icon-wrap svg {
		width: 1.5rem;
		height: 1.5rem;
	}

	.text-block {
		display: flex;
		flex-direction: column;
		gap: 0.35rem;
	}

	.title {
		margin: 0;
		font-size: 1.0625rem;
		font-weight: 700;
		letter-spacing: -0.02em;
		color: var(--color-text-primary, #eaeaea);
	}

	.sub {
		margin: 0;
		font-size: 0.875rem;
		color: var(--text-tertiary, rgba(255,255,255,0.5));
		line-height: 1.45;
	}

	.sub strong {
		color: var(--color-text-primary, #eaeaea);
		font-weight: 600;
	}

	.reason-block {
		width: 100%;
		background: color-mix(in srgb, #c92a2a 7%, var(--color-surface, #141414));
		border: 1px solid color-mix(in srgb, #c92a2a 22%, transparent);
		border-radius: 0.75rem;
		padding: 0.75rem 1rem;
		text-align: left;
	}

	.reason-label {
		margin: 0 0 0.25rem;
		font-size: 0.7rem;
		font-weight: 700;
		text-transform: uppercase;
		letter-spacing: 0.07em;
		color: #c92a2a;
	}

	.reason-text {
		margin: 0;
		font-size: 0.875rem;
		color: var(--color-text-primary, #eaeaea);
		line-height: 1.45;
		word-break: break-word;
	}

	.footer {
		margin: 0;
		font-size: 0.78rem;
		color: var(--text-quaternary, rgba(255,255,255,0.35));
		line-height: 1.4;
	}
</style>
