function createReducedMotion() {
	let reduced = $state(false);

	$effect.root(() => {
		const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
		reduced = mq.matches;
		const handler = (e: MediaQueryListEvent) => {
			reduced = e.matches;
		};
		mq.addEventListener('change', handler);
		return () => mq.removeEventListener('change', handler);
	});

	return { get value() { return reduced; } };
}

export const reducedMotion = createReducedMotion();
