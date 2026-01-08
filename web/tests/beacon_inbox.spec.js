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

  test('should show authentication requirement in DevTools', async ({ page }) => {
    // Open DevTools
    await page.keyboard.press('Control+Shift+D');

    // Verify DevTools shows login status
    await expect(page.getByText('Please log in to use DevTools')).toBeVisible();

    // The button should be disabled when not logged in
    const populateButton = page.getByText('Populate Mock Data');
    await expect(populateButton).toBeVisible();
    await expect(populateButton).toBeDisabled();
  });
});
