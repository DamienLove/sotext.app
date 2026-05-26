import { test, expect } from '@playwright/test';

test('Password toggle button should have aria-pressed or aria-expanded', async ({ page }) => {
  await page.goto('/');

  const toggleBtn = page.locator('.password-toggle-btn');
  await expect(toggleBtn).toBeVisible();

  // Test the required accessibility attribute based on memory:
  // "For accessibility, state toggle buttons (e.g., 'Show/Hide Password') should use a static aria-label (e.g., 'Toggle password visibility') combined with the aria-pressed or aria-expanded attribute to properly indicate their active toggled state to screen readers, rather than dynamically changing the aria-label based on state."

  // Should have static aria-label
  await expect(toggleBtn).toHaveAttribute('aria-label', 'Toggle password visibility');

  // Should initially have aria-pressed="false"
  await expect(toggleBtn).toHaveAttribute('aria-pressed', 'false');

  // Click to show password
  await toggleBtn.click();

  // Should now have aria-pressed="true"
  await expect(toggleBtn).toHaveAttribute('aria-pressed', 'true');
});
