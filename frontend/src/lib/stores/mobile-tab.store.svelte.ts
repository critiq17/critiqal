export type MobileTab = 'feed' | 'explore' | 'profile';

class TabStore {
	active = $state<MobileTab>('feed');
}

export const tabStore = new TabStore();
