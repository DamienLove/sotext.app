
import { test, expect } from '@playwright/test';

test.describe('Login UX Improvements', () => {
  test('Email input should be autofocused on load', async ({ page }) => {
    // Navigate to root (Login page)
    await page.goto('/');

    const emailInput = page.getByPlaceholder('you@example.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeFocused();
  });

  test('Password toggle button should have static aria-label and dynamic title/aria-pressed', async ({ page }) => {
    await page.goto('/');

    const toggleBtn = page.locator('.password-toggle-btn');
    await expect(toggleBtn).toBeVisible();

    // Check static aria-label
    await expect(toggleBtn).toHaveAttribute('aria-label', 'Toggle password visibility');

    // Initially hidden (password type)
    await expect(toggleBtn).toHaveAttribute('title', 'Show password');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'false');

    // Click to show
    await toggleBtn.click();
    await expect(toggleBtn).toHaveAttribute('title', 'Hide password');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'true');
  });

  test('Password input wrapper should forward focus to input', async ({ page }) => {
    await page.goto('/');

    const wrapper = page.locator('.password-input-wrapper');
    const input = page.locator('#login-password');

    // Click the wrapper
    await wrapper.click();

    // The input should be focused
    await expect(input).toBeFocused();
  });
});
