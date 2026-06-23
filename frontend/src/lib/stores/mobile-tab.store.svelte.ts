export type MobileTab = 'feed' | 'explore' | 'events' | 'profile';

class TabStore {
  active = $state<MobileTab>('feed');
}

export const tabStore = new TabStore();
