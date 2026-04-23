
import { test, expect } from '@playwright/test';

test.describe('Login UX Improvements', () => {
  test('Email input should be autofocused on load', async ({ page }) => {
    // Navigate to root (Login page)
    await page.goto('/');

    const emailInput = page.getByPlaceholder('you@example.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeFocused();
  });

  test('Password toggle button should have tooltip title and accessible state', async ({ page }) => {
    await page.goto('/');

    const toggleBtn = page.locator('.password-toggle-btn');
    await expect(toggleBtn).toBeVisible();

    // Initially hidden (password type)
    await expect(toggleBtn).toHaveAttribute('title', 'Show password');
    await expect(toggleBtn).toHaveAttribute('aria-label', 'Toggle password visibility');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'false');

    // Click to show
    await toggleBtn.click();
    await expect(toggleBtn).toHaveAttribute('title', 'Hide password');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'true');
  });

  test('Password wrapper should forward focus to input on click', async ({ page }) => {
    await page.goto('/');

    const passwordInput = page.locator('#login-password');
    const passwordWrapper = page.locator('.password-input-wrapper');

    await expect(passwordInput).toBeVisible();

    // Click on the wrapper
    await passwordWrapper.click({ position: { x: 5, y: 5 } });

    // Ensure input gets focus
    await expect(passwordInput).toBeFocused();
  });
});
