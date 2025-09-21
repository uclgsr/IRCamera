@echo off
chcp 65001

call gradlew clean

echo "[ph][ph][ph][ph]Google[ph][ph]"
call gradlew :app:assembleRelease

echo "[ph][ph][ph][ph][ph][ph]，apk[ph][ph][ph][ph][ph][ph]outputs/"
echo "Google[ph][ph]APK[ph][ph][ph]"

pause