<script lang="ts">
	interface Props {
		password: string;
	}

	let { password }: Props = $props();

	type StrengthLevel = 0 | 1 | 2 | 3 | 4;

	interface LevelInfo {
		label: string;
		color: string;
	}

	const LEVELS: Record<Exclude<StrengthLevel, 0>, LevelInfo> = {
		1: { label: 'Weak', color: '#e05252' },
		2: { label: 'Fair', color: '#e07d30' },
		3: { label: 'Good', color: '#d4b84a' },
		4: { label: 'Strong', color: '#4aad7a' }
	};

	function computeScore(pwd: string): StrengthLevel {
		if (pwd.length === 0) return 0;
		let score = 0;
		if (pwd.length >= 8) score++;
		if (/[0-9]/.test(pwd)) score++;
		if (/[A-Z]/.test(pwd)) score++;
		if (/[^A-Za-z0-9]/.test(pwd)) score++;
		return score as StrengthLevel;
	}

	const score = $derived(computeScore(password));
	const levelInfo = $derived(score > 0 ? LEVELS[score as Exclude<StrengthLevel, 0>] : null);
</script>

{#if password.length > 0}
	<div class="strength-wrapper" aria-label="Password strength: {levelInfo?.label ?? ''}">
		<div class="segments" role="presentation">
			{#each [1, 2, 3, 4] as seg (seg)}
				<div
					class="segment"
					style="background-color: {score >= seg && levelInfo
						? levelInfo.color
						: 'var(--color-border)'}"
				></div>
			{/each}
		</div>
		{#if levelInfo}
			<span class="strength-label" style="color: {levelInfo.color}">{levelInfo.label}</span>
		{/if}
	</div>
{/if}

<style>
	.strength-wrapper {
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
		margin-top: 0.5rem;
	}

	.segments {
		display: flex;
		gap: 0.25rem;
	}

	.segment {
		flex: 1;
		height: 0.1875rem;
		border-radius: 9999px;
		transition:
			background-color 0.2s ease,
			opacity 0.2s ease;
	}

	.strength-label {
		font-size: 0.75rem;
		font-weight: 500;
		transition: color 0.2s ease;
	}
</style>
