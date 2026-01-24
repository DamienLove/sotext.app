
import os
from playwright.sync_api import sync_playwright, expect

def verify_ui():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Navigate to the app (Vite port 5174 as 5173 was busy)
        try:
            page.goto("http://localhost:5174", timeout=10000)
        except Exception as e:
            print(f"Failed to load page: {e}")
            return

        # Check for the login card
        expect(page.locator(".login-card")).to_be_visible()

        # Take a screenshot of the login page
        os.makedirs("/home/jules/verification", exist_ok=True)
        page.screenshot(path="/home/jules/verification/login_page.png")
        print("Screenshot taken: /home/jules/verification/login_page.png")

        browser.close()

if __name__ == "__main__":
    verify_ui()
