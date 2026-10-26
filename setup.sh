#!/data/data/com.termux/files/usr/bin/bash
set -e
U=oneshoot5010; R=shariha-electricity; RP=$U/$R
cd "$(dirname "$0")"
echo ">> بجهّز الأدوات..."
pkg install -y git gh openjdk-17 unzip >/dev/null
[ -f shariha.zip ] && unzip -oq shariha.zip -d shariha
cd shariha
gh auth status >/dev/null 2>&1 || gh auth login
git config --global user.name >/dev/null || git config --global user.name "$U"
git config --global user.email >/dev/null || git config --global user.email "$U@users.noreply.github.com"
[ -d .git ] || git init -q -b main
BK=$HOME/shariha-keystore; mkdir -p "$BK"
if [ ! -f "$BK/ks.jks" ]; then
  PASS=$(head -c12 /dev/urandom | od -An -tx1 | tr -d ' \n')
  keytool -genkeypair -keystore "$BK/ks.jks" -alias key -keyalg RSA -keysize 2048 -validity 10000 -storepass "$PASS" -keypass "$PASS" -dname "CN=Shariha"
  echo -n "$PASS" > "$BK/pass.txt"
fi
git add -A; git commit -qm "build" || true
gh repo view $RP >/dev/null 2>&1 || gh repo create $RP --public --source=. --remote=origin
git remote get-url origin >/dev/null 2>&1 || git remote add origin https://github.com/$RP.git
base64 -w0 "$BK/ks.jks" | gh secret set KS_B64 -R $RP
gh secret set KS_PASS -R $RP --body "$(cat "$BK/pass.txt")"
echo ">> برفع الكود وبيبدأ البناء (مرحلتين)..."
git push -u origin main
sleep 12
ID=$(gh run list -R $RP -L 1 --json databaseId -q '.[0].databaseId')
if gh run watch $ID -R $RP --exit-status; then
  echo ">> البناء نجح والإصدار اتنشر. افتح التطبيق > الإعدادات > حدّث التطبيق"
else
  gh run view $ID -R $RP --log-failed > errors.txt 2>&1 || true
  tail -n 60 errors.txt
  if termux-clipboard-set < errors.txt 2>/dev/null; then echo ">> الأخطاء اتنسخت، ابعتها هنا"; else echo ">> الأخطاء في ملف errors.txt، انسخها وابعتها"; fi
  exit 1
fi
