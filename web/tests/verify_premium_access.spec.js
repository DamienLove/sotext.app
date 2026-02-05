
import { test, expect } from '@playwright/test';

test.describe('Premium Web Access Verification', () => {
  test('should verify premium access to Beacon Inbox', async ({ page }) => {
    // 1. Visit the app with mock_user=true to simulate a premium user
    await page.goto('/?mock_user=true');

    // 2. Verify we are logged in as Test User
    // The home hero might say "Good morning, Test" or similar
    await expect(page.locator('.home-hero h2')).toContainText('Test');

    // 3. Navigate to Beacon Inbox via Sidebar
    await page.locator('button[title="Beacon"]').click();

    // 4. Verify Inbox loads and is NOT showing the Premium Required blocker
    // The blocker contains this specific text
    await expect(page.getByText('Upgrade to PulseLink Pro or Premium')).not.toBeVisible();

    // 5. Verify threads are visible (mock data has threads)
    // We expect at least one thread from the mock setup
    const threadItems = page.locator('.thread-item');
    await expect(threadItems.first()).toBeVisible();

    // 6. Select a thread and verify composer
    await threadItems.first().click();
    await expect(page.locator('.composer')).toBeVisible();
    await expect(page.locator('textarea[placeholder*="Type a message"]')).toBeVisible();

    // 7. Navigate to Settings
    await page.locator('button[title="Settings"]').click();
    await expect(page.locator('h3').filter({ hasText: 'Settings' })).toBeVisible();

    // 8. Verify "Enable remote web access" is checked
    // Note: The label text in App.jsx is "Enable remote web access"
    const remoteAccessCheckbox = page.getByLabel('Enable remote web access');
    await expect(remoteAccessCheckbox).toBeChecked();
  });
});
