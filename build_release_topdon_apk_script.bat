@echo off
chcp 65001

call gradlew clean

echo "开始编译Topdon版本"
call gradlew :app:assembleRelease

echo "编译打包完成，apk文件在根目录outputs/"
echo "Topdon版本APK已完成"

pause