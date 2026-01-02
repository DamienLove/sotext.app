from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("http://localhost:5174")

    # Wait for app to load - check for sidebar
    expect(page.get_by_text("PulseLink Suite")).to_be_visible(timeout=30000)

    # Expect RingerSong panel button and click it
    page.get_by_title("RingerSong").click()

    # Search for song
    page.get_by_label("Search Spotify for songs").fill("Test")
    page.get_by_role("button", name="Search").click()

    # Wait for results
    expect(page.get_by_text("Test Song 1")).to_be_visible()

    # Click Add and expect Loading state
    add_btn = page.get_by_label("Add Test Song 1 to playlist")
    add_btn.click()

    # Expect busy and text change
    expect(add_btn).to_have_attribute("aria-busy", "true")
    expect(add_btn).to_have_text("Adding...")
    expect(add_btn).to_be_disabled()

    page.screenshot(path="verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)
