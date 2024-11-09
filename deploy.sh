echo "platform: $1";
rm docs/$1/CallHelper.apk
mv app/release/app-release.apk docs/$1/CallHelper.apk
cd docs && git add . && git commit -m "update CallHelper.apk" && git push -u origin master
