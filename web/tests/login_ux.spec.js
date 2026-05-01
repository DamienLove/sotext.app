
import { test, expect } from '@playwright/test';

test.describe('Login UX Improvements', () => {
  test('Email input should be autofocused on load', async ({ page }) => {
    // Navigate to root (Login page)
    await page.goto('/');

    const emailInput = page.getByPlaceholder('you@example.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeFocused();
  });

  test('Password toggle button should have tooltip title and correct aria attributes', async ({ page }) => {
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

  test('Clicking password wrapper should focus password input', async ({ page }) => {
    await page.goto('/');

    const wrapper = page.locator('.password-input-wrapper');
    const input = page.locator('#login-password');

    await expect(wrapper).toBeVisible();
    await expect(input).toBeVisible();

    // Click slightly to the left of the wrapper bounds, but inside the wrapper itself
    // so we don't click on the input or the toggle button directly
    await wrapper.click({ position: { x: 5, y: 5 } });

    await expect(input).toBeFocused();
  });
});
