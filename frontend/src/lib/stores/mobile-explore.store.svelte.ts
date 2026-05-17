export type ExploreTab = 'posts' | 'people';

class ExploreStore {
	query = $state('');
	tab = $state<ExploreTab>('posts');
}

export const mobileExploreStore = new ExploreStore();
