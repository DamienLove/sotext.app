from playwright.sync_api import sync_playwright

def verify_theme_publish(page):
    # 1. Arrange: Go to the app with mock user
    page.goto("http://localhost:5173/?mock_user=true")

    # Wait for loading to finish (sidebar to appear)
    page.wait_for_selector(".sidebar")

    # 2. Act: Navigate to Themes
    page.get_by_title("Themes").click()

    # 3. Fill out theme form (text only)
    page.get_by_placeholder("e.g., Aurora Drift").fill("Sentinel Test Theme")

    # 4. Click Publish
    page.get_by_text("Publish theme").click()

    # 5. Wait for feedback (likely failure due to no backend, but we capture the attempt state)
    # The button text changes to "Publishing..."
    page.wait_for_selector("button[aria-busy='true']")

    # Take screenshot
    page.screenshot(path="verification/theme_publish_attempt.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            verify_theme_publish(page)
        finally:
            browser.close()
