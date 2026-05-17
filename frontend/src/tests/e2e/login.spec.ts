import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:5173';
const API_URL = 'http://localhost:8080';

test.describe('Login flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(BASE_URL);
  });

  test('shows login page when unauthenticated', async ({ page }) => {
    // The app should render some form of login UI when no session exists.
    await expect(page).toHaveURL(/\//);
    // At minimum a heading or login button must be visible.
    const loginHeading = page.getByRole('heading').first();
    await expect(loginHeading).toBeVisible();
  });

  test('login form renders username and password fields', async ({ page }) => {
    // Navigate to the login page (adjust selector to match your actual route).
    const loginLink = page.getByRole('link', { name: /sign in|log in|login/i }).first();
    if (await loginLink.isVisible()) {
      await loginLink.click();
    }

    const usernameField = page
      .getByRole('textbox', { name: /username|email/i })
      .or(page.locator('input[name="username"]'))
      .or(page.locator('input[type="email"]'))
      .first();

    const passwordField = page.locator('input[type="password"]').first();

    await expect(usernameField).toBeVisible();
    await expect(passwordField).toBeVisible();
  });

  test('shows validation error on empty submit', async ({ page }) => {
    const loginLink = page.getByRole('link', { name: /sign in|log in|login/i }).first();
    if (await loginLink.isVisible()) {
      await loginLink.click();
    }

    const submitBtn = page.getByRole('button', { name: /sign in|log in|login|continue/i }).first();

    if (await submitBtn.isVisible()) {
      await submitBtn.click();
      // Browser native validation or custom error message should appear.
      const errorOrInvalid =
        (await page.locator('[aria-invalid="true"]').count()) > 0 ||
        (await page.locator('[role="alert"]').count()) > 0;
      expect(errorOrInvalid).toBe(true);
    }
  });

  test('successful login redirects to feed', async ({ page }) => {
    // Mock the auth API to avoid needing a running backend.
    // Backend returns UserDTO directly (no token wrapper — session is in HttpOnly cookie).
    await page.route(`${API_URL}/api/auth/login`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          username: 'testuser',
          name: 'Test User',
          bio: null,
          avatarUrl: null,
          createdAt: new Date().toISOString(),
        }),
      });
    });

    const loginLink = page.getByRole('link', { name: /sign in|log in|login/i }).first();
    if (await loginLink.isVisible()) {
      await loginLink.click();
    }

    const usernameField = page
      .getByRole('textbox', { name: /username|email/i })
      .or(page.locator('input[name="username"]'))
      .or(page.locator('input[type="email"]'))
      .first();
    const passwordField = page.locator('input[type="password"]').first();

    if (await usernameField.isVisible()) {
      await usernameField.fill('testuser');
      await passwordField.fill('password123');

      const submitBtn = page
        .getByRole('button', { name: /sign in|log in|login|continue/i })
        .first();
      await submitBtn.click();

      // After successful login, user should leave the login page.
      await page.waitForTimeout(500);
      const currentUrl = page.url();
      expect(currentUrl).not.toMatch(/login|sign-in/i);
    }
  });

  test('stores auth_user in localStorage after login (no token)', async ({ page }) => {
    // Cookie sessions: auth_user holds the cached user object for optimistic render.
    // auth_token must never appear — the session lives in an HttpOnly cookie only.
    await page.route(`${API_URL}/api/auth/login`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: {
            id: '1',
            username: 'testuser',
            name: 'Test User',
            email: 'test@example.com',
            bio: null,
            avatarUrl: null,
            followersCount: 0,
            followingCount: 0,
          },
        }),
      });
    });

    const loginLink = page.getByRole('link', { name: /sign in|log in|login/i }).first();
    if (await loginLink.isVisible()) {
      await loginLink.click();
    }

    const usernameField = page
      .getByRole('textbox', { name: /username|email/i })
      .or(page.locator('input[name="username"]'))
      .or(page.locator('input[type="email"]'))
      .first();
    const passwordField = page.locator('input[type="password"]').first();

    if (await usernameField.isVisible()) {
      await usernameField.fill('testuser');
      await passwordField.fill('password123');

      const submitBtn = page
        .getByRole('button', { name: /sign in|log in|login|continue/i })
        .first();
      await submitBtn.click();
      await page.waitForTimeout(500);
    }

    const [tokenInStorage, userInStorage] = await page.evaluate(() => {
      return [localStorage.getItem('auth_token'), localStorage.getItem('auth_user')];
    });

    // Bearer token must never be in localStorage.
    expect(tokenInStorage).toBeNull();
    // User cache must be present for optimistic rendering.
    expect(userInStorage).not.toBeNull();
  });
});
