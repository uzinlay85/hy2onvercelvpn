# Release APK Build Configuration Guide

## Changes Made

✅ **Fixed Debug vs Release APK Format Differences**

### 1. **Added Signing Configuration**

- Release builds now use proper signing with keystore
- Environment variables for secure keystore management:
  - `KEYSTORE_PATH`: Path to your .jks keystore file
  - `KEYSTORE_PASSWORD`: Keystore password
  - `KEY_ALIAS`: Key alias name
  - `KEY_PASSWORD`: Key password

### 2. **Enabled Minification & Resource Shrinking**

- `isMinifyEnabled = true` - Code obfuscation enabled
- `isShrinkResources = true` - Unused resources removed
- Reduced APK size significantly
- Enhanced security through code obfuscation

### 3. **Enhanced ProGuard Rules**

- Comprehensive rules for Hilt, Room, Retrofit, Gson
- Protected DTOs and model classes
- Removed debug logging from release builds

### 4. **Debug Mode Flag**

- Added `DEBUG_MODE` build config field for runtime checks
- Debug: `true` | Release: `false`

---

## Setup Instructions

### Step 1: Generate Release Keystore

Run this command to create a release keystore (if you don't have one):

```bash
# On Windows PowerShell or Command Prompt
keytool -genkey -v -keystore safenet-release-key.jks ^
  -keyalg RSA -keysize 2048 -validity 10000 ^
  -alias safenet_release

# On macOS/Linux
keytool -genkey -v -keystore safenet-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias safenet_release
```

You'll be prompted to enter:

- Keystore password
- Key password
- Certificate details (name, organization, etc.)

### Step 2: Set Environment Variables

#### **Option A: System Environment Variables (Permanent)**

**Windows:**

1. Open System Properties → Environment Variables
2. Click "New" under System Variables
3. Add each variable:

```
KEYSTORE_PATH = C:\path\to\safenet-release-key.jks
KEYSTORE_PASSWORD = your_keystore_password
KEY_ALIAS = safenet_release
KEY_PASSWORD = your_key_password
```

#### **Option B: Local gradle.properties (Development)**

Create/edit `android/local.properties` (NOT committed to Git):

```properties
KEYSTORE_PATH=C:\\Users\\zin\\path\\to\\safenet-release-key.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=safenet_release
KEY_PASSWORD=your_key_password
```

#### **Option C: CI/CD Pipeline (Production)**

Set secure environment variables in your CI/CD system:

- GitHub Actions Secrets
- GitLab CI/CD Variables
- Jenkins Credentials

### Step 3: Build Release APK

#### **Via Android Studio:**

1. Build → Generate Signed Bundle/APK
2. Select APK
3. Keystore path will be auto-detected from `KEYSTORE_PATH`

#### **Via Command Line:**

```bash
cd android
./gradlew clean assembleRelease
```

#### **Via Build Bundle (Recommended for Play Store):**

```bash
./gradlew clean bundleRelease
```

---

## APK Differences Explained

### **Debug APK**

- ✓ Debuggable (can be debugged in Android Studio)
- ✓ No minification (readable code)
- ✓ Full resources included
- ✗ Larger file size (~50-100MB+)
- ✓ Faster build time
- ✓ No signing required

### **Release APK** (After Changes)

- ✗ Not debuggable (production only)
- ✓ Minified code (obfuscated, smaller)
- ✓ Resources shrunk (unused removed)
- ✓ Smaller file size (~20-40MB)
- ✗ Slower build time
- ✓ Properly signed with release key
- ✓ Optimized performance
- ✓ Enhanced security

---

## Verification

### Check if Release APK is properly signed:

```bash
# Windows
keytool -printcert -jarfile app-release.apk

# macOS/Linux
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

### Compare APK sizes:

```bash
# List all APK files
dir android\app\build\outputs\apk /s

# Or use ls on macOS/Linux
find android/app/build/outputs/apk -name "*.apk" -exec ls -lh {} \;
```

---

## Important Security Notes

⚠️ **NEVER commit these to Git:**

- `safenet-release-key.jks`
- Passwords in gradle.properties
- Environment variables file

✅ **DO:**

- Store keystore in secure location
- Add keystore to `.gitignore`
- Use environment variables for CI/CD
- Backup keystore file securely
- Keep password safe (you'll need it forever for app updates)

---

## Troubleshooting

### "Signing configuration not found"

- Verify `KEYSTORE_PATH` environment variable is set
- Check keystore file exists at the path

### "Invalid keystore format"

- Regenerate keystore using Step 1
- Ensure `.jks` extension is correct

### "APK still large for release build"

- Enable resource shrinking: `isShrinkResources = true` ✓ (already done)
- Check ProGuard rules aren't too permissive
- Consider using Android App Bundle for Play Store

### "Build fails during minification"

- Update ProGuard rules in `proguard-rules.pro`
- Add `-keep` rules for any custom classes
- Check for reflection-based code that needs keeping

---

## Build Command Reference

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build debug and release
./gradlew assemble

# Build with specific minSdk
./gradlew assembleRelease -PminSdk=26

# Clear build cache and rebuild
./gradlew clean assembleRelease

# View detailed build logs
./gradlew assembleRelease --info
```

---

## Next Steps

1. ✅ Configure keystore path
2. ✅ Set environment variables
3. ✅ Build and test release APK
4. ✅ Verify APK is signed correctly
5. ✅ Test app functionality on release build
6. ✅ Compare sizes: Debug vs Release
