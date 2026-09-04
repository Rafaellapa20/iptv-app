#!/usr/bin/env bash
# Script de release automatizado.
#
# Substitui o processo manual repetido em quase todas as releases anteriores:
# bump de versao -> build -> copiar APK -> atualizar update.json -> commit -> push.
# Cada um desses passos ja causou pelo menos um erro humano numa release passada
# (versao esquecida no update.json, footer com texto fixo desatualizado, etc.),
# por isso este script centraliza tudo num unico ponto.
#
# Uso:
#   ./scripts/release.sh <novaVersionName> "<notas de release>"
#
# Exemplo:
#   ./scripts/release.sh 10.67 "Corrigido bug de sincronizacao entre dispositivos"
#
# O script:
#   1. Le o versionCode atual do app/build.gradle e incrementa +1
#   2. Escreve o novo versionCode/versionName no app/build.gradle
#   3. Corre ./gradlew assembleDebug
#   4. Arquiva o APK antigo (root) para archive/apks/ e copia o novo para a raiz
#   5. Atualiza update.json (versionCode, versionName, apkUrl, releaseNotes)
#   6. Mostra um resumo e pede confirmacao antes de qualquer commit/push
#
# Nao faz commit/push sem confirmacao explicita (SIM) para evitar publicar
# uma versao por engano.

set -euo pipefail
cd "$(dirname "$0")/.."

if [ $# -lt 2 ]; then
    echo "Uso: ./scripts/release.sh <novaVersionName> \"<notas de release>\""
    echo "Exemplo: ./scripts/release.sh 10.67 \"Corrigido bug de sincronizacao\""
    exit 1
fi

NEW_VERSION_NAME="$1"
RELEASE_NOTES="$2"
GRADLE_FILE="app/build.gradle"
UPDATE_JSON="update.json"
REPO_URL_BASE="https://raw.githubusercontent.com/Rafaellapa20/iptv-app/main"

echo "== Release automatizada =="

# 1. Ler versionCode atual e incrementar
# (usa sed em vez de grep -P: grep -P nao funciona de forma fiavel em todos
# os ambientes Git Bash / Windows por causa de restricoes de locale)
CURRENT_VERSION_CODE=$(sed -n 's/.*versionCode[[:space:]]\+\([0-9]\+\).*/\1/p' "$GRADLE_FILE")
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
CURRENT_VERSION_NAME=$(sed -n 's/.*versionName[[:space:]]\+"\([^"]*\)".*/\1/p' "$GRADLE_FILE")

echo "Versao atual:  versionCode=$CURRENT_VERSION_CODE versionName=$CURRENT_VERSION_NAME"
echo "Versao nova:   versionCode=$NEW_VERSION_CODE versionName=$NEW_VERSION_NAME"
echo "Notas:         $RELEASE_NOTES"
echo ""

# 2. Escrever nova versao no build.gradle
sed -i "s/versionCode $CURRENT_VERSION_CODE/versionCode $NEW_VERSION_CODE/" "$GRADLE_FILE"
sed -i "s/versionName \"$CURRENT_VERSION_NAME\"/versionName \"$NEW_VERSION_NAME\"/" "$GRADLE_FILE"

echo "-> app/build.gradle atualizado."

# 3. Build
echo "-> A compilar (./gradlew assembleDebug)..."
./gradlew assembleDebug

APK_SRC="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_SRC" ]; then
    echo "ERRO: APK nao encontrado em $APK_SRC apos o build."
    exit 1
fi

# 4. Arquivar APK antigo e copiar o novo para a raiz
mkdir -p archive/apks
OLD_APK=$(ls iptv_v*-debug.apk 2>/dev/null | head -n1 || true)
if [ -n "$OLD_APK" ]; then
    git mv "$OLD_APK" "archive/apks/$OLD_APK" 2>/dev/null || mv "$OLD_APK" "archive/apks/$OLD_APK"
    echo "-> APK antigo arquivado: archive/apks/$OLD_APK"
fi

NEW_APK="iptv_v${NEW_VERSION_NAME}-debug.apk"
cp "$APK_SRC" "$NEW_APK"
echo "-> Novo APK copiado para: $NEW_APK"

# 5. Atualizar update.json
cat > "$UPDATE_JSON" <<EOF
{
  "versionCode": $NEW_VERSION_CODE,
  "versionName": "$NEW_VERSION_NAME",
  "apkUrl": "$REPO_URL_BASE/$NEW_APK",
  "releaseNotes": "v${NEW_VERSION_NAME}:\n- $RELEASE_NOTES"
}
EOF
echo "-> update.json atualizado."

echo ""
echo "== Resumo =="
echo "versionCode: $NEW_VERSION_CODE"
echo "versionName: $NEW_VERSION_NAME"
echo "APK:         $NEW_APK"
echo ""
read -p "Fazer commit e push agora? (SIM/nao): " CONFIRM

if [ "$CONFIRM" = "SIM" ]; then
    # Importante: adiciona TODAS as alteracoes a ficheiros ja trackeados
    # (codigo-fonte, layouts, etc.), nao so os ficheiros de release
    # (build.gradle/update.json/APK). O APK e sempre compilado a partir do
    # working tree completo, por isso o commit tem de refletir exatamente
    # o que foi para dentro do APK publicado — caso contrario o source no
    # GitHub fica dessincronizado do binario distribuido.
    git add -u -- ':!.claude'
    git add "$GRADLE_FILE" "$UPDATE_JSON" "$NEW_APK" archive/apks/ 2>/dev/null || true
    git commit -m "Release v${NEW_VERSION_NAME}: ${RELEASE_NOTES}"
    git push origin main
    echo "-> Publicado no GitHub."
else
    echo "-> Commit/push cancelado. As alteracoes ficam por commitar localmente."
fi
