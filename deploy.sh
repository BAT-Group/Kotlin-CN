#!/usr/bin/env bash

cp ../release/project.properties project.properties
cp project.properties account/src/main/resources && cp project.properties article/src/main/resources

echo "++ recompile projects ++"
./gradlew account:jar --daemon && ./gradlew article:jar --daemon

echo '++ service stop ++'
pkill -f *article* && pkill -f *account*

#如果文件夹不存在，创建文件夹
if [ ! -d "log" ]; then
  mkdir log
fi

nohup java -jar account/build/libs/account-1.0.0-release.jar 9000 8080 > log/account1.log &
nohup java -jar account/build/libs/account-1.0.0-release.jar 9001 8081 > log/account2.log &
nohup java -jar account/build/libs/account-1.0.0-release.jar 9002 8082 > log/account3.log &
nohup java -jar article/build/libs/article-1.0.0-release.jar 9003 8083 > log/article1.log &
nohup java -jar article/build/libs/article-1.0.0-release.jar 9004 8084 > log/article2.log &
nohup java -jar article/build/libs/article-1.0.0-release.jar 9005 8085 > log/article3.log &
echo '++ service restart ++'