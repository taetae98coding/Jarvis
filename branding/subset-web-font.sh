#!/bin/sh
# Web 번들에 싣는 한글 폰트를 다시 만든다(docs/platform/web.html#release-build). Skiko wasm 은 브라우저 폰트를 쓰지
# 못해서 한글 폰트를 직접 싣는데, 원본 가변 폰트(10.4 MB)를 쓰는 글자 범위와 Regular 한 벌로 줄여 2.8 MB 로 만든다.
# fonttools 가 필요하다: python3 -m venv .venv && .venv/bin/pip install fonttools
set -eu

root=$(cd "$(dirname "$0")/.." && pwd)
fonttools=${FONTTOOLS:-fonttools}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

curl -sfL -o "$work/NotoSansKR.ttf" "https://github.com/google/fonts/raw/main/ofl/notosanskr/NotoSansKR%5Bwght%5D.ttf"

# 라틴·라틴-1, 한글 자모, 일반 구두점, 화살표(→), CJK 기호, 호환 자모, 한글 음절, 전각. 한자는 화면에 쓰지 않아 뺀다.
"$fonttools" subset "$work/NotoSansKR.ttf" \
    --unicodes="U+0020-00FF,U+1100-11FF,U+2000-206F,U+2190-21FF,U+3000-303F,U+3130-318F,U+AC00-D7A3,U+FF00-FFEF" \
    --layout-features='*' --output-file="$work/subset.ttf"
"$fonttools" varLib.instancer "$work/subset.ttf" wght=400 \
    -o "$root/webApp/src/wasmJsMain/composeResources/font/noto_sans_kr_regular.ttf"
