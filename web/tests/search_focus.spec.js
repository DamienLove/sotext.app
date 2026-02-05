// @ts-check
import { test, expect } from '@playwright/test';

test.describe('Search Interaction UX', () => {
  test.beforeEach(async ({ page }) => {
    // Start with mock user to bypass login
    await page.goto('/?mock_user=true');
    // Wait for the app to load
    await expect(page.locator('.sidebar')).toBeVisible();
  });

  test('Sidebar search should focus on container click and clear/blur on Escape', async ({ page }) => {
    // Navigate to Beacon
    await page.click('button[title="Beacon"]');
    await expect(page.locator('.sidebar-search-wrapper')).toBeVisible();

    const searchWrapper = page.locator('.sidebar-search-wrapper');
    const searchInput = page.locator('.sidebar-search-input-field');

    // Click the container (wrapper), not the input directly
    // To ensure we click the wrapper, we can click the edge or the icon area
    await searchWrapper.click({ position: { x: 5, y: 5 } });
    await expect(searchInput).toBeFocused();

    // Type something
    await searchInput.fill('Test Query');
    await expect(searchInput).toHaveValue('Test Query');

    // Press Escape to clear
    await searchInput.press('Escape');
    await expect(searchInput).toHaveValue('');
    // Should still be focused after clear (based on App.jsx logic: if (searchQuery) setSearchQuery('') else blur())
    await expect(searchInput).toBeFocused();

    // Press Escape again to blur
    await searchInput.press('Escape');
    await expect(searchInput).not.toBeFocused();
  });

  test('Contacts search should focus on container click and clear/blur on Escape', async ({ page }) => {
    // Navigate to Contacts
    await page.click('button[title="Contacts"]');
    await expect(page.locator('.contacts-panel')).toBeVisible();

    const searchContainer = page.locator('.contacts-panel .settings-search-container');
    const searchInput = page.locator('.contacts-panel .settings-search-input');

    // Click the container
    await searchContainer.click({ position: { x: 5, y: 5 } });
    await expect(searchInput).toBeFocused();

    // Type something
    await searchInput.fill('Mom');
    await expect(searchInput).toHaveValue('Mom');

    // Press Escape to clear
    await searchInput.press('Escape');
    await expect(searchInput).toHaveValue('');
    await expect(searchInput).toBeFocused();

    // Press Escape again to blur
    await searchInput.press('Escape');
    await expect(searchInput).not.toBeFocused();
  });

  test('Themes search should focus on container click and clear/blur on Escape', async ({ page }) => {
    // Navigate to Themes
    await page.click('button[title="Themes"]');
    await expect(page.locator('.themes-panel')).toBeVisible();

    const searchContainer = page.locator('.themes-panel .settings-search-container');
    const searchInput = page.locator('.themes-panel .settings-search-input');

    // Click the container
    await searchContainer.click({ position: { x: 5, y: 5 } });
    await expect(searchInput).toBeFocused();

    // Type something
    await searchInput.fill('Dark');
    await expect(searchInput).toHaveValue('Dark');

    // Press Escape to clear
    await searchInput.press('Escape');
    await expect(searchInput).toHaveValue('');
    await expect(searchInput).toBeFocused();

    // Press Escape again to blur
    await searchInput.press('Escape');
    await expect(searchInput).not.toBeFocused();
  });

  test('Settings search should focus on container click and clear/blur on Escape', async ({ page }) => {
    // Navigate to Settings
    await page.click('button[title="Settings"]');
    await expect(page.locator('.settings-panel')).toBeVisible();

    const searchContainer = page.locator('.settings-panel .settings-search-container');
    const searchInput = page.locator('.settings-panel .settings-search-input');

    // Click the container
    await searchContainer.click({ position: { x: 5, y: 5 } });
    await expect(searchInput).toBeFocused();

    // Type something
    await searchInput.fill('Account');
    await expect(searchInput).toHaveValue('Account');

    // Press Escape to clear
    await searchInput.press('Escape');
    await expect(searchInput).toHaveValue('');
    await expect(searchInput).toBeFocused();

    // Press Escape again to blur
    await searchInput.press('Escape');
    await expect(searchInput).not.toBeFocused();
  });
});
