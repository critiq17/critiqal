# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: login.spec.ts >> Login flow >> login form renders username and password fields
- Location: src/tests/e2e/login.spec.ts:19:3

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('textbox', { name: /username|email/i }).or(locator('input[name="username"]')).or(locator('input[type="email"]')).first()
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 5000ms
  - waiting for getByRole('textbox', { name: /username|email/i }).or(locator('input[name="username"]')).or(locator('input[type="email"]')).first()

```

# Page snapshot

```yaml
- generic [ref=e3]:
  - complementary "Navigation sidebar" [ref=e5]:
    - generic [ref=e6]:
      - img "Critiqal" [ref=e7]
      - generic [ref=e9]: critiqal
    - navigation "Main navigation" [ref=e10]:
      - link "Feed" [ref=e11] [cursor=pointer]:
        - /url: /
        - img [ref=e12]
        - generic [ref=e15]: Feed
    - generic [ref=e17]:
      - paragraph [ref=e18]: Welcome back
      - link "Sign in" [ref=e19] [cursor=pointer]:
        - /url: /login
      - link "Create account" [ref=e20] [cursor=pointer]:
        - /url: /register
  - main "Feed" [ref=e21]:
    - heading "Feed" [level=1] [ref=e23]
    - generic [ref=e24]:
      - article [ref=e25]:
        - generic [ref=e26]:
          - button "Profile @clown" [ref=e29] [cursor=pointer]:
            - generic [ref=e31]: C
            - generic [ref=e32]:
              - generic [ref=e33]: clown
              - generic [ref=e34]:
                - generic [ref=e35]: "@clown"
                - time [ref=e36]: ·14m
          - paragraph [ref=e37]: Helloas dasdasdas
          - region "Post photos for post 019e6d46-b209-7738-9aea-88aa26b551c4" [ref=e40]
          - generic [ref=e42]:
            - button "Like" [ref=e43] [cursor=pointer]:
              - img [ref=e45]
            - button "0 Comments" [ref=e47] [cursor=pointer]:
              - img [ref=e48]
            - button "Share" [ref=e50] [cursor=pointer]:
              - img [ref=e51]
            - generic "1":
              - generic: "1"
              - img
      - article [ref=e54]:
        - generic [ref=e55]:
          - button "Profile @clown" [ref=e58] [cursor=pointer]:
            - generic [ref=e60]: C
            - generic [ref=e61]:
              - generic [ref=e62]: clown
              - generic [ref=e63]:
                - generic [ref=e64]: "@clown"
                - time [ref=e65]: ·15m
          - paragraph [ref=e66]: Hello
          - region "Post photos for post 019e6d46-8107-7289-9b91-56cf2c327fb1" [ref=e69]
          - generic [ref=e71]:
            - button "Like" [ref=e72] [cursor=pointer]:
              - img [ref=e74]
            - button "0 Comments" [ref=e76] [cursor=pointer]:
              - img [ref=e77]
            - button "Share" [ref=e79] [cursor=pointer]:
              - img [ref=e80]
            - generic "1":
              - generic: "1"
              - img
```

# Test source

```ts
  1   | import { test, expect } from '@playwright/test';
  2   | 
  3   | const BASE_URL = 'http://localhost:5173';
  4   | const API_URL = 'http://localhost:8080';
  5   | 
  6   | test.describe('Login flow', () => {
  7   |   test.beforeEach(async ({ page }) => {
  8   |     await page.goto(BASE_URL);
  9   |   });
  10  | 
  11  |   test('shows login page when unauthenticated', async ({ page }) => {
  12  |     // The app should render some form of login UI when no session exists.
  13  |     await expect(page).toHaveURL(/\//);
  14  |     // At minimum a heading or login button must be visible.
  15  |     const loginHeading = page.getByRole('heading').first();
  16  |     await expect(loginHeading).toBeVisible();
  17  |   });
  18  | 
  19  |   test('login form renders username and password fields', async ({ page }) => {
  20  |     // Navigate to the login page (adjust selector to match your actual route).
  21  |     const loginLink = page.getByRole('link', { name: /sign in|log in|login/i }).first();
  22  |     if (await loginLink.isVisible()) {
  23  |       await loginLink.click();
  24  |     }
  25  | 
  26  |     const usernameField = page
  27  |       .getByRole('textbox', { name: /username|email/i })
  28  |       .or(page.locator('input[name="username"]'))
  29  |       .or(page.locator('input[type="email"]'))
  30  |       .first();
  31  | 
  32  |     const passwordField = page.locator('input[type="password"]').first();
  33  | 
> 34  |     await expect(usernameField).toBeVisible();
      |                                 ^ Error: expect(locator).toBeVisible() failed
  35  |     await expect(passwordField).toBeVisible();
  36  |   });
  37  | 
  38  |   test('shows validation error on empty submit', async ({ page }) => {
  39  |     const loginLink = page.getByRole('link', { name: /sign in|log in|login/i }).first();
  40  |     if (await loginLink.isVisible()) {
  41  |       await loginLink.click();
  42  |     }
  43  | 
  44  |     const submitBtn = page.getByRole('button', { name: /sign in|log in|login|continue/i }).first();
  45  | 
  46  |     if (await submitBtn.isVisible()) {
  47  |       await submitBtn.click();
  48  |       // Browser native validation or custom error message should appear.
  49  |       const errorOrInvalid =
  50  |         (await page.locator('[aria-invalid="true"]').count()) > 0 ||
  51  |         (await page.locator('[role="alert"]').count()) > 0;
  52  |       expect(errorOrInvalid).toBe(true);
  53  |     }
  54  |   });
  55  | 
  56  |   test('successful login redirects to feed', async ({ page }) => {
  57  |     // Mock the auth API to avoid needing a running backend.
  58  |     // Backend returns UserDTO directly (no token wrapper — session is in HttpOnly cookie).
  59  |     await page.route(`${API_URL}/api/auth/login`, async (route) => {
  60  |       await route.fulfill({
  61  |         status: 200,
  62  |         contentType: 'application/json',
  63  |         body: JSON.stringify({
  64  |           id: 1,
  65  |           username: 'testuser',
  66  |           name: 'Test User',
  67  |           bio: null,
  68  |           avatarUrl: null,
  69  |           createdAt: new Date().toISOString(),
  70  |         }),
  71  |       });
  72  |     });
  73  | 
  74  |     const loginLink = page.getByRole('link', { name: /sign in|log in|login/i }).first();
  75  |     if (await loginLink.isVisible()) {
  76  |       await loginLink.click();
  77  |     }
  78  | 
  79  |     const usernameField = page
  80  |       .getByRole('textbox', { name: /username|email/i })
  81  |       .or(page.locator('input[name="username"]'))
  82  |       .or(page.locator('input[type="email"]'))
  83  |       .first();
  84  |     const passwordField = page.locator('input[type="password"]').first();
  85  | 
  86  |     if (await usernameField.isVisible()) {
  87  |       await usernameField.fill('testuser');
  88  |       await passwordField.fill('password123');
  89  | 
  90  |       const submitBtn = page
  91  |         .getByRole('button', { name: /sign in|log in|login|continue/i })
  92  |         .first();
  93  |       await submitBtn.click();
  94  | 
  95  |       // After successful login, user should leave the login page.
  96  |       await page.waitForTimeout(500);
  97  |       const currentUrl = page.url();
  98  |       expect(currentUrl).not.toMatch(/login|sign-in/i);
  99  |     }
  100 |   });
  101 | 
  102 |   test('stores auth_user in localStorage after login (no token)', async ({ page }) => {
  103 |     // Cookie sessions: auth_user holds the cached user object for optimistic render.
  104 |     // auth_token must never appear — the session lives in an HttpOnly cookie only.
  105 |     await page.route(`${API_URL}/api/auth/login`, async (route) => {
  106 |       await route.fulfill({
  107 |         status: 200,
  108 |         contentType: 'application/json',
  109 |         body: JSON.stringify({
  110 |           user: {
  111 |             id: '1',
  112 |             username: 'testuser',
  113 |             name: 'Test User',
  114 |             email: 'test@example.com',
  115 |             bio: null,
  116 |             avatarUrl: null,
  117 |             followersCount: 0,
  118 |             followingCount: 0,
  119 |           },
  120 |         }),
  121 |       });
  122 |     });
  123 | 
  124 |     const loginLink = page.getByRole('link', { name: /sign in|log in|login/i }).first();
  125 |     if (await loginLink.isVisible()) {
  126 |       await loginLink.click();
  127 |     }
  128 | 
  129 |     const usernameField = page
  130 |       .getByRole('textbox', { name: /username|email/i })
  131 |       .or(page.locator('input[name="username"]'))
  132 |       .or(page.locator('input[type="email"]'))
  133 |       .first();
  134 |     const passwordField = page.locator('input[type="password"]').first();
```