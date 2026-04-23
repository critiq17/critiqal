● Контекста достаточно. Ниже — подробное code review и план рефакторинга для critiqal-svelte. Пишу с позиции Senior Svelte Dev, Svelte 5 + SvelteKit 2, с фокусом на мгновенную навигацию, DDD-чистоту и            
  UX-полировку 2026.                                                                                                                                                                                              
                                                                                                                                                                                                                    
  ---
  1. Code Review — Critiqal Frontend                                                                                                                                                                                
                                                                                                                                                                                                                    
  1.1. Архитектурные проблемы (CRITICAL)
                                                                                                                                                                                                                    
  A. Дублирование компонента Post — два почти одинаковых файла                                                                                                                                                      
                                                                                                                                                                                                                    
  - src/lib/components/PostCard.svelte — 1 305 строк, импорт-имя PostCard                                                                                                                                           
  - src/lib/components/Post/Post.svelte — 1 481 строка, экспорт Post + типы PostVariant/PostSize                                                                                                                  
  - Обе реализуют: reactions, comments, replies, view-tracking, options menu, delete — одну и ту же логику, отличаясь только тем, что «вторая чуть лучше» (добавлены variant, size, onEdited, onAuthorClick,        
  константы из $lib/reactions).                                                                                                                                                                                     
  - Текущая ветка /routes/+page.svelte использует PostCard, а в Post/index.ts экспортируется Post — живёт две версии одновременно.                                                                                  
  - Плата: двойное поддержание багов (например, оптимистик-ап в PostCard отличается от Post порядком try/catch — есть race condition где rollback промахнётся).                                                     
                                                                                                                                                                                                                    
  B. God-файлы (KISS + SRP нарушены)                                                                                                                                                                                
                                                                                                                                                                                                                    
  ┌────────────────────────────────┬───────┬─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐                          
  │              Файл              │  LOC  │                                                                  Проблема                                                                   │
  ├────────────────────────────────┼───────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤                          
  │ MobileProfile.svelte           │ 2 252 │ Совмещает 6 sheet'ов, drag-to-dismiss × 3, Strava, avatar upload, edit mode, post feed, reactions, sticky header, followers/following lists │                        
  ├────────────────────────────────┼───────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ routes/[username]/+page.svelte │ 1 523 │ Дубликат MobileProfile для desktop — своя загрузка, свой рендер постов, свой модалка follow                                                 │                          
  ├────────────────────────────────┼───────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤                          
  │ MobileFeed.svelte              │ 819   │ Feed + pull-to-refresh + reactions + photo carousel + view-tracking + открытие comment sheet                                                │                          
  ├────────────────────────────────┼───────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤                          
  │ MobileExplore.svelte           │ 945   │ Двухрежимный поиск (posts/users) + debounce + пагинация + рендер                                                                            │                        
  ├────────────────────────────────┼───────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤                          
  │ MobilePostComposer.svelte      │ 640   │ Composer + photo preview + upload queue                                                                                                     │                        
  ├────────────────────────────────┼───────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤                          
  │ CommentSheet.svelte            │ 644   │ Лист + drag + reply-state-maps × 4                                                                                                          │                        
  └────────────────────────────────┴───────┴─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘                          
                                                                                                                                                                                                                  
  Общий антипаттерн: вся бизнес-логика живёт в компоненте. Сервис дергается из onMount, состояние хранится в локальных $state, оптимистичные апдейты, retry, кэш — всё там же.                                      
                                                                                                                                                                                                                  
  C. Нет +page.ts / +page.server.ts / +layout.ts — главная причина чёрных экранов                                                                                                                                   
                                                                                                                                                                                                                  
  $ find routes -name "+page.ts" -o -name "+page.server.ts" -o -name "+layout.ts"                                                                                                                                   
  # пусто                                                                                                                                                                                                           
  Это значит:
  - SvelteKit не делает SSR данных → первый рендер = пустая оболочка → onMount → fetch → 200-800мс пустоты → появление контента.                                                                                    
  - data-sveltekit-preload-data="hover" не работает (нет load) → клик по ссылке всегда блокирует UI.                                                                                                              
  - Ctrl+Click / refresh → видно «чёрный layout loader» из +layout.svelte (<div style="height:100vh;background:#0a0a0a"></div>) до mounted = true — это и есть тот самый чёрный экран, который пользователь видит.  
                                                                                                                                                                                                                    
  D. Нет +error.svelte, +loading.svelte, hooks.client.ts, hooks.server.ts                                                                                                                                           
                                                                                                                                                                                                                    
  - Любая ошибка 500/404 → дефолтный белый экран SvelteKit.                                                                                                                                                         
  - Нет централизованного перехватчика 401 (надо разлогинить + редирект на /login).                                                                                                                                 
                                                                                                                                                                                                                    
  E. Хаос стейт-менеджмента                                                                                                                                                                                       
                                                                                                                                                                                                                    
  В src/lib/stores/ одновременно:                                                                                                                                                                                   
  - .svelte.ts на рунах: auth.store.svelte.ts, strava.store.svelte.ts
  - Старые writable из svelte/store: mobile-feed.store.ts, mobile-explore.store.ts, mobile-tab.store.ts, profile-nav.store.ts, sheet.store.ts, compose.store.ts                                                     
                                                                                                                                                                                                                  
  Два разных mental model в одном приложении. В MobileFeed.svelte вы не подписываетесь на store, а дублируете его поля в локальный $state (posts, isLoading, hasNext…) и синхронизируете вручную — этого кода не    
  видно в прочитанных 180 строках, но он есть (и это классический источник rogue state).                                                                                                                            
                                                                                                                                                                                                                    
  F. Нет «query cache» — каждая навигация = заново fetch                                                                                                                                                            
                                                                                                                                                                                                                  
  Открыл профиль, вернулся в feed → feed перезагружается (потому что в +page.svelte posts = [] в loadFeed()). В mobile есть примитивный TTL-кэш в mobileFeedStore (30 с), но в /routes/+page.svelte его нет. Для    
  explore / profile / [username] — тоже нет. Нужен единый query-слой (TanStack Query style или свой тонкий invalidation-layer поверх рун).                                                                        
                                                                                                                                                                                                                    
  G. Нарушение слоёв                                                                                                                                                                                                
   
  - viewTracker.ts вызывает apiClient.get('/api/posts/${postId}', true) мимо сервиса. Эндпоинт при этом не «track view», это обычный getById — значит прикручено через GET-сайд-эффект. Ужас.                       
  - auth.store хранит токен в localStorage, но в CLAUDE.md заявлено «HttpOnly cookie sessions (no JWT in cookies)». Либо backend ещё не на cookie-сессиях, либо фронт не синхронизирован. XSS-риск: любой скрипт в
  DOM получит токен. И мёртвый код: в login() строки 80-94 — сначала cloudStorage.set, потом всё равно localStorage.setItem «mirror-write» — костыль.                                                               
                                                                                                                                                                                                                  
  ---                                                                                                                                                                                                               
  1.2. UI/UX проблемы                                                                                                                                                                                             
                     
  1. Чёрный заглушечный <div style="height:100vh;background:#0a0a0a"> в +layout.svelte до mounted — именно это пользователь видит как «загрузку без контента». Fix: SSR-рендер + display:contents вместо чёрного
  блока, а детект мобилы/десктопа через SSR user-agent, а не client-only.                                                                                                                                           
  2. В MobileLayout.svelte сразу монтируются все три таба (MobileFeed + MobileExplore + MobileProfile) — это 3 полноценных onMount-фетча на старте. На slow 3G — 4+ секунды до TTI.
  3. MobilePostComposer lazy-loaded (правильно), но через дикую конструкцию composeOpen.subscribe на топ-уровне модуля — утечка подписки (нет onDestroy-отписки в SSR-friendly виде).                               
  4. formatRelativeTime и getInitials переопределены локально минимум в 5 файлах (MobileFeed, MobileProfile, CommentSheet, UserProfileOverlay, [username]/+page.svelte) — хотя в $lib/utils/formatRelativeTime.ts   
  утилита уже есть. DRY нарушен.                                                                                                                                                                                    
  5. Анимации: slide/fade из svelte/transition без reduced-motion-guard'а; в MobileLayout есть ручной cubic-bezier transform на translateX — фрагментарно.                                                          
  6. Reactions: отдельные <img> с fallbackEmoji и assetPath — нет preload, нет sprite, при первом лайке иконка «мигает» (flash of unloaded content). Для 2026-minimal нужен SVG-sprite, preload через <link         
  rel="preload" as="image"> в +layout.svelte.                                                                                                                                                                       
  7. Comments: в Post.svelte нет long-press / double-tap эмодзи-реакций, нет thread-visualization (L-линия от parent к child), нет optimistic «sending…» стейта отдельного от isSubmitting (оптимистик пост появится
   только после ответа сервера).                                                                                                                                                                                    
  8. PostCardSkeleton фиксированной формы — при лайв-переходе skeleton → real post прыгает layout (CLS > 0.1). Нужен content-visibility: auto + reserved aspect-ratio для фото.                                   
  9. Нет accessibility: role="dialog" есть, но нет focus-trap, нет aria-labelledby на реальный заголовок, нет Escape handler (только в [username] модалка это делает).                                              
  10. scrollbar-width: none + скрытый скролл в feed — нарушает UX на десктопе (пользователь не видит позицию).                                                                                                      
  11. Pull-to-refresh в MobileFeed сам по себе, но нет haptic-confirm-фазы при отпускании через threshold.                                                                                                          
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  1.3. Performance проблемы                                                                                                                                                                                         
                                                                                                                                                                                                                  
  1. Нет code-splitting по роутам кроме composer. Весь mobile-стек (MobileFeed + MobileExplore + MobileProfile + UserProfileOverlay + CommentSheet) грузится при первом клике в mini-app.
  2. В MobileLayout composeOpen.subscribe + profileNav.subscribe + activeTab.subscribe три subscribe на module-scope без гарантированного unmount (есть только один return () => unsubscribe() внутри onMount,      
  остальные subscribe вне его).                                                                                                                                                                                     
  3. viewTracker создаёт один глобальный IntersectionObserver — ок, но Map<Element, …> растёт при SPA-навигации; нет очистки при logout.                                                                            
  4. replyStates = $state<Map<number, ReplyState>>(new Map()) + new Map(replyStates) каждый setReplyState → O(n) каждый тик набора текста в reply-композере. При 50+ комментариях — заметный джанк при typing.      
  5. Пагинация feed: posts = [...posts, ...res.content] — полный ресурс reactive update; при 200+ постах рендер тяжелеет. Нужен windowing (virtual list) — @tanstack/svelte-virtual или svelte-virtual-list.        
  6. fetch без AbortSignal на unmount компонента — если пользователь быстро переключает таб, ответы приходят «в никуда» и тригерят $state на несуществующей инстансе (Svelte 5 warn'ит в dev).                      
  7. Нет Service Worker / no <link rel="prefetch"> для критических роутов.                                                                                                                                          
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  1.4. Безопасность / Корректность                                                                                                                                                                                  
                                                                                                                                                                                                                  
  1. JWT в localStorage — XSS-рискован. План: перевести на HttpOnly cookie + CSRF token header (это тоже требует backend-правки, но план должен отражать).
  2. ngrok-skip-browser-warning — зашит в prod, срабатывает в TMA. Оставить только в DEV.                                                                                                                           
  3. credentials: 'include' + Authorization: Bearer — дублирование стратегий auth. Выбрать одну.                                                                                                                    
  4. Нет input validation на клиенте (Zod) — RegisterRequest, LoginRequest, CreatePostRequest шлются «как есть».                                                                                                    
  5. console.error('[MobileFeed] fetchFeed error:', err) в прод-коде — правило no console.log нарушено.                                                                                                             
  6. ApiError выбрасывается, но нигде не ловится централизованно (401 → надо logout, 403 → toast, 408 → retry button).                                                                                              
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  1.5. Тестирование                                                                                                                                                                                                 
                                                                                                                                                                                                                  
  Покрытие катастрофически низкое:
  - Есть: auth.store.test.ts, client.test.ts, telegram.test.ts, formatRelativeTime.test.ts (всё < 300 LOC).
  - Нет ни одного component-теста (@testing-library/svelte), ни одного +page.ts load-теста, ни одного E2E (Playwright не установлен).                                                                               
  - Ни один из 1000+ LOC файлов не покрыт.
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                             
  1.6. Мелкое, но раздражающее                                                                                                                                                                                      
                                                                                                                                                                                                                    
  - src/lib/services/index.ts не реэкспортирует stravaService, authService — барель неполный, поэтому в коде смесь $lib/services и $lib/services/strava.service.
  - src/lib/types/index.ts тоже частично — Comment['replies'] не виден без прямого импорта.                                                                                                                         
  - PostStatus объявлен как type PostStatus = 'DRAFT' | 'PUBLISHED' | 'DELETED' | 'ARCHIVED' — используется только PUBLISHED в UI, значит в бэке 4 статуса, а на фронте нет filtered view. Либо удалить, либо       
  применить.                                                                                                                                                                                                        
  - В PostCard.svelte и Post/Post.svelte DEFAULT_REACTIONS захардкожен { GIGACHAD: 0, THE_ROCK: 0, DAVID: 0 }, хотя есть $lib/reactions — только один из двух файлов его использует.                                
  - package.json без svelte-preprocess, без prettier, без eslint, без playwright, без @testing-library/svelte — tooling пустой.                                                                                     
  - Нет CSS-системы: ни Tailwind, ни UnoCSS, ни CSS-modules. Каждый компонент — 200-500 строк своего CSS, тысячи дубликатов .sheet, .backdrop, .avatar, .chip.                                                      
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  2. Рефакторинг-план для агента critiqal-svelte                                                                                                                                                                    
                                                                                                                                                                                                                    
  План сделан в 7 фаз, каждая — независимо мержабельна, каждая — с проверяемым acceptance criteria. Агент должен двигаться строго по порядку; каждая фаза = один PR, один focused commit.
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                             
  ФАЗА 0 — Tooling и baseline (0.5 дня)                                                                                                                                                                             
                                                                                                                                                                                                                    
  Цель: инструментарий, без которого дальнейший рефакторинг небезопасен.
                                                                                                                                                                                                                    
  1. Добавить в devDependencies: prettier, prettier-plugin-svelte, eslint, eslint-plugin-svelte, @typescript-eslint/*, @testing-library/svelte, @testing-library/jest-dom, @playwright/test, zod, unocss (или       
  @unocss/svelte-scoped).                                                                                                                                                                                           
  2. prettier.config.js, eslint.config.js, uno.config.ts — строгий TS, no-console, svelte/no-at-html-tags, svelte/valid-compile.                                                                                    
  3. playwright.config.ts с проектами mobile-chrome и desktop-chrome.                                                                                                                                               
  4. Скрипты: lint, lint:fix, format, test:e2e, test:e2e:ui.                                                                                                                                                        
  5. tsconfig.json: "strict": true, "noUncheckedIndexedAccess": true, "exactOptionalPropertyTypes": true.                                                                                                           
  6. Настроить PostToolUse hook Prettier (уже описано в rules/typescript/hooks.md).                                                                                                                                 
  7. Acceptance: npm run lint && npm run check && npm run test зелёные; репозиторий форматирован.                                                                                                                   
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  ФАЗА 1 — SvelteKit routing & data loading (1-2 дня) — УСТРАНЯЕТ ЧЁРНЫЕ ЭКРАНЫ                                                                                                                                     
                                                                                                                                                                                                                    
  Цель: ни одной страницы без +page.ts; везде prefetch-on-hover; SSR данных там, где возможно.                                                                                                                    
                                                                                                                                                                                                                    
  1. Создать src/routes/+layout.ts:                                                                                                                                                                                 
  export const ssr = true;                                                                                                                                                                                          
  export const prerender = false;                                                                                                                                                                                   
  export const load: LayoutLoad = async ({ fetch, depends }) => {                                                                                                                                                   
    depends('app:auth');                                                                                                                                                                                            
    // hydrate auth from cookie (когда backend на cookies)                                                                                                                                                          
    return { me: await authApi.me(fetch).catch(() => null) };                                                                                                                                                       
  };                                                                                                                                                                                                                
  2. +layout.svelte: убрать if (!mounted) <чёрный div> полностью. Вместо этого — render children всегда, а MobileLayout выбирать через server-side user-agent detection в +layout.server.ts (передать isMobile:     
  boolean в data).                                                                                                                                                                                                  
  3. Для каждой страницы:                                                                                                                                                                                         
    - src/routes/+page.ts → load({ fetch }) { return { feed: postService.getFeed(0, { fetch }) } } (передавать SvelteKit-ный fetch, чтобы SSR работал).                                                             
    - src/routes/[username]/+page.ts → загружает profile, posts, followers, following параллельно через Promise.all.                                                                                                
    - src/routes/explore/+page.ts → начальный empty state без fetch.                                                                                                                                                
    - src/routes/login/+page.ts, register/+page.ts → load({ parent }) { const { me } = await parent(); if (me) redirect(303, '/'); }.                                                                               
  4. src/lib/api/client.ts: добавить поддержку fetch: typeof fetch параметра во всех методах (SvelteKit требует свой fetch для SSR). Создать createApiClient(fetch) factory.                                        
  5. Добавить +error.svelte на корне и на [username]:                                                                                                                                                               
    - Минималистичный fallback с retry и ссылкой домой.                                                                                                                                                             
  6. Добавить src/hooks.client.ts: глобальный handleError — logs + toast.                                                                                                                                           
  7. Добавить src/hooks.server.ts: прокидывание cookie в backend; event.locals.user из /api/auth/me.                                                                                                                
  8. На каждую <a href> в навигации добавить data-sveltekit-preload-data="hover" (дефолт ок, но проверить что не отключено).                                                                                        
  9. Acceptance:                                                                                                                                                                                                    
    - Lighthouse: FCP < 1.2 с, LCP < 2.0 с на slow-3G.                                                                                                                                                              
    - Ни одной страницы, которая показывает пустой фон > 100 мс после навигации.                                                                                                                                    
    - view-transition API подключён через SvelteKit’овский onNavigate.                                                                                                                                              
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  ФАЗА 2 — Design system & CSS-унификация (1-1.5 дня)                                                                                                                                                               
                                                                                                                                                                                                                    
  Цель: убрать дубли CSS, сделать визуальный baseline.
                                                                                                                                                                                                                    
  1. Подключить UnoCSS (легче Tailwind, zero-config, отлично работает с Svelte 5). Конфиг: токены в uno.config.ts:                                                                                                  
  theme: { colors: { bg, surface, surfaceRaised, border, text, textMuted, accent, skeleton }, spacing, radius, shadow }                                                                                             
  2. Перенести все --color-* из +layout.svelte в theme + оставить CSS-переменные как runtime-токены для prefers-color-scheme.                                                                                       
  3. Создать src/lib/ui/ — design-system пакет:                                                                                                                                                                     
    - Button.svelte, IconButton.svelte                                                                                                                                                                              
    - Avatar.svelte (инкапсулирует getInitials + <img> + fallback)                                                                                                                                                  
    - Sheet.svelte (generic bottom-sheet с drag-to-dismiss, focus-trap, Escape, backdrop-blur)                                                                                                                      
    - Modal.svelte                                                                                                                                                                                                  
    - Skeleton.svelte + SkeletonText, SkeletonAvatar, SkeletonImage                                                                                                                                                 
    - Spinner.svelte, Toast.svelte, Chip.svelte                                                                                                                                                                     
    - Tabs.svelte, Segmented.svelte                                                                                                                                                                                 
    - ReactionIcon.svelte (единая точка с preload + SVG-sprite)                                                                                                                                                     
  4. Все <div class="sheet">, <div class="backdrop">, .drag-handle — выпилить из CommentSheet, MobileProfile, UserProfileOverlay → использовать <Sheet>.                                                            
  5. prefers-reduced-motion-guard во всех анимациях (wrapper reducedMotion.svelte.ts).                                                                                                                              
  6. Preload reaction icons в +layout.svelte:                                                                                                                                                                       
  <link rel="preload" as="image" href="/assets/reactions/sprite.svg" />                                                                                                                                             
  7. Acceptance: размер CSS-бандла уменьшается на ≥ 40%, все sheets используют <Sheet>, нет ни одного hardcoded rem/px в компонентах кроме ui/.                                                                     
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  ФАЗА 3 — Единый Post компонент + split на subcomponents (1-1.5 дня)                                                                                                                                               
                                                                                                                                                                                                                    
  Цель: удалить PostCard.svelte, оставить один расширяемый <Post>, распилить его на ≤ 200 LOC частей.
                                                                                                                                                                                                                    
  1. Удалить src/lib/components/PostCard.svelte. Все импорты заменить на $lib/components/post/Post.svelte.                                                                                                          
  2. src/lib/components/post/ (lowercase — консистентно со Svelte-экосистемой):                                                                                                                                     
    - Post.svelte (≤ 180 LOC, только composition)                                                                                                                                                                   
    - PostHeader.svelte — avatar, name, username, time, options                                                                                                                                                     
    - PostBody.svelte — text + photo carousel                                                                                                                                                                       
    - PostFooter.svelte — reactions + comments button + views                                                                                                                                                       
    - PostReactions.svelte — логика reactions (оптимистик, pop-animation, haptic)                                                                                                                                   
    - PostComments.svelte — inline comments (desktop) + CTA → <CommentSheet> (mobile)                                                                                                                               
    - PostOptionsMenu.svelte                                                                                                                                                                                        
    - PostPhotoGallery.svelte (consolidate PhotoCarousel)                                                                                                                                                           
  3. Создать src/lib/features/posts/ — фичевой модуль:                                                                                                                                                              
    - usePostMutations.svelte.ts — rune-класс с методами react, unreact, addComment, deletePost, deleteComment, addReply — ЕДИНЫЙ источник оптимистичных апдейтов.                                                  
    - useComments.svelte.ts — load/paginate/replies state.                                                                                                                                                          
    - useReactions.svelte.ts — load + state + optimistic diff.                                                                                                                                                      
  4. Post.svelte принимает только post: Post, дёргает rune-хуки, передаёт колбэки в дочерние компоненты.                                                                                                            
  5. Acceptance: удалён PostCard.svelte; Post.svelte ≤ 200 LOC; все 3 роута (/, /[username], mobile feed) рендерят один и тот же <Post>.                                                                            
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  ФАЗА 4 — Query layer + унификация state (1-2 дня)                                                                                                                                                                 
                                                                                                                                                                                                                    
  Цель: убить дубликат store'ов, внедрить единый query/cache-слой с invalidation.
                                                                                                                                                                                                                    
  1. Создать src/lib/query/ — тонкий query-layer на рунах:                                                                                                                                                          
  // query.svelte.ts — inspired by TanStack Query, но ~150 LOC                                                                                                                                                      
  class Query<T> {                                                                                                                                                                                                  
    data = $state<T | null>(null);                                                                                                                                                                                  
    error = $state<Error | null>(null);                                                                                                                                                                             
    status = $state<'idle' | 'loading' | 'success' | 'error'>('idle');                                                                                                                                              
    isStale(): boolean { /* ... */ }                                                                                                                                                                                
    async fetch(): Promise<T> { /* dedupe + cache + abort */ }
    invalidate(): void;                                                                                                                                                                                             
  }                                                                                                                                                                                                               
  export function createQuery<T>(key: QueryKey, fetcher: QueryFn<T>, opts?: { staleTime?: number }): Query<T>;                                                                                                      
  export function createMutation<TArgs, T>(fn: (args: TArgs) => Promise<T>, opts?: MutationOpts<T>): Mutation<TArgs, T>;                                                                                            
    - Единая карта queryCache: Map<string, Query>.                                                                                                                                                                  
    - Abort на unmount через $effect cleanup.                                                                                                                                                                       
    - StaleTime + revalidate-on-focus.                                                                                                                                                                              
  2. Заменить существующие store'ы:                                                                                                                                                                                 
    - mobile-feed.store.ts → createQuery(['feed', page], () => postService.getFeed(page)).                                                                                                                          
    - mobile-explore.store.ts → createQuery(['search', tab, query], …) с enabled: query.length > 1.                                                                                                                 
    - Оставить чисто-UI store'ы на рунах: mobile-tab.store.svelte.ts, sheet.store.svelte.ts, compose.store.svelte.ts, profile-nav.store.svelte.ts.                                                                  
  3. Унифицировать: все store файлы — .svelte.ts. Выпилить writable/derived из svelte/store полностью.                                                                                                              
  4. SvelteKit load заполняет initial data в query-cache через setQueryData(key, data) — компоненты стартуют уже с данными, без повторного fetch.                                                                   
  5. Acceptance: в проекте нет import { writable } from 'svelte/store'; навигация feed→profile→feed не триггерит повторный fetch, если данные свежие.                                                               
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  ФАЗА 5 — Декомпозиция god-файлов (2-3 дня)                                                                                                                                                                        
                                                                                                                                                                                                                    
  Цель: ни одного Svelte-файла > 400 LOC.
                                                                                                                                                                                                                    
  1. MobileProfile.svelte (2252 → ~180) разобрать на:                                                                                                                                                               
    - MobileProfile.svelte — orchestrator                                                                                                                                                                           
    - ProfileHeader.svelte                                                                                                                                                                                          
    - ProfileAvatarUpload.svelte                                                                                                                                                                                  
    - ProfileEditForm.svelte                                                                                                                                                                                        
    - ProfileStravaCard.svelte                                                                                                                                                                                    
    - ProfileStickyHeader.svelte                                                                                                                                                                                    
    - ProfilePostsList.svelte (reuse feed list)                                                                                                                                                                     
    - FollowersSheet.svelte, FollowingSheet.svelte (использует <Sheet>)                                                                                                                                             
    - PostDetailSheet.svelte                                                                                                                                                                                        
    - ProfileSettingsSheet.svelte                                                                                                                                                                                   
    - $lib/features/profile/useProfile.svelte.ts — query + mutations                                                                                                                                                
  2. routes/[username]/+page.svelte (1523 → ~100) — использовать те же компоненты из features/profile.                                                                                                              
  3. MobileFeed.svelte (819 → ~150):                                                                                                                                                                                
    - MobileFeed.svelte — orchestrator                                                                                                                                                                              
    - FeedPullToRefresh.svelte                                                                                                                                                                                      
    - FeedList.svelte — virtual list (@tanstack/svelte-virtual)                                                                                                                                                     
    - Pull-to-refresh — отдельный action usePullToRefresh в $lib/actions/pullToRefresh.ts                                                                                                                           
  4. MobileExplore.svelte (945 → ~120):                                                                                                                                                                             
    - ExploreSearchBar.svelte, ExplorePostsTab.svelte, ExplorePeopleTab.svelte, ExploreEmptyState.svelte                                                                                                            
    - useDebouncedSearch.svelte.ts                                                                                                                                                                                  
  5. MobilePostComposer.svelte (640 → ~180):                                                                                                                                                                      
    - ComposerTextarea.svelte, ComposerPhotoPicker.svelte, ComposerPhotoPreview.svelte                                                                                                                              
    - useComposer.svelte.ts с photo-upload-очередью                                                                                                                                                                 
  6. CommentSheet.svelte (644 → ~180): использует <Sheet>, PostComments, useComments.                                                                                                                               
  7. UserProfileOverlay.svelte (618 → ~120): reuse features/profile components.                                                                                                                                     
  8. LeftSidebar.svelte — split на SidebarNav, SidebarUserCard.                                                                                                                                                     
  9. Acceptance: ни одного .svelte > 400 LOC; все списки постов рендерят <FeedList> с virtual scroll.                                                                                                               
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  ФАЗА 6 — UX/Animation polish (1-1.5 дня)                                                                                                                                                                          
                                                                                                                                                                                                                    
  Цель: уровень лучшего приложения мира, минимализм, 60fps.
                                                                                                                                                                                                                    
  1. View Transitions API — через onNavigate:                                                                                                                                                                       
  onNavigate((nav) => {                                                                                                                                                                                             
    if (!document.startViewTransition) return;                                                                                                                                                                      
    return new Promise((resolve) => { document.startViewTransition(async () => { resolve(); await nav.complete; }); });                                                                                           
  });                                                                                                                                                                                                               
  1. Ставит view-transition-name: post-{id} на карточку — плавный morph при открытии деталей поста.                                                                                                               
  2. Reactions — переработать:                                                                                                                                                                                      
    - SVG-sprite (<symbol id="r-gigachad">…</symbol>), preload в +layout.                                                                                                                                           
    - Pop-анимация: spring из svelte/motion с stiffness: 0.3, damping: 0.4.                                                                                                                                         
    - Haptic: selection-changed (тоньше чем impactLight) на hover, impact на click.                                                                                                                                 
    - Long-press (600 мс) — показ tooltip с reaction-name.                                                                                                                                                          
  3. Comments:                                                                                                                                                                                                      
    - Optimistic state: новый комментарий появляется сразу с opacity: 0.6 + spinner справа; при success — плавный fade-to-1.                                                                                        
    - Reply thread visualization — тонкая линия border-left: 1px solid var(--border) + отступ, без bordered-boxes.                                                                                                  
    - Auto-link detection (простой regex) + user-mention подсветка.                                                                                                                                                 
    - Double-tap comment = реакция 💪 (в стиле Instagram).                                                                                                                                                          
  4. Pull-to-refresh: добавить 3 фазы (idle/armed/loading) с microinteractions (индикатор rotating на armed). Haptic notificationOccurred('success') на success.                                                    
  5. Skeleton → content transition: crossfade (Svelte crossfade) между skeleton и реальной карточкой с одинаковым key-атрибутом.                                                                                    
  6. Scroll restoration — SvelteKit делает это автоматически для <a>, но для mobile-feed overlay (overlay backgrounds scroll-locked) — ручная логика сохранения scrollTop в feed-store.                             
  7. Empty states — дизайн-минимализм: иконка + заголовок (1 строка) + текст (1 строка) + CTA. Без эмодзи, без картинок.                                                                                            
  8. Error boundaries: in-line error banner с retry, а не toast и не fullscreen.                                                                                                                                    
  9. prefers-reduced-motion: весь motion через useReducedMotion() — transition: reducedMotion ? 'none' : spring(...).                                                                                               
  10. Focus management: в <Sheet> — focus trap через focus-trap npm-пакет; при закрытии — возврат фокуса на элемент-триггер.                                                                                        
  11. Accessibility: aria-live="polite" для feed-updated счётчика, aria-busy на списках, keyboard-nav в reactions (←/→ + Enter).                                                                                    
  12. Acceptance: Lighthouse a11y ≥ 95, Performance ≥ 90 mobile; Motion 60fps в Chrome DevTools Performance.                                                                                                        
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  ФАЗА 7 — Безопасность, тесты, cleanup (1-2 дня)                                                                                                                                                                   
                                                                                                                                                                                                                    
  1. Security:
    - Миграция auth на HttpOnly cookie: apiClient без setInMemoryToken, authStore без localStorage.auth_token (оставить только auth_user как soft-cache + TTL 60 мин, либо вынести в query-cache).                  
    - Централизованный 401-handler в apiClient: на 401 — authStore.logout() + goto('/login').                                                                                                                       
    - Zod-схемы для всех DTO в $lib/schemas/ — login, register, createPost, updateProfile.                                                                                                                          
    - Убрать ngrok-skip-browser-warning из production-пути (if (import.meta.env.DEV) only).                                                                                                                         
    - cspPolicy + CSP headers в +layout.server.ts.                                                                                                                                                                  
  2. Тесты:                                                                                                                                                                                                         
    - Unit: все хуки useReactions, usePostMutations, useComments, useComposer, query-layer (90%+).                                                                                                                  
    - Component: Post.svelte, Sheet.svelte, Avatar.svelte, PostReactions.svelte, CommentSheet.svelte через @testing-library/svelte.                                                                                 
    - E2E (Playwright): login flow, создать пост + фото, лайк + анлайк, добавить/удалить коммент, прокрутка feed, follow/unfollow, профиль и Strava.                                                                
    - Coverage target: 80%+ на $lib/** (исключая ui/*.svelte).                                                                                                                                                      
  3. Cleanup (вызвать refactor-cleaner агента):                                                                                                                                                                     
    - Удалить мёртвый код (canEdit = false, дубли formatRelativeTime/getInitials, mirror-write в auth.store).                                                                                                       
    - knip + ts-prune прогон.                                                                                                                                                                                       
    - Удалить coverage/ из репо, добавить в .gitignore.                                                                                                                                                             
  4. Docs: обновить docs/CODEMAPS/frontend.md через doc-updater агента.                                                                                                                                             
  5. Acceptance: npm run check && npm run lint && npm test && npm run test:e2e — всё зелёное; no dead code reported by knip.                                                                                        
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  3. Целевая структура проекта                                                                                                                                                                                      
                                                                                                                                                                                                                    
  frontend/src/
  ├── app.html                                                                                                                                                                                                      
  ├── app.d.ts                                                                                                                                                                                                      
  ├── hooks.client.ts
  ├── hooks.server.ts                                                                                                                                                                                               
  ├── routes/                                                                                                                                                                                                     
  │   ├── +layout.svelte        (≤ 80 LOC)                                                                                                                                                                          
  │   ├── +layout.ts            (auth bootstrap)
  │   ├── +layout.server.ts     (isMobile detection)                                                                                                                                                                
  │   ├── +error.svelte                                                                                                                                                                                             
  │   ├── +page.svelte          (≤ 120 LOC, только composition)                                                                                                                                                     
  │   ├── +page.ts              (feed load)                                                                                                                                                                         
  │   ├── (auth)/                                                                                                                                                                                                 
  │   │   ├── login/+page.svelte                                                                                                                                                                                    
  │   │   ├── login/+page.ts                                                                                                                                                                                        
  │   │   ├── register/+page.svelte
  │   │   └── register/+page.ts                                                                                                                                                                                     
  │   ├── explore/+page.svelte  (+ +page.ts)                                                                                                                                                                        
  │   ├── [username]/+page.svelte  (+ +page.ts, + +error.svelte)
  │   └── settings/+page.svelte                                                                                                                                                                                     
  ├── lib/                                                                                                                                                                                                          
  │   ├── api/                                                                                                                                                                                                      
  │   │   ├── client.ts         (factory createApiClient(fetch))                                                                                                                                                    
  │   │   ├── endpoints.ts                                                                                                                                                                                          
  │   │   └── error-handler.ts  (401, 408, network)
  │   ├── schemas/              (Zod)                                                                                                                                                                               
  │   │   ├── auth.schema.ts                                                                                                                                                                                      
  │   │   ├── post.schema.ts                                                                                                                                                                                        
  │   │   └── user.schema.ts                                                                                                                                                                                        
  │   ├── types/                (inferred from zod где можно)
  │   ├── query/                                                                                                                                                                                                    
  │   │   ├── query.svelte.ts                                                                                                                                                                                     
  │   │   ├── mutation.svelte.ts                                                                                                                                                                                    
  │   │   └── cache.ts                                                                                                                                                                                            
  │   ├── services/             (чистые API-обёртки, все принимают { fetch })                                                                                                                                       
  │   ├── stores/               (только UI-state, все .svelte.ts)                                                                                                                                                   
  │   │   ├── auth.store.svelte.ts                                                                                                                                                                                  
  │   │   ├── mobile-tab.store.svelte.ts                                                                                                                                                                            
  │   │   ├── sheet.store.svelte.ts                                                                                                                                                                                 
  │   │   ├── compose.store.svelte.ts                                                                                                                                                                             
  │   │   └── profile-nav.store.svelte.ts                                                                                                                                                                           
  │   ├── features/             (фичевые use-case hooks)                                                                                                                                                            
  │   │   ├── posts/
  │   │   │   ├── usePostMutations.svelte.ts                                                                                                                                                                        
  │   │   │   ├── useReactions.svelte.ts                                                                                                                                                                          
  │   │   │   ├── useComments.svelte.ts                                                                                                                                                                             
  │   │   │   └── useComposer.svelte.ts                                                                                                                                                                           
  │   │   ├── profile/useProfile.svelte.ts                                                                                                                                                                          
  │   │   ├── explore/useSearch.svelte.ts                                                                                                                                                                           
  │   │   └── auth/useAuth.svelte.ts
  │   ├── ui/                   (design system, 15-20 primitives)                                                                                                                                                   
  │   │   ├── Button.svelte                                                                                                                                                                                         
  │   │   ├── Sheet.svelte                                                                                                                                                                                          
  │   │   ├── Modal.svelte                                                                                                                                                                                          
  │   │   ├── Avatar.svelte                                                                                                                                                                                       
  │   │   ├── Skeleton.svelte                                                                                                                                                                                       
  │   │   ├── Spinner.svelte
  │   │   ├── Chip.svelte                                                                                                                                                                                           
  │   │   ├── IconButton.svelte                                                                                                                                                                                     
  │   │   ├── Tabs.svelte
  │   │   ├── Segmented.svelte                                                                                                                                                                                      
  │   │   ├── Toast.svelte                                                                                                                                                                                        
  │   │   ├── ReactionIcon.svelte                                                                                                                                                                                   
  │   │   └── index.ts
  │   ├── components/                                                                                                                                                                                               
  │   │   ├── post/             (Post + подкомпоненты)                                                                                                                                                              
  │   │   ├── feed/             (FeedList, PullToRefresh)
  │   │   ├── profile/          (ProfileHeader, ProfilePostsList, ProfileStravaCard, …)                                                                                                                             
  │   │   ├── comments/         (CommentSheet, CommentList, CommentItem)                                                                                                                                            
  │   │   ├── composer/         (Composer, ComposerPhotoPicker)                                                                                                                                                     
  │   │   ├── mobile/                                                                                                                                                                                               
  │   │   │   ├── MobileLayout.svelte    (≤ 120)                                                                                                                                                                    
  │   │   │   ├── MobileFeed.svelte      (≤ 150)                                                                                                                                                                  
  │   │   │   ├── MobileExplore.svelte   (≤ 150)                                                                                                                                                                    
  │   │   │   ├── MobileProfile.svelte   (≤ 180)                                                                                                                                                                  
  │   │   │   ├── BottomNav.svelte                                                                                                                                                                                  
  │   │   │   └── UserProfileOverlay.svelte  (≤ 150)                                                                                                                                                              
  │   │   └── desktop/                                                                                                                                                                                              
  │   │       └── LeftSidebar.svelte                                                                                                                                                                              
  │   ├── actions/              (Svelte use:… directives)                                                                                                                                                           
  │   │   ├── pullToRefresh.ts                                                                                                                                                                                      
  │   │   ├── infiniteScroll.ts                                                                                                                                                                                     
  │   │   ├── focusTrap.ts                                                                                                                                                                                          
  │   │   ├── clickOutside.ts                                                                                                                                                                                       
  │   │   └── viewTransitions.ts
  │   ├── motion/                                                                                                                                                                                                   
  │   │   ├── reducedMotion.svelte.ts                                                                                                                                                                               
  │   │   ├── spring-presets.ts
  │   │   └── crossfade.ts                                                                                                                                                                                          
  │   ├── utils/                (чистые функции)                                                                                                                                                                    
  │   │   ├── formatRelativeTime.ts
  │   │   ├── formatCount.ts                                                                                                                                                                                        
  │   │   ├── getInitials.ts                                                                                                                                                                                      
  │   │   └── viewTracker.ts    (через сервис, не напрямую apiClient)                                                                                                                                               
  │   ├── telegram/             (TMA SDK thin adapter)                                                                                                                                                              
  │   └── reactions/            (reactions config + sprite)                                                                                                                                                         
  └── tests/                                                                                                                                                                                                        
      └── e2e/                                                                                                                                                                                                      
          ├── login.spec.ts                                                                                                                                                                                         
          ├── feed.spec.ts                                                                                                                                                                                          
          ├── post-create.spec.ts
          ├── reactions.spec.ts                                                                                                                                                                                     
          ├── comments.spec.ts                                                                                                                                                                                      
          └── profile.spec.ts
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                             
  4. Сводный definition of done
                                                                                                                                                                                                                    
  - Ни одной страницы без +page.ts/+layout.ts; SSR данных там где возможно.
  - Ни одного .svelte > 400 LOC.                                                                                                                                                                                    
  - Удалён PostCard.svelte, один <Post> для desktop + mobile.                                                                                                                                                       
  - Все state-store'ы — на рунах (.svelte.ts), нет svelte/store writable.                                                                                                                                           
  - Есть query-layer с dedupe + cache + stale time + abort; все API-дёрганья через него.                                                                                                                            
  - Design-system $lib/ui/ покрывает: Button, Sheet, Modal, Avatar, Skeleton, Chip, ReactionIcon (≥ 15 примитивов).                                                                                                 
  - View Transitions API включён для post-detail и profile-overlay переходов.                                                                                                                                       
  - Reduced-motion respected везде.                                                                                                                                                                                 
  - Pull-to-refresh, optimistic reactions/comments, optimistic posting работают с rollback.                                                                                                                         
  - Auth на HttpOnly cookie, token не в localStorage.                                                                                                                                                               
  - Lighthouse Mobile: Perf ≥ 90, a11y ≥ 95, best-practices ≥ 95.                                                                                                                                                   
  - FCP < 1.2 с, LCP < 2.0 с, CLS < 0.05 на slow 3G (DevTools throttling).                                                                                                                                          
  - Coverage ≥ 80% на $lib/** (ui и types исключены).                                                                                                                                                               
  - E2E (Playwright) покрывают 6 critical flows.                                                                                                                                                                    
  - Zero console.log/console.error в prod build (enforced by lint).                                                                                                                                                 
  - knip / ts-prune чистые.                                                                                                                                                                                         
                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                               
  5. Порядок исполнения и точки контроля                                                                                                                                                                          
                                                                                                                                                                                                                    
  1. Фаза 0 → merge → smoke check.
  2. Фаза 1 → обязательная ручная проверка: кликнуть по всем страницам, убедиться что чёрный экран ушёл.                                                                                                            
  3. Фаза 2 → визуальный regression review (скриншоты до/после).                                                                                                                                                    
  4. Фаза 3 → PostCard удалён, код-ревью critiqal-svelte.                                                                                                                                                           
  5. Фаза 4 → проверить что нет повторных fetch'ей при навигации (DevTools Network).                                                                                                                                
  6. Фаза 5 → контроль LOC (wc -l src/**/*.svelte | sort -n).                                                                                                                                                       
  7. Фаза 6 → Lighthouse + ручной UX-обход на Telegram iOS Mini-App.                                                                                                                                                
  8. Фаза 7 → финальный security-review через security-reviewer agent + code-reviewer + E2E.                                                                                                                        
                                                                                                                                                                                                                    
  Это можно отдавать в critiqal-svelte фазами. Начать рекомендую с Фазы 0 + Фазы 1 — они дают самый заметный эффект (исчезает чёрный экран) и не блокируют остальную работу. Дальше Фаза 3 (единый Post) параллельно
   с Фазой 2 (design system) — они почти не пересекаются.                                                                                                                                                           
                                                                                                                                                                                                                    
  Если готов — скажи, какую фазу запускаем первой, и я передам задачу critiqal-svelte с точной спецификацией.                                                                                                       
   
✻ Sautéed for 3m 45s                                                                                                                                                                                                
                                                                                                                                                                                                                  
────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
❯  
────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
  ⏵⏵ accept edits on (shift+tab to cycle)                                                                                                                                                                         

          ├── post-create.spec.ts
          ├── reactions.spec.ts
          ├── comments.spec.ts
          └── profile.spec.ts

  ---
  4. Сводный definition of done

  - Ни одной страницы без +page.ts/+layout.ts; SSR данных там где возможно.
  - Ни одного .svelte > 400 LOC.
  - Удалён PostCard.svelte, один <Post> для desktop + mobile.
  - Все state-store'ы — на рунах (.svelte.ts), нет svelte/store writable.
  - Есть query-layer с dedupe + cache + stale time + abort; все API-дёрганья через него.
  - Design-system $lib/ui/ покрывает: Button, Sheet, Modal, Avatar, Skeleton, Chip, ReactionIcon (≥ 15 примитивов).
  - View Transitions API включён для post-detail и profile-overlay переходов.
  - Reduced-motion respected везде.
  - Pull-to-refresh, optimistic reactions/comments, optimistic posting работают с rollback.
  - Auth на HttpOnly cookie, token не в localStorage.
  - Lighthouse Mobile: Perf ≥ 90, a11y ≥ 95, best-practices ≥ 95.
  - FCP < 1.2 с, LCP < 2.0 с, CLS < 0.05 на slow 3G (DevTools throttling).
  - Coverage ≥ 80% на $lib/** (ui и types исключены).
  - E2E (Playwright) покрывают 6 critical flows.
  - Zero console.log/console.error в prod build (enforced by lint).
  - knip / ts-prune чистые.

  ---
  5. Порядок исполнения и точки контроля

  1. Фаза 0 → merge → smoke check.
  2. Фаза 1 → обязательная ручная проверка: кликнуть по всем страницам, убедиться что чёрный экран ушёл.
  3. Фаза 2 → визуальный regression review (скриншоты до/после).
  4. Фаза 3 → PostCard удалён, код-ревью critiqal-svelte.
  5. Фаза 4 → проверить что нет повторных fetch'ей при навигации (DevTools Network).
  6. Фаза 5 → контроль LOC (wc -l src/**/*.svelte | sort -n).
  7. Фаза 6 → Lighthouse + ручной UX-обход на Telegram iOS Mini-App.
  8. Фаза 7 → финальный security-review через security-reviewer agent + code-reviewer + E2E.

  Это можно отдавать в critiqal-svelte фазами. Начать рекомендую с Фазы 0 + Фазы 1 — они дают самый заметный эффект (исчезает чёрный экран) и не блокируют остальную работу. Дальше Фаза 3 (единый Post) параллельно
   с Фазой 2 (design system) — они почти не пересекаются.

  Если готов — скажи, какую фазу запускаем первой, и я передам задачу critiqal-svelte с точной спецификацией.

✻ Sautéed for 3m 45s

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
❯  
────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
          ├── post-create.spec.ts
          ├── reactions.spec.ts
          ├── comments.spec.ts
          └── profile.spec.ts

  ---
  4. Сводный definition of done

  - Ни одной страницы без +page.ts/+layout.ts; SSR данных там где возможно.
  - Ни одного .svelte > 400 LOC.
  - Удалён PostCard.svelte, один <Post> для desktop + mobile.
  - Все state-store'ы — на рунах (.svelte.ts), нет svelte/store writable.
  - Есть query-layer с dedupe + cache + stale time + abort; все API-дёрганья через него.
  - Design-system $lib/ui/ покрывает: Button, Sheet, Modal, Avatar, Skeleton, Chip, ReactionIcon (≥ 15 примитивов).
  - View Transitions API включён для post-detail и profile-overlay переходов.
  - Reduced-motion respected везде.
  - Pull-to-refresh, optimistic reactions/comments, optimistic posting работают с rollback.
  - Auth на HttpOnly cookie, token не в localStorage.
  - Lighthouse Mobile: Perf ≥ 90, a11y ≥ 95, best-practices ≥ 95.
  - FCP < 1.2 с, LCP < 2.0 с, CLS < 0.05 на slow 3G (DevTools throttling).
  - Coverage ≥ 80% на $lib/** (ui и types исключены).
  - E2E (Playwright) покрывают 6 critical flows.
  - Zero console.log/console.error в prod build (enforced by lint).
  - knip / ts-prune чистые.

  ---
  5. Порядок исполнения и точки контроля

  1. Фаза 0 → merge → smoke check.
  2. Фаза 1 → обязательная ручная проверка: кликнуть по всем страницам, убедиться что чёрный экран ушёл.
  3. Фаза 2 → визуальный regression review (скриншоты до/после).
  4. Фаза 3 → PostCard удалён, код-ревью critiqal-svelte.
  5. Фаза 4 → проверить что нет повторных fetch'ей при навигации (DevTools Network).
  6. Фаза 5 → контроль LOC (wc -l src/**/*.svelte | sort -n).
  7. Фаза 6 → Lighthouse + ручной UX-обход на Telegram iOS Mini-App.
  8. Фаза 7 → финальный security-review через security-reviewer agent + code-reviewer + E2E.

  Это можно отдавать в critiqal-svelte фазами. Начать рекомендую с Фазы 0 + Фазы 1 — они дают самый заметный эффект (исчезает чёрный экран) и не блокируют остальную работу. Дальше Фаза 3 (единый Post) параллельно
   с Фазой 2 (design system) — они почти не пересекаются.

  Если готов — скажи, какую фазу запускаем первой, и я передам задачу critiqal-svelte с точной спецификацией.
