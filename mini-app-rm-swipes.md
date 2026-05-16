# CLAUDE.md — Telegram Mini App: Fullscreen + Swipe Lock + Dynamic Header

## ТВОЯ ЗАДАЧА

Ты внедряешь в Telegram Mini App три вещи одновременно, везде, без исключений:

1. **Fullscreen-режим** — приложение занимает весь экран
2. **Блокировка свайпа** — закрытие ТОЛЬКО по нативной кнопке хедера, никаких свайпов
3. **Динамический хедер** — на каждой странице свои кнопки и обработчики

Эти три вещи — единая система. Никогда не внедряй одно без двух других.

---

## ОБЯЗАТЕЛЬНЫЙ ПОРЯДОК ДЕЙСТВИЙ

### Шаг 1 — Сканирование проекта

Перед любым кодом сделай:
```
find . -name "*.html" -o -name "*.js" -o -name "*.ts" -o -name "*.jsx" -o -name "*.tsx" -o -name "*.vue" | head -50
```

Найди:
- Где подключается `telegram-web-app.js` (или SDK)
- Где находится точка входа (index.html / main.ts / App.vue / App.tsx)
- Какой роутер используется (React Router / Vue Router / Next.js / ручной)
- Есть ли уже какие-то вызовы `tg.expand()`, `tg.ready()` и т.д.

### Шаг 2 — Создай центральный модуль

Создай файл `src/lib/telegram.ts` (или `.js` если нет TypeScript).
Весь Telegram API — ТОЛЬКО через этот файл. Никаких прямых вызовов `window.Telegram.WebApp` в компонентах.

### Шаг 3 — Внедри инициализацию в точку входа

В самом начале жизненного цикла приложения (до рендера) вызови `initTelegram()`.

### Шаг 4 — Внедри роутер-интеграцию

На каждый переход страницы вызывай `setHeaderForPage(pageName)`.

---

## ФАЙЛ: src/lib/telegram.ts

Создай этот файл ТОЧНО в таком виде (адаптируй только типы под стек проекта):

```typescript
// src/lib/telegram.ts
// Единственный файл для работы с Telegram WebApp API.
// Все остальные файлы импортируют только отсюда.

declare global {
  interface Window {
    Telegram: {
      WebApp: TelegramWebApp;
    };
  }
}

interface TelegramWebApp {
  version: string;
  platform: string;
  colorScheme: 'light' | 'dark';
  themeParams: Record<string, string>;
  isExpanded: boolean;
  isFullscreen: boolean;
  isVerticalSwipesEnabled: boolean;
  isClosingConfirmationEnabled: boolean;
  isActive: boolean;
  safeAreaInset: { top: number; bottom: number; left: number; right: number };
  contentSafeAreaInset: { top: number; bottom: number; left: number; right: number };
  BackButton: {
    isVisible: boolean;
    show(): void;
    hide(): void;
    onClick(cb: () => void): void;
    offClick(cb: () => void): void;
  };
  MainButton: {
    text: string;
    isVisible: boolean;
    isActive: boolean;
    show(): void;
    hide(): void;
    enable(): void;
    disable(): void;
    setText(text: string): void;
    onClick(cb: () => void): void;
    offClick(cb: () => void): void;
    setParams(params: {
      text?: string;
      color?: string;
      text_color?: string;
      has_shine_effect?: boolean;
      is_active?: boolean;
      is_visible?: boolean;
    }): void;
    showProgress(leaveActive?: boolean): void;
    hideProgress(): void;
  };
  SecondaryButton: {
    text: string;
    isVisible: boolean;
    show(): void;
    hide(): void;
    setText(text: string): void;
    onClick(cb: () => void): void;
    offClick(cb: () => void): void;
    setParams(params: Record<string, unknown>): void;
  };
  SettingsButton: {
    isVisible: boolean;
    show(): void;
    hide(): void;
    onClick(cb: () => void): void;
    offClick(cb: () => void): void;
  };
  isVersionAtLeast(version: string): boolean;
  expand(): void;
  ready(): void;
  close(): void;
  requestFullscreen(): void;
  exitFullscreen(): void;
  disableVerticalSwipes(): void;
  enableVerticalSwipes(): void;
  enableClosingConfirmation(): void;
  disableClosingConfirmation(): void;
  setHeaderColor(color: string): void;
  setBackgroundColor(color: string): void;
  setBottomBarColor(color: string): void;
  showAlert(message: string, callback?: () => void): void;
  showConfirm(message: string, callback?: (ok: boolean) => void): void;
  showPopup(params: {
    title?: string;
    message: string;
    buttons?: Array<{ id?: string; type?: string; text?: string }>;
  }, callback?: (buttonId: string) => void): void;
  onEvent(eventType: string, handler: (...args: unknown[]) => void): void;
  offEvent(eventType: string, handler: (...args: unknown[]) => void): void;
  HapticFeedback: {
    impactOccurred(style: 'light' | 'medium' | 'heavy' | 'rigid' | 'soft'): void;
    notificationOccurred(type: 'error' | 'success' | 'warning'): void;
    selectionChanged(): void;
  };
}

// ─── Получение инстанса ───────────────────────────────────────────────────────

function getTg(): TelegramWebApp {
  if (!window.Telegram?.WebApp) {
    throw new Error('[TG] window.Telegram.WebApp недоступен. Убедись, что telegram-web-app.js подключён до этого модуля.');
  }
  return window.Telegram.WebApp;
}

// ─── Состояние обработчиков ───────────────────────────────────────────────────
// Храним текущие обработчики, чтобы корректно снимать их при смене страницы.

let _currentBackHandler: (() => void) | null = null;
let _currentMainHandler: (() => void) | null = null;
let _currentSecondaryHandler: (() => void) | null = null;

// ─── ИНИЦИАЛИЗАЦИЯ ────────────────────────────────────────────────────────────

/**
 * Вызывай ОДИН РАЗ в точке входа приложения, ДО первого рендера.
 * Устанавливает fullscreen, блокирует свайпы, задаёт цвета.
 */
export function initTelegram(): void {
  const tg = getTg();

  // 1. Развернуть до максимальной высоты (работает на всех версиях)
  tg.expand();

  // 2. Fullscreen-режим (Bot API 8.0+)
  if (tg.isVersionAtLeast('8.0')) {
    tg.requestFullscreen();

    // Слушаем переход в fullscreen, чтобы применить safe area
    tg.onEvent('fullscreenChanged', handleFullscreenChanged);
    tg.onEvent('fullscreenFailed', handleFullscreenFailed);
  }

  // 3. БЛОКИРОВКА ВЕРТИКАЛЬНЫХ СВАЙПОВ (Bot API 7.7+)
  // После этого вызова закрытие/сворачивание свайпом по контенту — НЕВОЗМОЖНО.
  // Пользователь может закрыть приложение ТОЛЬКО через кнопку × в хедере Telegram.
  if (tg.isVersionAtLeast('7.7')) {
    tg.disableVerticalSwipes();
  }

  // 4. Включить диалог подтверждения закрытия (дополнительный слой защиты)
  // Если пользователь нажмёт × в хедере — появится "Вы уверены?"
  if (tg.isVersionAtLeast('6.2')) {
    tg.enableClosingConfirmation();
  }

  // 5. Задать цвет хедера (критично для fullscreen — влияет на контраст статус-бара)
  const headerColor = tg.themeParams.bg_color || tg.themeParams.secondary_bg_color || '#ffffff';
  tg.setHeaderColor(headerColor);

  // 6. Применить safe area через CSS-переменные (для нотчей и навигационных баров)
  applySafeArea();
  tg.onEvent('safeAreaChanged', applySafeArea);
  tg.onEvent('contentSafeAreaChanged', applySafeArea);

  // 7. Обработка паузы/возобновления (Bot API 8.0+)
  if (tg.isVersionAtLeast('8.0')) {
    tg.onEvent('activated', () => {
      // Приложение снова активно (вышло из свёрнутого состояния)
      // Здесь можно возобновить анимации, таймеры, polling
    });
    tg.onEvent('deactivated', () => {
      // Приложение свёрнуто / перешло в фон
      // Здесь pauseAnimations(), stopPolling() и т.д.
    });
  }

  // 8. Последний вызов — сообщить Telegram что приложение готово к показу
  // Это скрывает loading-заглушку. ВСЕГДА вызывать последним.
  tg.ready();

  console.info('[TG] Инициализация завершена. Версия:', tg.version, '| Платформа:', tg.platform);
}

// ─── SAFE AREA ────────────────────────────────────────────────────────────────

/**
 * Пишет значения safe area в CSS-переменные на :root.
 * Вызывается при инициализации и при каждом изменении (поворот экрана и т.д.)
 */
function applySafeArea(): void {
  const tg = getTg();
  const root = document.documentElement;

  // Системные отступы (нотч, навигационная панель Android)
  root.style.setProperty('--tg-safe-top', `${tg.safeAreaInset?.top ?? 0}px`);
  root.style.setProperty('--tg-safe-bottom', `${tg.safeAreaInset?.bottom ?? 0}px`);
  root.style.setProperty('--tg-safe-left', `${tg.safeAreaInset?.left ?? 0}px`);
  root.style.setProperty('--tg-safe-right', `${tg.safeAreaInset?.right ?? 0}px`);

  // Контентные отступы (хедер и другие элементы самого Telegram)
  root.style.setProperty('--tg-content-top', `${tg.contentSafeAreaInset?.top ?? 0}px`);
  root.style.setProperty('--tg-content-bottom', `${tg.contentSafeAreaInset?.bottom ?? 0}px`);
}

// ─── FULLSCREEN HANDLERS ──────────────────────────────────────────────────────

function handleFullscreenChanged(): void {
  const tg = getTg();
  if (tg.isFullscreen) {
    document.body.classList.add('tg-fullscreen');
    applySafeArea();
  } else {
    document.body.classList.remove('tg-fullscreen');
  }
}

function handleFullscreenFailed(error: unknown): void {
  const err = error as { error?: string };
  if (err?.error === 'UNSUPPORTED') {
    console.warn('[TG] Fullscreen не поддерживается на этом устройстве');
  }
  // ALREADY_FULLSCREEN — не ошибка, игнорируем
}

// ─── HEADER MANAGEMENT ────────────────────────────────────────────────────────

export interface HeaderConfig {
  /** Показывать ли нативную кнопку "Назад" в хедере Telegram */
  backButton?: boolean;
  /** Обработчик нажатия кнопки "Назад". Обязателен если backButton: true */
  onBack?: () => void;

  /** Конфиг главной кнопки (синяя кнопка внизу экрана) */
  mainButton?: {
    text: string;
    visible: boolean;
    active?: boolean;
    color?: string;
    textColor?: string;
    shineEffect?: boolean;
    handler: () => void;
  };

  /** Конфиг вторичной кнопки (Bot API 7.10+) */
  secondaryButton?: {
    text: string;
    visible: boolean;
    handler: () => void;
  };
}

/**
 * Обновляет все нативные кнопки Telegram для текущей страницы.
 *
 * ВАЖНО: вызывать при КАЖДОМ переходе на страницу.
 * Функция сначала снимает все старые обработчики, затем устанавливает новые.
 * Это предотвращает накопление дублирующихся обработчиков.
 *
 * @example
 * // В роутере:
 * router.on('navigate', (page) => {
 *   setHeaderForPage(page);
 * });
 */
export function setHeader(config: HeaderConfig): void {
  const tg = getTg();

  // ── BackButton ──────────────────────────────────────────────────────────────
  // Снимаем старый обработчик ПЕРЕД установкой нового — иначе оба сработают
  if (_currentBackHandler) {
    tg.BackButton.offClick(_currentBackHandler);
    _currentBackHandler = null;
  }

  if (config.backButton && config.onBack) {
    _currentBackHandler = config.onBack;
    tg.BackButton.onClick(_currentBackHandler);
    tg.BackButton.show();
  } else {
    tg.BackButton.hide();
  }

  // ── MainButton ──────────────────────────────────────────────────────────────
  if (_currentMainHandler) {
    tg.MainButton.offClick(_currentMainHandler);
    _currentMainHandler = null;
  }

  if (config.mainButton?.visible) {
    const mb = config.mainButton;
    _currentMainHandler = mb.handler;

    tg.MainButton.setParams({
      text: mb.text,
      color: mb.color,
      text_color: mb.textColor,
      has_shine_effect: mb.shineEffect ?? false,
      is_active: mb.active ?? true,
      is_visible: true,
    });

    tg.MainButton.onClick(_currentMainHandler);
  } else {
    tg.MainButton.setParams({ is_visible: false });
  }

  // ── SecondaryButton ─────────────────────────────────────────────────────────
  if (_currentSecondaryHandler) {
    tg.SecondaryButton.offClick(_currentSecondaryHandler);
    _currentSecondaryHandler = null;
  }

  if (config.secondaryButton?.visible && tg.isVersionAtLeast('7.10')) {
    const sb = config.secondaryButton;
    _currentSecondaryHandler = sb.handler;
    tg.SecondaryButton.setParams({ text: sb.text, is_visible: true });
    tg.SecondaryButton.onClick(_currentSecondaryHandler);
  } else if (tg.isVersionAtLeast('7.10')) {
    tg.SecondaryButton.setParams({ is_visible: false });
  }
}

// ─── УТИЛИТЫ ─────────────────────────────────────────────────────────────────

/** Показать лоадер на MainButton и задизейблить его */
export function showMainButtonLoader(): void {
  const tg = getTg();
  tg.MainButton.showProgress(false); // false = задизейблить кнопку пока грузится
}

/** Скрыть лоадер на MainButton */
export function hideMainButtonLoader(): void {
  const tg = getTg();
  tg.MainButton.hideProgress();
}

/** Хаптик-фидбек — используй вместо console.log для подтверждения действий */
export const haptic = {
  light: () => getTg().HapticFeedback.impactOccurred('light'),
  medium: () => getTg().HapticFeedback.impactOccurred('medium'),
  heavy: () => getTg().HapticFeedback.impactOccurred('heavy'),
  success: () => getTg().HapticFeedback.notificationOccurred('success'),
  error: () => getTg().HapticFeedback.notificationOccurred('error'),
  warning: () => getTg().HapticFeedback.notificationOccurred('warning'),
  selection: () => getTg().HapticFeedback.selectionChanged(),
};

/** Закрыть приложение программно */
export function closeApp(): void {
  getTg().close();
}

/** Текущая цветовая схема */
export function getColorScheme(): 'light' | 'dark' {
  return getTg().colorScheme;
}

/** Текущие параметры темы Telegram */
export function getThemeParams(): Record<string, string> {
  return getTg().themeParams;
}

/** Прямой доступ к инстансу (только если нет нужного метода выше) */
export function getTelegramWebApp(): TelegramWebApp {
  return getTg();
}
```

---

## CSS — ОБЯЗАТЕЛЬНЫЙ БАЗОВЫЙ СТИЛЬ

Добавь в глобальный CSS (index.css / globals.css / app.css):

```css
/* ─── Telegram Mini App Base Styles ─── */

/* Сбрасываем все системные отступы */
* {
  box-sizing: border-box;
  -webkit-tap-highlight-color: transparent;
}

html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  /* Запрещаем overscroll-bounce на iOS — иначе пользователь увидит
     "резиновое" отпружинивание, которое выглядит как попытка закрыть */
  overscroll-behavior: none;
  overflow: hidden;
}

/* Корневой контейнер приложения */
#root, #app {
  width: 100%;
  height: 100%;
  /* Разрешаем скролл только внутри контейнера */
  overflow-y: auto;
  overflow-x: hidden;
  /* Плавный инерционный скролл на iOS */
  -webkit-overflow-scrolling: touch;
  /* КРИТИЧНО: предотвращаем "вытягивание" за края на iOS */
  overscroll-behavior-y: contain;
}

/* Safe area — применяется автоматически через initTelegram() */
:root {
  --tg-safe-top: 0px;
  --tg-safe-bottom: 0px;
  --tg-safe-left: 0px;
  --tg-safe-right: 0px;
  --tg-content-top: 0px;
  --tg-content-bottom: 0px;
}

/* В fullscreen хедер Telegram прозрачный,
   контент должен учитывать --tg-content-top */
.tg-fullscreen .page-content {
  padding-top: var(--tg-content-top);
}

/* Класс для скроллируемых контейнеров внутри страниц */
.scrollable {
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
}

/* Нижний отступ для контента — чтобы MainButton не перекрывал */
.page-content {
  padding-bottom: calc(var(--tg-safe-bottom) + 80px);
}
```

---

## ИНТЕГРАЦИЯ ПО ФРЕЙМВОРКАМ

### React + React Router

**src/main.tsx:**
```tsx
import { initTelegram } from './lib/telegram';
// Вызов ДО создания React-корня
initTelegram();

import ReactDOM from 'react-dom/client';
import App from './App';
ReactDOM.createRoot(document.getElementById('root')!).render(<App />);
```

**src/App.tsx:**
```tsx
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { setHeader } from './lib/telegram';
import { PAGE_HEADER_CONFIGS } from './config/headerConfigs';

export default function App() {
  const location = useLocation();

  useEffect(() => {
    const pageName = location.pathname.replace('/', '') || 'home';
    const config = PAGE_HEADER_CONFIGS[pageName];
    if (config) setHeader(config);
  }, [location.pathname]);

  return <RouterOutlet />;
}
```

### Vue 3 + Vue Router

**src/main.ts:**
```ts
import { initTelegram } from './lib/telegram';
initTelegram(); // ← ДО createApp

import { createApp } from 'vue';
import App from './App.vue';
import router from './router';

const app = createApp(App);
app.use(router);
app.mount('#app');
```

**src/App.vue:**
```vue
<script setup lang="ts">
import { watch } from 'vue';
import { useRoute } from 'vue-router';
import { setHeader } from '@/lib/telegram';
import { PAGE_HEADER_CONFIGS } from '@/config/headerConfigs';

const route = useRoute();

watch(() => route.name, (pageName) => {
  const config = PAGE_HEADER_CONFIGS[String(pageName)];
  if (config) setHeader(config);
}, { immediate: true });
</script>

<template>
  <RouterView />
</template>
```

### Next.js (App Router)

**app/layout.tsx:**
```tsx
'use client';
import { useEffect } from 'react';
import { initTelegram } from '@/lib/telegram';

export default function RootLayout({ children }) {
  useEffect(() => {
    initTelegram(); // useEffect гарантирует выполнение только на клиенте
  }, []);

  return (
    <html>
      <head>
        <script src="https://telegram.org/js/telegram-web-app.js?62" />
      </head>
      <body>{children}</body>
    </html>
  );
}
```

**Для каждой страницы — используй хук:**
```tsx
// hooks/useTelegramHeader.ts
import { useEffect } from 'react';
import { setHeader, HeaderConfig } from '@/lib/telegram';

export function useTelegramHeader(config: HeaderConfig) {
  useEffect(() => {
    setHeader(config);
  }, []); // Устанавливается один раз при монтировании страницы
}

// Использование в странице:
export default function CartPage() {
  const router = useRouter();

  useTelegramHeader({
    backButton: true,
    onBack: () => router.back(),
    mainButton: {
      text: 'Оформить заказ',
      visible: true,
      shineEffect: true,
      handler: () => handleCheckout(),
    },
  });

  return <div>...</div>;
}
```

### Vanilla JS (без фреймворка)

**index.html:**
```html
<!DOCTYPE html>
<html>
<head>
  <!-- telegram-web-app.js ВСЕГДА первым скриптом в <head> -->
  <script src="https://telegram.org/js/telegram-web-app.js?62"></script>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="./main.js"></script>
</body>
</html>
```

**main.js:**
```js
import { initTelegram, setHeader } from './lib/telegram.js';

// 1. Инициализация — самая первая строка
initTelegram();

// 2. Роутер
const routes = {
  home: () => import('./pages/home.js'),
  catalog: () => import('./pages/catalog.js'),
  cart: () => import('./pages/cart.js'),
};

export async function navigateTo(page, params = {}) {
  const { default: render } = await routes[page]();
  document.getElementById('app').innerHTML = '';
  await render(document.getElementById('app'), params);
  // Хедер обновляется ПОСЛЕ рендера страницы
  setHeader(PAGE_HEADER_CONFIGS[page]);
}

navigateTo('home');
```

---

## КОНФИГ ХЕДЕРОВ ПО СТРАНИЦАМ

**src/config/headerConfigs.ts:**
```typescript
import { HeaderConfig } from '../lib/telegram';
import { navigateTo } from '../router'; // твой роутер

export const PAGE_HEADER_CONFIGS: Record<string, HeaderConfig> = {
  home: {
    backButton: false,
    mainButton: {
      text: 'Начать',
      visible: true,
      handler: () => navigateTo('/catalog'),
    },
  },

  catalog: {
    backButton: true,
    onBack: () => navigateTo('/home'),
    mainButton: {
      text: 'Корзина',
      visible: true,
      handler: () => navigateTo('/cart'),
    },
  },

  cart: {
    backButton: true,
    onBack: () => navigateTo('/catalog'),
    mainButton: {
      text: 'Оформить заказ',
      visible: true,
      color: '#2ea05a',
      textColor: '#ffffff',
      shineEffect: true,
      handler: () => navigateTo('/checkout'),
    },
    secondaryButton: {
      text: 'Продолжить покупки',
      visible: true,
      handler: () => navigateTo('/catalog'),
    },
  },

  checkout: {
    backButton: true,
    onBack: () => navigateTo('/cart'),
    mainButton: {
      text: 'Оплатить',
      visible: true,
      shineEffect: true,
      handler: () => submitOrder(),
    },
  },

  // Страница без кнопок (например, профиль)
  profile: {
    backButton: true,
    onBack: () => navigateTo('/home'),
    mainButton: { visible: false, text: '', handler: () => {} },
  },

  // Страница с лоадером на кнопке
  loading: {
    backButton: false,
    mainButton: { visible: false, text: '', handler: () => {} },
  },
};

// ─── Пример динамического обновления кнопки (например, счётчик корзины) ───
export function updateCartButtonText(count: number): void {
  import('../lib/telegram').then(({ getTelegramWebApp }) => {
    const tg = getTelegramWebApp();
    tg.MainButton.setText(count > 0 ? `Корзина (${count})` : 'Корзина пуста');
    if (count === 0) tg.MainButton.disable();
    else tg.MainButton.enable();
  });
}
```

---

## ЧАСТЫЕ ОШИБКИ — НЕ ДЕЛАЙ ТАК

### ❌ Прямые вызовы в компонентах
```ts
// НЕЛЬЗЯ — Telegram API вызывается прямо в компоненте
window.Telegram.WebApp.MainButton.setText('Купить');
```
```ts
// НУЖНО — всё через lib/telegram.ts
import { setHeader } from '@/lib/telegram';
setHeader({ mainButton: { text: 'Купить', ... } });
```

### ❌ Забыть offClick перед onClick
```ts
// НЕЛЬЗЯ — накапливаются дублирующиеся обработчики
tg.MainButton.onClick(() => doSomething());
tg.MainButton.onClick(() => doOther()); // оба сработают!
```
```ts
// НУЖНО — функция setHeader() в telegram.ts делает это автоматически
setHeader({ mainButton: { ..., handler: () => doOther() } });
```

### ❌ Вызов ready() до expand() и requestFullscreen()
```ts
// НЕЛЬЗЯ
tg.ready(); // ← показывает приложение в неправильном состоянии
tg.expand();
tg.requestFullscreen();
```
```ts
// НУЖНО — initTelegram() делает это в правильном порядке
initTelegram(); // expand → fullscreen → disableSwipes → ready
```

### ❌ Скролл через margin/padding на body
```css
/* НЕЛЬЗЯ — body скроллится, пользователь может случайно потянуть вниз */
body {
  overflow-y: scroll;
}
```
```css
/* НУЖНО — скролл только внутри #root/#app */
body { overflow: hidden; }
#root { overflow-y: auto; overscroll-behavior-y: contain; }
```

### ❌ Не учитывать safe area в fullscreen
```css
/* НЕЛЬЗЯ — контент уйдёт под нотч */
.header { margin-top: 0; }
```
```css
/* НУЖНО */
.header { padding-top: var(--tg-content-top); }
```

---

## ЧЕКЛИСТ ПЕРЕД КОММИТОМ

Перед тем как считать задачу выполненной, проверь каждый пункт:

- [ ] `telegram-web-app.js` подключён в `<head>` как ПЕРВЫЙ скрипт
- [ ] `initTelegram()` вызывается ДО первого рендера
- [ ] `disableVerticalSwipes()` вызывается при инициализации
- [ ] `requestFullscreen()` вызывается при инициализации (с version check)
- [ ] `enableClosingConfirmation()` вызывается при инициализации
- [ ] `ready()` вызывается ПОСЛЕДНИМ в initTelegram
- [ ] `setHeader()` вызывается при каждом переходе страницы
- [ ] Старые обработчики снимаются через `offClick` перед установкой новых
- [ ] `overscroll-behavior-y: contain` установлен на скроллируемых контейнерах
- [ ] `overflow: hidden` на `body`, скролл только внутри `#root`
- [ ] Safe area CSS-переменные используются для отступов контента
- [ ] Нет прямых вызовов `window.Telegram.WebApp` вне `lib/telegram.ts`
- [ ] Каждая страница приложения имеет конфиг в `PAGE_HEADER_CONFIGS`
