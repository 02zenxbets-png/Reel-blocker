#!/bin/bash
# Manual APK build for Reel Blocker (Gradle daemon never starts in this env).
# Usage: bash build-apk.sh
set -e
cd "$(dirname "$0")"

JDK=~/jdk17
export JAVA_HOME=$HOME/jdk17
export PATH=$JAVA_HOME/bin:$PATH
SDK=~/android-sdk
BT=$SDK/build-tools/34.0.0
ANDROID_JAR=$SDK/platforms/android-34/android.jar
OUT=build-manual
APK_OUT=~/workspace/your_files/ReelBlocker.apk
VER_CODE=2
VER_NAME="2.0"

rm -rf $OUT/compiled-res $OUT/apk $OUT/gen $OUT/classes $OUT/dex
mkdir -p $OUT/compiled-res $OUT/apk $OUT/gen $OUT/classes $OUT/dex

echo "== aapt2 compile =="
$BT/aapt2 compile --dir app/src/main/res -o $OUT/compiled-res/res.zip

echo "== aapt2 link =="
$BT/aapt2 link -o $OUT/apk/unsigned.apk \
  --manifest app/src/main/AndroidManifest.xml \
  -I $ANDROID_JAR \
  --java $OUT/gen \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code $VER_CODE --version-name "$VER_NAME" \
  $OUT/compiled-res/res.zip

echo "== javac =="
find app/src/main/java $OUT/gen -name "*.java" > $OUT/sources.txt
$JDK/bin/javac -encoding UTF-8 -source 17 -target 17 \
  -cp $ANDROID_JAR -d $OUT/classes @$OUT/sources.txt

echo "== d8 =="
$BT/d8 --min-api 26 --output $OUT/dex \
  $(find $OUT/classes -name "*.class")

echo "== aapt add classes.dex (from inside dex dir!) =="
cd $OUT/dex
$BT/aapt add ../apk/unsigned.apk classes.dex
cd - > /dev/null

echo "== zipalign + apksigner =="
$BT/zipalign -f 4 $OUT/apk/unsigned.apk $OUT/apk/aligned.apk
$BT/apksigner sign --ks $OUT/debug.keystore \
  --ks-pass pass:android --key-pass pass:android \
  --out "$APK_OUT" $OUT/apk/aligned.apk

echo "== verify =="
$BT/apksigner verify --print-certs "$APK_OUT" | head -3
ls -la "$APK_OUT"
echo "BUILD OK"
