@echo off
echo ========================================================
echo SafeNet VPN - Building libbox.aar (Sing-Box Engine)
echo ========================================================
echo.

set TEMP_DIR=%TEMP%\sing-box-build
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"
cd "%TEMP_DIR%"

if not exist "sing-box" (
    echo [1/3] Cloning Sing-Box repository...
    git clone https://github.com/SagerNet/sing-box.git
)

cd sing-box
git restore .
git pull

echo.
echo [2/3] Installing gomobile tools...
go install golang.org/x/mobile/cmd/gomobile@latest
go install golang.org/x/mobile/cmd/gobind@latest
go get golang.org/x/mobile/bind

echo.
echo [3/3] Compiling libbox.aar (This will take 5-10 minutes)...
gomobile bind -v -androidapi 21 -javapkg=io.nekohasekai -target=android/arm64 -tags "with_quic,with_grpc,with_utls,with_clash_api" -o libbox.aar ./experimental/libbox

if exist "libbox.aar" (
    echo.
    echo Build Successful! Copying to your Android project...
    copy /Y libbox.aar "c:\Users\zin\Downloads\Ai_WebCodes\Hy2_app_Safenetapp_V2\android\app\libs\libbox.aar"
    echo Done! You can now build the APK in Android Studio.
) else (
    echo.
    echo Build Failed! Please check the error messages above.
)

pause
