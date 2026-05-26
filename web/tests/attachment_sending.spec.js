/* eslint-env node */
import { test, expect } from '@playwright/test';

test.use({
  geolocation: { longitude: -98.35, latitude: 39.5 },
  permissions: ['geolocation'],
  viewport: { width: 1280, height: 720 },
});

test.beforeEach(async ({ page }) => {
  // Navigate to app with mock user
  await page.goto('http://localhost:5173/?mock_user=true');
  await page.waitForTimeout(2000);
});

test('Web Message Composer shows paperclip and handles file selection', async ({ page }) => {
  // Navigate to Beacon Inbox
  await page.click('button[title="Beacon"]');
  await page.waitForTimeout(1000);

  // Select a thread (mocked)
  // Ensure we are in a thread view
  const thread = page.locator('.thread-item').first();
  await expect(thread).toBeVisible();
  await thread.click();

  // Check for Paperclip icon
  const paperclip = page.locator('button[aria-label="Attach image"]');
  await expect(paperclip).toBeVisible();

  // Mock file input and selection
  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles({
    name: 'test.png',
    mimeType: 'image/png',
    buffer: Buffer.from('this is a test image')
  });

  // Verify UI feedback - Placeholder changes when attachment is present
  const textarea = page.locator('.composer-textarea');
  await expect(textarea).toHaveAttribute('placeholder', 'Add a caption...');

  // Verify Paperclip active state
  await expect(paperclip).toHaveClass(/active/);
});
