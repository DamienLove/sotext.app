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
});
