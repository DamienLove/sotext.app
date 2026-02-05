import { test, expect } from '@playwright/test';

test.describe('Extensions and Feature Toggles', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate with mock user to bypass login
    await page.goto('/?mock_user=true');
    // Wait for app to load
    await expect(page.locator('.sidebar')).toBeVisible();
  });

  test('Beacon Inbox toggle should show/hide sidebar item', async ({ page }) => {
    // Enable
    await page.evaluate(() => {
        window.debugSetRemoteSettings(prev => ({ ...prev, beaconLauncherEnabled: true }));
    });
    await expect(page.locator('.nav-item[title="Beacon"]')).toBeVisible();

    // Disable
    await page.evaluate(() => {
        window.debugSetRemoteSettings(prev => ({ ...prev, beaconLauncherEnabled: false }));
    });
    await expect(page.locator('.nav-item[title="Beacon"]')).toBeHidden();
  });

  test('PulseLink toggle should show/hide sidebar item', async ({ page }) => {
    // Enable
    await page.evaluate(() => {
        window.debugSetRemoteSettings(prev => ({ ...prev, pulseLinkEnabled: true }));
    });
    await expect(page.locator('.nav-item[title="PulseLink"]')).toBeVisible();

    // Disable
    await page.evaluate(() => {
        window.debugSetRemoteSettings(prev => ({ ...prev, pulseLinkEnabled: false }));
    });
    await expect(page.locator('.nav-item[title="PulseLink"]')).toBeHidden();
  });

  test('Command Palette toggle should enable/disable hotkey', async ({ page }) => {
    // Enable
    await page.evaluate(() => {
        window.debugSetRemoteSettings(prev => ({ ...prev, commandPaletteEnabled: true }));
    });

    // Press Ctrl+K
    await page.keyboard.press('Control+k');
    await expect(page.locator('.command-palette-overlay')).toBeVisible();

    // Ensure input is focused so Escape works
    await expect(page.locator('.command-palette-input')).toBeFocused();

    // Close it
    await page.keyboard.press('Escape');
    await expect(page.locator('.command-palette-overlay')).toBeHidden();

    // Disable
    await page.evaluate(() => {
        window.debugSetRemoteSettings(prev => ({ ...prev, commandPaletteEnabled: false }));
    });

    // Give React time to process effect
    await page.waitForTimeout(200);

    // Press Ctrl+K again
    await page.keyboard.press('Control+k');
    // Should NOT be visible
    await expect(page.locator('.command-palette-overlay')).toBeHidden();
  });
});
