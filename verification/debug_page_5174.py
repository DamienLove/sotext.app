from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("http://localhost:5174")
    page.screenshot(path="verification/debug_home_5174.png")
    print(page.content())
    browser.close()

with sync_playwright() as playwright:
    run(playwright)
