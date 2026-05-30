import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';
import { svelteTesting } from '@testing-library/svelte/vite';

export default defineConfig({
	plugins: [sveltekit(), svelteTesting()],
	server: {
		port: 3002,
		proxy: {
			'/api': {
				target: 'http://127.0.0.1:8082',
				changeOrigin: true,
			},
		},
		allowedHosts: true,
	},
	test: {
		globals: true,
		environment: 'jsdom',
		include: ['src/**/*.test.ts'],
		setupFiles: ['src/test-setup.ts'],
		coverage: {
			provider: 'v8',
			include: ['src/lib/**/*.ts'],
			exclude: ['src/lib/**/*.svelte.ts', 'src/lib/types/**'],
		},
		alias: {
			$lib: new URL('./src/lib', import.meta.url).pathname,
		},
	},
});
