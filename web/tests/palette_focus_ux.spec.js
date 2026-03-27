import { test, expect } from '@playwright/test';

test.describe('Palette UX Enhancements: Focus forwarding', () => {
  test('Password wrapper should forward focus to input on click', async ({ page }) => {
    await page.goto('/');

    const wrapper = page.locator('.password-input-wrapper');
    await expect(wrapper).toBeVisible();

    const input = page.locator('#login-password');
    await wrapper.click({ position: { x: 5, y: 5 } }); // Click top-left of wrapper (not on input directly)
    await expect(input).toBeFocused();
  });

  test('Command palette header should forward focus to input on click', async ({ page }) => {
    await page.goto('/?mock_user=true');

    // Open command palette (Ctrl+K or Cmd+K)
    await page.keyboard.press('ControlOrMeta+k');
    const paletteOverlay = page.locator('.command-palette-overlay');
    await expect(paletteOverlay).toBeVisible();

    const header = page.locator('.command-palette-header');
    await expect(header).toBeVisible();

    const input = page.locator('.command-palette-input');
    await expect(input).toBeFocused(); // Initial focus

    // Blur input
    await input.blur();
    await expect(input).not.toBeFocused();

    // Click on header
    await header.click({ position: { x: 5, y: 5 } });
    await expect(input).toBeFocused();
  });
});
