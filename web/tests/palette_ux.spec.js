
import { test, expect } from '@playwright/test';

test.describe('Palette UX Enhancements', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    await page.waitForTimeout(1000);
  });

  test('Sidebar search should have shortcut hint', async ({ page }) => {
    // Navigate to Beacon panel where sidebar search is visible
    await page.locator('.nav-item[title="Beacon"]').click();

    const searchInput = page.locator('.sidebar-search-input-field');
    const hint = page.locator('.sidebar-search-wrapper .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Contacts search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Contacts"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search by name, phone, or email');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Themes search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Themes"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search by name or creator');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Settings search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Settings"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search settings');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Avatar should have accessible text contrast', async ({ page }) => {
    // Go to PulseLink (Profile) panel
    await page.locator('.nav-item[title="PulseLink"]').click();

    // Type "Alice" (Light Background -> Black Text)
    const nameInput = page.locator('label:has-text("Display name") input');
    await nameInput.fill('Alice');

    const avatar = page.locator('.profile-avatar-preview .profile-avatar-img');
    // We expect color to be black (rgb(0, 0, 0))
    await expect(avatar).toHaveCSS('color', 'rgb(0, 0, 0)');

    // Type "Bob" (Dark Background -> White Text)
    await nameInput.fill('Bob');
    // We expect color to be white (rgb(255, 255, 255))
    await expect(avatar).toHaveCSS('color', 'rgb(255, 255, 255)');
  });
});
