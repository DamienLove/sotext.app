import pathlib
from google.oauth2 import service_account
from googleapiclient.discovery import build

REPO_ROOT = pathlib.Path(__file__).resolve().parent

def resolve_service_account_key():
    candidates = [
        REPO_ROOT / "secrets" / "service-account-key.json",
        pathlib.Path("C:/Projects/pulselink/secrets/service-account-key.json"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    raise SystemExit("service account key missing")

svc_path = resolve_service_account_key()
creds = service_account.Credentials.from_service_account_file(
    svc_path,
    scopes=['https://www.googleapis.com/auth/androidpublisher']
)
service = build('androidpublisher', 'v3', credentials=creds)

packages = ['com.free.pulselink', 'com.pulselink.pro']
version_code = 144

for package in packages:
    print(f"\n=== {package} ===")
    
    # Check all tracks
    for track_name in ['internal', 'beta', 'production']:
        track = service.edits().tracks().get(
            packageName=package,
            editId=service.edits().insert(body={}, packageName=package).execute()['id'],
            track=track_name
        ).execute()
        
        if 'releases' in track and track['releases']:
            latest = track['releases'][0]
            version_codes = latest.get('versionCodes', [])
            status = latest.get('status', 'unknown')
            print(f"  {track_name}: versions {version_codes}, status={status}")
        else:
            print(f"  {track_name}: no releases")

print("\n✓ Current status displayed above")
print("Note: v144 is already in all tracks (internal, beta, production)")
print("Production releases are automatically submitted for review when published.")
print("Check Play Console for review status: https://play.google.com/console")
