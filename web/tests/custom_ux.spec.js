import { test, expect } from '@playwright/test';

test('Focus forwarding for password input wrapper', async ({ page }) => {
  await page.goto('/');

  const passwordInput = page.locator('#login-password');
  const container = page.locator('.password-input-wrapper');

  await container.click({ position: { x: 5, y: 5 } });
  await expect(passwordInput).toBeFocused();
});
