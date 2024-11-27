ls docs
read -p "platform: " path

rm docs/$path/CallHelper.apk
cp app/release/app-release.apk docs/$path/CallHelper.apk
cd docs && git add . && git commit -m "update CallHelper.apk" && git push -u origin master
