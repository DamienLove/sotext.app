/* eslint-env node */
import { test, expect } from '@playwright/test';

test.describe('Premium Access Gate (Free)', () => {
  test.beforeEach(async ({ page }) => {
    // 1. Visit the app with mock user and free tier
    await page.goto('/?mock_user=true&mock_tier=free');
  });

  test('should show upgrade message in Beacon Inbox', async ({ page }) => {
    // Navigate to Beacon Inbox
    const navItem = page.locator('.nav-item[title="Beacon"]');
    if (await navItem.isVisible()) {
        await navItem.click();
    } else {
        await page.getByRole('button', { name: 'Beacon Inbox' }).first().click();
    }

    // Verify upgrade prompt
    await expect(page.getByText('Premium Feature')).toBeVisible();
    await expect(page.getByText('Upgrade to PulseLink Pro or Premium')).toBeVisible();

    // Verify message list is NOT visible
    await expect(page.locator('.messages-list')).not.toBeVisible();
    await expect(page.getByPlaceholder('Type a message...')).not.toBeVisible();
  });
});
