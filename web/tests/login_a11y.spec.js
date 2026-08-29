import { test, expect } from '@playwright/test';

test('Login Screen accessibility & focus', async ({ page }) => {
  await page.goto('/');

  const passwordWrapper = page.locator('.password-input-wrapper');
  const passwordInput = page.locator('#login-password');
  const toggleBtn = page.locator('.password-toggle-btn');

  // Verify elements are visible
  await expect(passwordWrapper).toBeVisible();
  await expect(passwordInput).toBeVisible();
  await expect(toggleBtn).toBeVisible();

  // Test click on wrapper focuses input
  await passwordWrapper.click({ position: { x: 5, y: 5 } }); // Click empty space in wrapper
  await expect(passwordInput).toBeFocused();

  // Test ARIA attribute toggle
  await expect(toggleBtn).toHaveAttribute('aria-pressed', 'false');
  await toggleBtn.click();
  await expect(toggleBtn).toHaveAttribute('aria-pressed', 'true');
  await expect(passwordInput).toHaveAttribute('type', 'text');
});
