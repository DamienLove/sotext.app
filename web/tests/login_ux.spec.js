
import { test, expect } from '@playwright/test';

test.describe('Login UX Improvements', () => {
  test('Email input should be autofocused on load', async ({ page }) => {
    // Navigate to root (Login page)
    await page.goto('/');

    const emailInput = page.getByPlaceholder('you@example.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeFocused();
  });

  test('Password toggle button should have static title and use aria-pressed', async ({ page }) => {
    await page.goto('/');

    const toggleBtn = page.locator('.password-toggle-btn');
    await expect(toggleBtn).toBeVisible();

    // The title should be static
    await expect(toggleBtn).toHaveAttribute('title', 'Toggle password visibility');

    // Initially hidden (password type), aria-pressed should be 'false' or omitted/falsy
    // Playwright `toHaveAttribute` checks for exact match. Initially it's not toggled, so `aria-pressed="false"`.
    // Let's verify aria-pressed behavior
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'false');

    // Click to show
    await toggleBtn.click();
    await expect(toggleBtn).toHaveAttribute('title', 'Toggle password visibility');
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'true');
  });
});
