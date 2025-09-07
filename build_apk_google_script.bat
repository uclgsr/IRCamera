@echo off
chcp 65001

call gradlew clean

echo "开始编译google测试版本"
call gradlew :app:assembleDebug


@rem call gradlew :app:assembleProdDebug

echo "编译打包完成，apk文件在根目录outputs/"
echo "文件名有Topdon是目标版本27,发到官方网站"

pause