
import { test, expect } from '@playwright/test';

test.describe('Theme Editor Color Inputs', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/?mock_user=true');
    // Navigate to Themes
    await page.locator('.nav-item[title="Themes"]').click();
  });

  test('Should show color picker and hex input for Primary Color', async ({ page }) => {
    // My component structure: <label> Text <div> <input type=color> <input type=text> </div> </label>
    const colorInput = page.getByLabel('Primary color color picker');
    const textInput = page.getByLabel('Primary color hex code');

    await expect(colorInput).toBeVisible();
    await expect(textInput).toBeVisible();

    // Check initial sync (mock user might have default values)
    const initialColor = await colorInput.inputValue();
    const initialText = await textInput.inputValue();
    expect(initialColor.toUpperCase()).toBe(initialText.toUpperCase());

    // Test interaction: Type in text input
    await textInput.fill('#FF0000');
    await expect(colorInput).toHaveValue('#ff0000');

    // Test interaction: Pick color (simulate value change)
    await colorInput.fill('#00ff00');
    // The text input should update to match
    await expect(textInput).toHaveValue('#00ff00');
  });
});
