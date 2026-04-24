
import { test, expect } from '@playwright/test';

test.describe('Login UX Improvements', () => {
  test('Email input should be autofocused on load', async ({ page }) => {
    // Navigate to root (Login page)
    await page.goto('/');

    const emailInput = page.getByPlaceholder('you@example.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeFocused();
  });

  test('Password toggle button should have static tooltip and aria-pressed attributes', async ({ page }) => {
    await page.goto('/');

    const toggleBtn = page.locator('.password-toggle-btn');
    await expect(toggleBtn).toBeVisible();

    // Initially hidden (password type)
    await expect(toggleBtn).toHaveAttribute('title', 'Toggle password visibility');
    await expect(toggleBtn).toHaveAttribute('aria-label', 'Toggle password visibility');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'false');

    // Click to show
    await toggleBtn.click();
    await expect(toggleBtn).toHaveAttribute('title', 'Toggle password visibility');
    await expect(toggleBtn).toHaveAttribute('aria-label', 'Toggle password visibility');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'true');
  });

  test('Password wrapper should forward focus to input', async ({ page }) => {
    await page.goto('/');

    const wrapper = page.locator('.password-input-wrapper');
    const passwordInput = page.locator('#login-password');

    await wrapper.click();
    await expect(passwordInput).toBeFocused();
  });
});
