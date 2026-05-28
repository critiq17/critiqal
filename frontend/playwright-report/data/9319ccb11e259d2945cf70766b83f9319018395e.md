# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: login.spec.ts >> Login flow >> stores auth_user in localStorage after login (no token)
- Location: src/tests/e2e/login.spec.ts:102:3

# Error details

```
Error: expect(received).not.toBeNull()

Received: null
```

# Test source

```ts
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
  135 | 
  136 |     if (await usernameField.isVisible()) {
  137 |       await usernameField.fill('testuser');
  138 |       await passwordField.fill('password123');
  139 | 
  140 |       const submitBtn = page
  141 |         .getByRole('button', { name: /sign in|log in|login|continue/i })
  142 |         .first();
  143 |       await submitBtn.click();
  144 |       await page.waitForTimeout(500);
  145 |     }
  146 | 
  147 |     const [tokenInStorage, userInStorage] = await page.evaluate(() => {
  148 |       return [localStorage.getItem('auth_token'), localStorage.getItem('auth_user')];
  149 |     });
  150 | 
  151 |     // Bearer token must never be in localStorage.
  152 |     expect(tokenInStorage).toBeNull();
  153 |     // User cache must be present for optimistic rendering.
> 154 |     expect(userInStorage).not.toBeNull();
      |                               ^ Error: expect(received).not.toBeNull()
  155 |   });
  156 | });
  157 | 
```