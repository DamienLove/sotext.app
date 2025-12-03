# How to Set Up Donation Options

This guide helps you enable GitHub Sponsors, set up Ko‑fi and PayPal donation links, and wire the badges that already exist in the main `README.md`.

## 1) GitHub Sponsors

- Enable: GitHub → Your repository → Settings → “Sponsorships” → Set up Sponsors.
- Optional but recommended: add `.github/FUNDING.yml` to advertise links across GitHub.

Example `FUNDING.yml`:

```yml
github: damienlove
custom: [
  "https://ko-fi.com/YOUR_USERNAME",
  "https://www.paypal.com/donate?hosted_button_id=YOUR_BUTTON_ID"
]
```

- In `README.md`, replace `YOUR_USERNAME` in the Sponsors badge link with your GitHub username.

## 2) Ko‑fi

- Create your page at https://ko-fi.com/ and copy your profile URL (e.g. `https://ko-fi.com/damienlove`).
- In `README.md`, replace `YOUR_USERNAME` in the Ko‑fi badge/link.

## 3) PayPal Donate Button

- Log into PayPal → Tools → Pay & Get Paid → PayPal buttons → “Donate button” (or “Fundraisers”).
- Create a hosted donate button; copy the `hosted_button_id` from the generated link.
- In `README.md`, replace `YOUR_BUTTON_ID` in the PayPal link.

## 4) Badges (already present in README)

These use shields.io. Once you update the links, the badges will work immediately:

```md
[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-❤_GitHub_Sponsors-ea4aaa?logo=github)](https://github.com/sponsors/YOUR_USERNAME)
[![Ko‑fi](https://img.shields.io/badge/Buy_me_a_coffee-Ko%E2%80%91fi-29abe0?logo=kofi)](https://ko-fi.com/YOUR_USERNAME)
[![PayPal](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal)](https://www.paypal.com/donate?hosted_button_id=YOUR_BUTTON_ID)
```

## Placeholders to replace now

- `YOUR_USERNAME` → your GitHub/Ko‑fi handle.
- `YOUR_BUTTON_ID` → the PayPal hosted button ID.

## Quick checklist

- [ ] Enable GitHub Sponsors.
- [ ] Add `.github/FUNDING.yml`.
- [ ] Create Ko‑fi page and plug URL into the badge.
- [ ] Create PayPal Donate button and plug `hosted_button_id`.
- [ ] Commit changes to `main`.
