import { test, expect } from '@playwright/test';

test.describe('Command Palette Focus', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    // Wait for the app to load
    await page.waitForSelector('.sidebar');

    // Navigate to Beacon panel to ensure the Command Palette button is visible in Sidebar
    // The nav item has title="SoText"
    await page.locator('.nav-item[title="SoText"]').click();
  });

  test('should restore focus to trigger button after closing with Escape', async ({ page }) => {
    // Locate the trigger button in the sidebar
    // Note: The button has aria-label="Open command palette (Ctrl+K)"
    const triggerButton = page.locator('button[aria-label="Open command palette (Ctrl+K)"]');

    // Ensure it's visible and click it
    await expect(triggerButton).toBeVisible();
    await triggerButton.click();

    // Verify Command Palette is open and input is focused
    const paletteInput = page.locator('.command-palette-input');
    await expect(paletteInput).toBeVisible();
    await expect(paletteInput).toBeFocused();

    // Press Escape to close
    await page.keyboard.press('Escape');

    // Verify Command Palette is closed
    await expect(paletteInput).not.toBeVisible();

    // Verify focus is restored to the trigger button
    await expect(triggerButton).toBeFocused();
  });
});
