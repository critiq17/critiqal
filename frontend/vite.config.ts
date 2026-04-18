import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		proxy: {
			'/api': {
				target: 'http://localhost:8082',
				changeOrigin: true
			}
		},
		allowedHosts: true
	},
	test: {
		globals: true,
		environment: 'jsdom',
		include: ['src/**/*.test.ts'],
		coverage: {
			provider: 'v8',
			include: ['src/lib/**/*.ts'],
			// Exclude rune-based store files (processed by Svelte compiler, not plain TS)
			// and bare type declarations.
			exclude: ['src/lib/**/*.svelte.ts', 'src/lib/types/**'],
			// Per-file thresholds. Grow this list as more test files are added.
			thresholds: {
				'src/lib/telegram.ts': {
					lines: 80
				}
			}
		},
		alias: {
			$lib: '/home/critiq/development/critiqal/frontend/src/lib'
		}
	}
});
