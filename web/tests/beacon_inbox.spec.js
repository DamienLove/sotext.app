// web/tests/beacon_inbox.spec.js
/* eslint-env node */
import { test, expect } from '@playwright/test';

test.describe('Beacon Inbox', () => {
  test.beforeEach(async ({ page }) => {
    // 1. Visit the app
    await page.goto('/');
  });

  test('should display login screen and dev tools toggle', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('PulseLink Web');
    await expect(page.getByPlaceholder('you@example.com')).toBeVisible();

    // Simulate Ctrl+Shift+D to open DevTools
    await page.keyboard.press('Control+Shift+D');
    await expect(page.getByText('Dev Tools')).toBeVisible();
    await expect(page.getByText('Populate Mock Data')).toBeVisible();
  });

  test('should toggle DevTools visibility with Ctrl+Shift+D', async ({ page }) => {
    // Initial state: hidden
    await expect(page.getByText('Dev Tools')).not.toBeVisible();

    // Open
    await page.keyboard.press('Control+Shift+D');
    await expect(page.getByText('Dev Tools')).toBeVisible();

    // Close via shortcut
    await page.keyboard.press('Control+Shift+D');
    await expect(page.getByText('Dev Tools')).not.toBeVisible();
  });

  test('should handle mock data population attempt', async ({ page }) => {
    // Open DevTools
    await page.keyboard.press('Control+Shift+D');

    // Ensure the input is visible and has default value
    const input = page.getByLabel('Target User ID');
    await expect(input).toBeVisible();
    await expect(input).toHaveValue('test_user_123');

    // Click populate (This might fail if Firestore isn't reachable, but we verify the attempt)
    await page.getByText('Populate Mock Data').click();

    // Check for status message (Success or Error)
    // We expect either "Success!" or "Error:" depending on the environment connectivity
    // This assertion just ensures the button triggered a state change
    await expect(page.locator('[role="alert"]')).toBeVisible();
  });
});
