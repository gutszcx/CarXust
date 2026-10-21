#!/usr/bin/env bash
#
# Integra o CarXust (APK + helper CAN + overlay) em uma árvore AOSP já
# sincronizada do raspberry-vanilla/android_device_brcm_rpi4.
#
# O script é idempotente: pode ser executado novamente após rebuild do APK.
#
# Uso:
#   ./rom/rpi4/integrate-carxust.sh --aosp ~/aosp-rpi4 --apk app/build/outputs/apk/debug/app-debug.apk
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../common/functions.sh
source "$SCRIPT_DIR/../common/functions.sh"

AOSP_DIR="${AOSP_DIR:-$HOME/aosp-rpi4}"
APK=""
PATCH_PRODUCT=1

usage() {
    cat <<'EOF'
Uso: integrate-carxust.sh [opções]

  --aosp DIR       árvore AOSP (padrão: ~/aosp-rpi4)
  --apk ARQUIVO    APK a integrar (padrão: último build do Gradle)
  --no-patch       copia arquivos mas não altera aosp_rpi4_car.mk
  --yes            assume "sim"
  -h, --help       mostra esta ajuda
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --aosp) AOSP_DIR="$2"; shift 2 ;;
        --apk) APK="$2"; shift 2 ;;
        --no-patch) PATCH_PRODUCT=0; shift ;;
        --yes) CARXUST_ASSUME_YES=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) die "opção desconhecida: $1" ;;
    esac
done

export CARXUST_ASSUME_YES="${CARXUST_ASSUME_YES:-0}"

PRODUCT_MAKEFILE="$AOSP_DIR/device/brcm/rpi4/aosp_rpi4_car.mk"
DEVICE_DIR="$AOSP_DIR/device/brcm/rpi4"
DEST="$DEVICE_DIR/carxust"

require_dir "$DEVICE_DIR"
require_file "$PRODUCT_MAKEFILE"

if ! APK_PATH="$(find_apk "$APK")"; then
    die "APK não encontrado. Gere com ./gradlew assembleDebug ou passe --apk CAMINHO"
fi
require_file "$APK_PATH"

log "APK: $APK_PATH"
log "Destino: $DEST"
confirm "Copiar APK e overlay para a árvore AOSP em $AOSP_DIR?" || die "cancelado"

rm -rf "$DEST"
stage_device_files "$DEST" "$APK_PATH"
sha256_of "$APK_PATH" > "$DEST/CarXust.apk.sha256"
ok "Arquivos copiados"

if [ "$PATCH_PRODUCT" = "1" ]; then
    INHERIT_LINE='$(call inherit-product, device/brcm/rpi4/carxust/carxust_product.mk)'
    if grep -qF "$INHERIT_LINE" "$PRODUCT_MAKEFILE"; then
        ok "aosp_rpi4_car.mk já contém a integração"
    else
        {
            printf '\n# CarXust\n'
            printf '%s\n' "$INHERIT_LINE"
        } >> "$PRODUCT_MAKEFILE"
        ok "Integração adicionada a aosp_rpi4_car.mk"
    fi
else
    warn "--no-patch: lembre-se de herdar carxust_product.mk manualmente"
fi

cat <<EOF

Próximos passos:
  cd "$AOSP_DIR"
  source build/envsetup.sh
  lunch aosp_rpi4_car-cp2a-userdebug
  make bootimage systemimage vendorimage -j\$(nproc)
  ./device/brcm/rpi4/mkimg.sh
EOF
