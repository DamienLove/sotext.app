
import { test, expect } from '@playwright/test';

test.describe('Toast Notification UX', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    await page.waitForTimeout(1000);
  });

  test('should show toast with dismiss button when saving a contact', async ({ page }) => {
    // Navigate to PulseLink panel
    await page.locator('.nav-item[title="PulseLink"]').click();
    await expect(page.getByRole('heading', { name: 'PulseLink' })).toBeVisible();

    // Scope to the card "Add trusted contact"
    const addCard = page.locator('.settings-card').filter({ hasText: 'Add trusted contact' });

    // Fill contact form
    await addCard.getByLabel('Name').fill('Toast Test User');

    // Click Add contact
    await addCard.getByRole('button', { name: 'Add contact' }).click();

    // Verify toast appears in the Add Card
    const toast = addCard.locator('.toast');
    await expect(toast).toBeVisible();
    await expect(toast).toContainText('Contact saved.');

    // Verify dismiss button exists
    const dismissBtn = toast.locator('button[aria-label="Dismiss"]');
    await expect(dismissBtn).toBeVisible();

    // Click dismiss
    await dismissBtn.click();

    // Verify toast disappears
    await expect(toast).toBeHidden();
  });
});
