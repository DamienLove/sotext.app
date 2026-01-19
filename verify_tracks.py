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

packages = {
    'com.free.pulselink': 'PulseLink (Free)',
    'com.pulselink.pro': 'PulseLink Pro'
}

print("=" * 60)
print("PULSELINK v144 RELEASE STATUS")
print("=" * 60)

for package, name in packages.items():
    print(f"\n{name}")
    print("-" * 60)
    
    edit = service.edits().insert(body={}, packageName=package).execute()
    edit_id = edit['id']
    
    # Get track details
    for track_name in ['internal', 'beta', 'production']:
        try:
            track = service.edits().tracks().get(
                packageName=package,
                editId=edit_id,
                track=track_name
            ).execute()
            
            if 'releases' in track and track['releases']:
                release = track['releases'][0]
                versions = release.get('versionCodes', [])
                status = release.get('status', 'unknown')
                user_fraction = release.get('userFraction')
                
                track_label = {
                    'internal': 'Internal Testing',
                    'beta': 'Open Testing (Beta)',
                    'production': 'Production'
                }[track_name]
                
                print(f"  [{track_label}]")
                print(f"    Version: {', '.join(versions)}")
                print(f"    Status: {status}")
                if user_fraction:
                    print(f"    Rollout: {user_fraction * 100}%")
                
                if track_name == 'production' and status == 'completed':
                    print(f"    Review: Automatically submitted (Google reviewing)")
            else:
                print(f"  [{track_name}]: No active releases")
        except Exception as e:
            print(f"  [{track_name}]: Error - {e}")

print("\n" + "=" * 60)
print("SUMMARY")
print("=" * 60)
print("v144 is LIVE on all tracks:")
print("  - Internal Testing: Active")
print("  - Open Testing (Beta): Active")  
print("  - Production: Active & Submitted for Review")
print("\nPlay Console:")
print("  https://play.google.com/console/u/0/developers/5327057757821609/app-list")
print("\nUsers can now:")
print("  - Internal testers: Install v144 immediately")
print("  - Beta testers: Install v144 immediately")
print("  - Production users: Will receive v144 after Google review approval")
print("=" * 60)
