import { writable } from 'svelte/store';

export type MobileTab = 'feed' | 'explore' | 'profile';

export const activeTab = writable<MobileTab>('feed');
