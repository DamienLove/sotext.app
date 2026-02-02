/* eslint-env node */
import { test, expect } from '@playwright/test';

test.describe('Beacon Inbox (Premium)', () => {
  test.beforeEach(async ({ page }) => {
    // 1. Visit the app with mock user and premium tier
    await page.goto('/?mock_user=true&mock_tier=premium');
  });

  test('should display inbox and threads for premium user', async ({ page }) => {
    // Verify Sidebar exists
    await expect(page.locator('.sidebar')).toBeVisible();

    // Navigate to Beacon Inbox
    // Try sidebar nav item or home tile
    const navItem = page.locator('.nav-item[title="Beacon"]');
    if (await navItem.isVisible()) {
        await navItem.click();
    } else {
        await page.getByRole('button', { name: 'Beacon Inbox' }).first().click();
    }

    // Verify thread list
    await expect(page.locator('.thread-list')).toBeVisible();
    await expect(page.getByText('Test Contact')).toBeVisible();
    await expect(page.getByText('Hello World')).toBeVisible(); // Snippet
  });

  test('should allow message composition', async ({ page }) => {
    const navItem = page.locator('.nav-item[title="Beacon"]');
    if (await navItem.isVisible()) {
        await navItem.click();
    } else {
        await page.getByRole('button', { name: 'Beacon Inbox' }).first().click();
    }

    // Select the thread
    await page.getByText('Test Contact').click();

    // Check header
    await expect(page.locator('.chat-header')).toContainText('+15559998888');

    // Check composer
    await expect(page.getByPlaceholder('Type a message...')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Send' })).toBeVisible();
  });
});
