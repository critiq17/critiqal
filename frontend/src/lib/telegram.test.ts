import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { isTelegramMiniApp, getTelegramWebApp, cloudStorage, initTelegram } from './telegram';

function makeMockWebApp(overrides: {
	backgroundColor?: string;
	themeParams?: { text_color: string; hint_color: string; button_color: string; button_text_color: string };
} = {}) {
	return {
		ready: vi.fn(),
		expand: vi.fn(),
		requestFullscreen: vi.fn(),
		setHeaderColor: vi.fn(),
		onEvent: vi.fn(),
		offEvent: vi.fn(),
		colorScheme: 'light' as const,
		isFullscreen: false,
		isExpanded: true,
		viewportHeight: 800,
		safeAreaInset: { top: 0, bottom: 0, left: 0, right: 0 },
		contentSafeAreaInset: { top: 0, bottom: 0, left: 0, right: 0 },
		backgroundColor: overrides.backgroundColor ?? '#ffffff',
		themeParams: overrides.themeParams ?? {
			text_color: '#000000',
			hint_color: '#999999',
			button_color: '#3390ec',
			button_text_color: '#ffffff'
		},
		BackButton: { show: vi.fn(), hide: vi.fn(), onClick: vi.fn(), offClick: vi.fn() },
		HapticFeedback: { impactOccurred: vi.fn(), notificationOccurred: vi.fn() },
		CloudStorage: {
			getItem: vi.fn(),
			setItem: vi.fn(),
			removeItem: vi.fn()
		}
	};
}

afterEach(() => {
	vi.unstubAllGlobals();
	vi.restoreAllMocks();
});

describe('isTelegramMiniApp', () => {
	it('returns false when window.Telegram is undefined', () => {
		vi.stubGlobal('Telegram', undefined);
		expect(isTelegramMiniApp()).toBe(false);
	});

	it('returns true when window.Telegram.WebApp is a non-null object', () => {
		vi.stubGlobal('Telegram', { WebApp: makeMockWebApp() });
		expect(isTelegramMiniApp()).toBe(true);
	});
});

describe('getTelegramWebApp', () => {
	it('returns null when not in Telegram', () => {
		vi.stubGlobal('Telegram', undefined);
		expect(getTelegramWebApp()).toBeNull();
	});

	it('returns the WebApp object when window.Telegram.WebApp exists', () => {
		const webApp = makeMockWebApp();
		vi.stubGlobal('Telegram', { WebApp: webApp });
		expect(getTelegramWebApp()).toBe(webApp);
	});
});

describe('cloudStorage — localStorage fallback (not in Telegram)', () => {
	beforeEach(() => {
		vi.stubGlobal('Telegram', undefined);
		localStorage.clear();
	});

	it('get: resolves to the stored value via localStorage', async () => {
		localStorage.setItem('my_key', 'my_value');
		const result = await cloudStorage.get('my_key');
		expect(result).toBe('my_value');
	});

	it('get: resolves to null when key is absent in localStorage', async () => {
		const result = await cloudStorage.get('missing_key');
		expect(result).toBeNull();
	});

	it('set: persists value to localStorage', async () => {
		await cloudStorage.set('my_key', 'my_value');
		expect(localStorage.getItem('my_key')).toBe('my_value');
	});

	it('remove: deletes key from localStorage', async () => {
		localStorage.setItem('my_key', 'to_be_removed');
		await cloudStorage.remove('my_key');
		expect(localStorage.getItem('my_key')).toBeNull();
	});

	it('get: calls localStorage.getItem with the correct key', async () => {
		const spy = vi.spyOn(Storage.prototype, 'getItem');
		await cloudStorage.get('spy_key');
		expect(spy).toHaveBeenCalledWith('spy_key');
	});

	it('set: calls localStorage.setItem with the correct key and value', async () => {
		const spy = vi.spyOn(Storage.prototype, 'setItem');
		await cloudStorage.set('spy_key', 'spy_val');
		expect(spy).toHaveBeenCalledWith('spy_key', 'spy_val');
	});

	it('remove: calls localStorage.removeItem with the correct key', async () => {
		const spy = vi.spyOn(Storage.prototype, 'removeItem');
		await cloudStorage.remove('spy_key');
		expect(spy).toHaveBeenCalledWith('spy_key');
	});
});

describe('cloudStorage — Telegram CloudStorage paths', () => {
	function makeWebAppWithCloudStorage() {
		const webApp = makeMockWebApp();
		vi.stubGlobal('Telegram', { WebApp: webApp });
		return webApp;
	}

	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it('get: resolves value from tg.CloudStorage.getItem on success', async () => {
		const webApp = makeWebAppWithCloudStorage();
		webApp.CloudStorage.getItem.mockImplementation(
			(_key: string, cb: (err: unknown, value: string) => void) => cb(null, 'cloud_val')
		);

		const result = await cloudStorage.get('any_key');
		expect(result).toBe('cloud_val');
		expect(webApp.CloudStorage.getItem).toHaveBeenCalledWith('any_key', expect.any(Function));
	});

	it('get: resolves null when tg.CloudStorage.getItem returns an error', async () => {
		const webApp = makeWebAppWithCloudStorage();
		webApp.CloudStorage.getItem.mockImplementation(
			(_key: string, cb: (err: unknown, value: string) => void) =>
				cb(new Error('cloud error'), '')
		);

		const result = await cloudStorage.get('any_key');
		expect(result).toBeNull();
	});

	it('set: resolves when tg.CloudStorage.setItem succeeds', async () => {
		const webApp = makeWebAppWithCloudStorage();
		webApp.CloudStorage.setItem.mockImplementation(
			(_key: string, _value: string, cb: (err: unknown, stored: boolean) => void) =>
				cb(null, true)
		);

		await expect(cloudStorage.set('k', 'v')).resolves.toBeUndefined();
		expect(webApp.CloudStorage.setItem).toHaveBeenCalledWith('k', 'v', expect.any(Function));
	});

	it('set: rejects when tg.CloudStorage.setItem reports failure', async () => {
		const webApp = makeWebAppWithCloudStorage();
		webApp.CloudStorage.setItem.mockImplementation(
			(_key: string, _value: string, cb: (err: unknown, stored: boolean) => void) =>
				cb(null, false)
		);

		await expect(cloudStorage.set('k', 'v')).rejects.toThrow('Failed to store key "k"');
	});

	it('remove: resolves when tg.CloudStorage.removeItem succeeds', async () => {
		const webApp = makeWebAppWithCloudStorage();
		webApp.CloudStorage.removeItem.mockImplementation(
			(_key: string, cb: (err: unknown, removed: boolean) => void) => cb(null, true)
		);

		await expect(cloudStorage.remove('k')).resolves.toBeUndefined();
		expect(webApp.CloudStorage.removeItem).toHaveBeenCalledWith('k', expect.any(Function));
	});

	it('remove: rejects when tg.CloudStorage.removeItem reports failure', async () => {
		const webApp = makeWebAppWithCloudStorage();
		webApp.CloudStorage.removeItem.mockImplementation(
			(_key: string, cb: (err: unknown, removed: boolean) => void) => cb(null, false)
		);

		await expect(cloudStorage.remove('k')).rejects.toThrow('Failed to remove key "k"');
	});
});

describe('initTelegram', () => {
	it('sets --tg-bg CSS custom property when in Telegram', () => {
		const webApp = makeMockWebApp({ backgroundColor: '#1a1a2e' });
		vi.stubGlobal('Telegram', { WebApp: webApp });

		const setPropertySpy = vi.spyOn(document.documentElement.style, 'setProperty');

		initTelegram();

		expect(setPropertySpy).toHaveBeenCalledWith('--tg-bg', '#1a1a2e');
	});

	it('sets all theme CSS custom properties', () => {
		const webApp = makeMockWebApp({
			backgroundColor: '#0d0d0d',
			themeParams: {
				text_color: '#ffffff',
				hint_color: '#aaaaaa',
				button_color: '#ff6600',
				button_text_color: '#000000'
			}
		});
		vi.stubGlobal('Telegram', { WebApp: webApp });

		const setPropertySpy = vi.spyOn(document.documentElement.style, 'setProperty');

		initTelegram();

		expect(setPropertySpy).toHaveBeenCalledWith('--tg-text', '#ffffff');
		expect(setPropertySpy).toHaveBeenCalledWith('--tg-hint', '#aaaaaa');
		expect(setPropertySpy).toHaveBeenCalledWith('--tg-accent', '#ff6600');
		expect(setPropertySpy).toHaveBeenCalledWith('--tg-btn-text', '#000000');
	});

	it('does nothing when not in Telegram', () => {
		vi.stubGlobal('Telegram', undefined);

		// Fresh spy — no prior calls in this test
		const setPropertySpy = vi.spyOn(document.documentElement.style, 'setProperty');

		initTelegram();

		expect(setPropertySpy).not.toHaveBeenCalled();
	});
});
