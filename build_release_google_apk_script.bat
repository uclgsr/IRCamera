@echo off
chcp 65001

call gradlew clean

echo "开始编译Google版本"
call gradlew :app:assembleRelease


@rem call gradlew :app:assembleProdDebug

echo "编译打包完成，apk文件在根目录outputs/"
echo "文件名有Topdon是目标版本27,发到官方网站"
echo "如果是debug测试，则取debug的文件夹"

pause