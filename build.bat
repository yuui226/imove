@echo off
REM Convenience wrapper: `build` packages the release APK (app/build/outputs/apk/release/imove.apk).
REM Extra args are forwarded to Gradle, e.g. `build --info` or `build installRelease`.
call "%~dp0gradlew.bat" assembleRelease %*
