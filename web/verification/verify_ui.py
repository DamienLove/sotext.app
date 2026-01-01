from playwright.sync_api import sync_playwright
import time

def verify_ui():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        # Use a larger viewport to see the sidebar and content clearly
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        # Navigate to the local dev server
        # Vite usually runs on 5173
        try:
            page.goto("http://localhost:5173", timeout=10000)
        except Exception as e:
            print(f"Failed to load page: {e}")
            # Try waiting a bit more for Vite to start
            time.sleep(5)
            page.goto("http://localhost:5173", timeout=10000)

        # Wait for the app shell to load
        page.wait_for_selector(".app-shell")

        # Screenshot the Login Screen (Initial State)
        page.screenshot(path="web/verification/1_login.png")
        print("Login screenshot captured.")

        # NOTE: We can't easily log in without a real user, but we can verify the login UI styling.
        # However, we can use AppWithMocks.jsx if we want to see the main UI.
        # But AppWithMocks might not be mounted.
        # The prompt mentioned AppWithMocks.jsx mirrors App.jsx but with mocks.
        # We can try to modify main.jsx temporarily to render AppWithMocks, OR
        # just verify the Login UI which was also heavily styled.

        # Let's verify the Login UI details
        # Check if the glass card is visible
        if page.is_visible(".login-card"):
            print("Login card is visible.")

        browser.close()

if __name__ == "__main__":
    verify_ui()
