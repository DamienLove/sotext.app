# Regenerating the Docs Site and GitHub Wiki

This repository auto‑publishes documentation from the `docs/` folder using GitHub Pages.

## Docs site (GitHub Pages)

- Workflow: `.github/workflows/deploy-pages.yml`
- Source: `docs/`
- Published URL: `https://damienlove.github.io/sotext.app/`

### How to publish/refresh

1. Commit any change under `docs/**` to the `main` branch.
2. GitHub Actions will automatically build and deploy the site.
3. Check progress at GitHub → Actions → "Deploy GitHub Pages".

No manual steps are required beyond committing to `main`.

## Optional: Mirror content to the GitHub Wiki

If you also want to use the separate GitHub Wiki (a distinct repo), you can mirror or copy content from `docs/` so both stay in sync.

### Clone the wiki repository

```bash
git clone https://github.com/DamienLove/sotext.app.wiki.git
cd sotext.app.wiki
```

### Copy content from `docs/` into the wiki

Windows (PowerShell):

```powershell
Copy-Item -Recurse -Force ..\docs\* .\
```

macOS/Linux:

```bash
rsync -av --delete ../docs/ ./
# or: cp -R ../docs/* ./
```

### Commit and push to the wiki

```bash
git add .
git commit -m "Sync docs into wiki"
git push origin main
```

### Tips to avoid drift

- Keep detailed, canonical documents in `docs/`.
- Use the wiki for concise summaries and quick links back to the full docs.
- When updating docs, re‑sync the wiki (or link directly to the Pages site) so both remain consistent.
