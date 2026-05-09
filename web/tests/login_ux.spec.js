
import { test, expect } from '@playwright/test';

test.describe('Login UX Improvements', () => {
  test('Email input should be autofocused on load', async ({ page }) => {
    // Navigate to root (Login page)
    await page.goto('/');

    const emailInput = page.getByPlaceholder('you@example.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeFocused();
  });

  test('Password toggle button should have tooltip title and correct accessibility attributes', async ({ page }) => {
    await page.goto('/');

    const toggleBtn = page.locator('.password-toggle-btn');
    await expect(toggleBtn).toBeVisible();

    // Verify static aria-label
    await expect(toggleBtn).toHaveAttribute('aria-label', 'Toggle password visibility');

    // Initially hidden (password type)
    await expect(toggleBtn).toHaveAttribute('title', 'Show password');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'false');

    // Click to show
    await toggleBtn.click();
    await expect(toggleBtn).toHaveAttribute('title', 'Hide password');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'true');
  });

  test('Clicking password input wrapper forwards focus to password input', async ({ page }) => {
    await page.goto('/');

    const wrapper = page.locator('.password-input-wrapper');
    const passwordInput = page.locator('#login-password');

    await expect(wrapper).toBeVisible();
    await wrapper.click();

    await expect(passwordInput).toBeFocused();
  });
});
