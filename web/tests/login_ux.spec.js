
import { test, expect } from '@playwright/test';

test.describe('Login UX Improvements', () => {
  test('Email input should be autofocused on load', async ({ page }) => {
    // Navigate to root (Login page)
    await page.goto('/');

    const emailInput = page.getByPlaceholder('you@example.com');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toBeFocused();
  });

  test('Password toggle button should have static aria-label and dynamic aria-pressed', async ({ page }) => {
    await page.goto('/');

    const toggleBtn = page.locator('.password-toggle-btn');
    await expect(toggleBtn).toBeVisible();

    // Static aria-label
    await expect(toggleBtn).toHaveAttribute('aria-label', 'Toggle password visibility');

    // Initially hidden (password type)
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'false');
    await expect(toggleBtn).toHaveAttribute('title', 'Show password');

    // Click to show
    await toggleBtn.click();
    await expect(toggleBtn).toHaveAttribute('aria-pressed', 'true');
    await expect(toggleBtn).toHaveAttribute('title', 'Hide password');
  });

  test('Clicking password wrapper should focus password input', async ({ page }) => {
    await page.goto('/');

    const wrapper = page.locator('.password-input-wrapper');
    const input = page.locator('#login-password');
    const toggleBtn = page.locator('.password-toggle-btn');

    await expect(wrapper).toBeVisible();
    await expect(input).toBeVisible();

    // Click outside first
    await page.locator('body').click();
    await expect(input).not.toBeFocused();

    // Click wrapper space
    await wrapper.click();
    await expect(input).toBeFocused();

    // Click toggle button should not trigger focus on input (propagation stopped)
    await input.blur();
    await expect(input).not.toBeFocused();
    await toggleBtn.click();
    // After clicking toggle button, it should have focus or body, but not input
    await expect(input).not.toBeFocused();
  });
});
