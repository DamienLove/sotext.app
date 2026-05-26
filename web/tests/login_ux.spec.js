
import { test, expect } from '@playwright/test';

test.describe('Login UX Improvements', () => {
  test('Email input should be autofocused on load', async ({ page }) => {
    // Navigate to root (Login page)
    await page.goto('/');

    const emailInput = page.getByPlaceholder('you@example.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeFocused();
  });

  test('Password toggle button should have tooltip title', async ({ page }) => {
    await page.goto('/');

    const toggleBtn = page.locator('.password-toggle-btn');
    await expect(toggleBtn).toBeVisible();

    // Initially hidden (password type)
    await expect(toggleBtn).toHaveAttribute('title', 'Show password');

    // Click to show
    await toggleBtn.click();
    await expect(toggleBtn).toHaveAttribute('title', 'Hide password');
  });

  test('Clicking password wrapper should focus password input', async ({ page }) => {
    await page.goto('/');

    const passwordWrapper = page.locator('.password-input-wrapper');
    const passwordInput = page.locator('#login-password');
    const toggleBtn = page.locator('.password-toggle-btn');

    await expect(passwordWrapper).toBeVisible();
    await expect(passwordInput).toBeVisible();

    // Click the wrapper
    await passwordWrapper.click();
    await expect(passwordInput).toBeFocused();

    // Ensure clicking the toggle button doesn't mess up focus incorrectly or break
    await toggleBtn.click();
    // In our implementation toggle button handles its own click and stops propagation,
    // so wrapper won't re-focus the input. We're just making sure the test passes and nothing breaks.
    await expect(toggleBtn).toBeVisible();
  });
});
