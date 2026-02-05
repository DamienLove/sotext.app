
import { test, expect } from '@playwright/test';

test.describe('Palette UX: Contacts Copy', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    await page.waitForTimeout(1000);
  });

  test('Contacts should have a copy button', async ({ page }) => {
    // Navigate to Contacts panel
    await page.locator('.nav-item[title="Contacts"]').click();

    // Wait for contacts to load (mock data usually immediate, but good to wait)
    await page.waitForSelector('.contact-row');

    // Get the first contact row
    const firstContact = page.locator('.contact-row').first();

    // Check if there is a copy button
    // The label should be dynamic, e.g., "Copy +15553334444"
    const copyBtn = firstContact.locator('button[aria-label^="Copy"]');

    await expect(copyBtn).toBeVisible();
  });
});
