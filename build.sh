#!/usr/bin/env bash
set -euo pipefail
sdk="${ANDROID_HOME:?Android SDK required}"
bt="$sdk/build-tools/35.0.0"
jar="$sdk/platforms/android-35/android.jar"
mkdir -p build/{classes,dex}
"$bt/aapt2" compile --dir res -o build/resources.zip
"$bt/aapt2" link -I "$jar" --manifest AndroidManifest.xml -o build/app.apk build/resources.zip
javac --release 8 -classpath "$jar" -encoding UTF-8 -d build/classes src/tr/olgun/alkis/*.java
jar --create --file build/classes.jar -C build/classes .
"$bt/d8" --min-api 28 --lib "$jar" --output build/dex build/classes.jar
(cd build/dex && zip -q ../app.apk classes.dex)
"$bt/zipalign" -f 4 build/app.apk build/aligned.apk
