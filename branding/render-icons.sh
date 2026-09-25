#!/bin/sh
# branding/*.svg 에서 플랫폼 아이콘 PNG·ICNS 를 다시 만든다. 결과는 커밋한다 — 빌드 머신에 Chrome 을 요구하지 않기 위해서다.
# 이 머신에는 rsvg-convert·ImageMagick 이 없어서 SVG 를 그리는 데 headless Chrome 을 쓴다(docs/common/release-build.html#implementation).
# Android 런처 아이콘은 벡터 드로어블(androidApp/src/main/res/drawable/ic_launcher_foreground.xml)이라 여기서 만들지 않는다.
# 도형을 바꾸면 그 파일도 손으로 맞춘다.
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
chrome=${CHROME_BIN:-"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

render() { # <svg 이름> <출력 png>
    printf '<html><body style="margin:0;background:transparent"><img src="file://%s" width="1024" height="1024" style="display:block"></body></html>' \
        "$root/branding/$1.svg" > "$work/$1.html"
    "$chrome" --headless --disable-gpu --hide-scrollbars --default-background-color=00000000 \
        --window-size=1024,1024 --allow-file-access-from-files --screenshot="$2" "file://$work/$1.html" 2>/dev/null
}

render icon-square "$work/square.png"
render icon-macos "$work/macos.png"

# iOS 는 알파가 없는 1024px 하나만 받는다. icon-square 는 바탕을 꽉 채워서 알파가 생기지 않는다.
cp "$work/square.png" "$root/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png"

sips -z 180 180 "$work/macos.png" --out "$root/webApp/src/wasmJsMain/resources/favicon.png" > /dev/null

mkdir -p "$work/Jarvis.iconset"
for size in 16 32 128 256 512; do
    sips -z "$size" "$size" "$work/macos.png" --out "$work/Jarvis.iconset/icon_${size}x${size}.png" > /dev/null
    double=$((size * 2))
    sips -z "$double" "$double" "$work/macos.png" --out "$work/Jarvis.iconset/icon_${size}x${size}@2x.png" > /dev/null
done
iconutil -c icns "$work/Jarvis.iconset" -o "$root/desktopApp/icon/Jarvis.icns"
