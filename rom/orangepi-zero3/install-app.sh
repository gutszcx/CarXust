#!/usr/bin/env bash
#
# Instala o CarXust como app privilegiado em um Orange Pi Zero 3 (Android 12
# BSP) já inicializado, via ADB. Requer build userdebug/eng com root e
# partição /system gravável (adb remount).
#
# Uso:
#   ./rom/orangepi-zero3/install-app.sh --apk app/build/outputs/apk/debug/app-debug.apk
#   ./rom/orangepi-zero3/install-app.sh --serial 192.168.1.50:5555 --no-reboot
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../common/functions.sh
source "$SCRIPT_DIR/../common/functions.sh"

APK=""
SERIAL=""
DO_REBOOT=1
ADB=(adb)

usage() {
    cat <<'EOF'
Uso: install-app.sh [opções]

  --apk ARQUIVO    APK do CarXust (padrão: último build do Gradle)
  --serial SERIAL  serial ou host:porta do adb
  --no-reboot      não reinicia ao final
  -h, --help       mostra esta ajuda
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --apk) APK="$2"; shift 2 ;;
        --serial) SERIAL="$2"; shift 2 ;;
        --no-reboot) DO_REBOOT=0; shift ;;
        -h|--help) usage; exit 0 ;;
        *) die "opção desconhecida: $1" ;;
    esac
done

require_cmd adb
[ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL")

if ! APK_PATH="$(find_apk "$APK")"; then
    die "APK não encontrado. Gere com ./gradlew assembleDebug ou passe --apk CAMINHO"
fi
require_file "$APK_PATH"

"${ADB[@]}" wait-for-device
[ "$("${ADB[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] || \
    die "dispositivo ainda não concluiu o boot"

log "Reiniciando adbd como root..."
"${ADB[@]}" root >/dev/null 2>&1 || true
"${ADB[@]}" wait-for-device
if [ "$("${ADB[@]}" shell id -u | tr -d '\r')" != "0" ]; then
    die "sem root no dispositivo. Use um build userdebug/eng do BSP."
fi

log "Remontando /system como leitura/escrita..."
if ! "${ADB[@]}" remount >/dev/null 2>&1; then
    warn "adb remount falhou. Execute manualmente e tente novamente:"
    cat <<'EOF'
  adb disable-verity
  adb reboot
  adb root && adb remount
EOF
    die "não foi possível remontar /system"
fi

log "Enviando APK e overlay..."
"${ADB[@]}" shell 'mkdir -p /system/priv-app/CarXust /system/etc/init /system/etc/permissions /system/etc/default-permissions'
"${ADB[@]}" push "$APK_PATH" /system/priv-app/CarXust/CarXust.apk
"${ADB[@]}" push "$CARXUST_ROOT/rom/overlay/privapp-permissions-carxust.xml" \
    /system/etc/permissions/privapp-permissions-carxust.xml
"${ADB[@]}" push "$CARXUST_ROOT/rom/overlay/carxust-default-permissions.xml" \
    /system/etc/default-permissions/carxust-default-permissions.xml
"${ADB[@]}" push "$CARXUST_ROOT/rom/overlay/carxust.rc" /system/etc/init/carxust.rc
"${ADB[@]}" push "$CARXUST_ROOT/rom/overlay/carxust-boot.sh" /system/etc/carxust-boot.sh

"${ADB[@]}" shell '
    chown 0:0 /system/priv-app/CarXust/CarXust.apk \
        /system/etc/init/carxust.rc \
        /system/etc/carxust-boot.sh \
        /system/etc/permissions/privapp-permissions-carxust.xml \
        /system/etc/default-permissions/carxust-default-permissions.xml
    chmod 644 /system/priv-app/CarXust/CarXust.apk \
        /system/etc/init/carxust.rc \
        /system/etc/carxust-boot.sh \
        /system/etc/permissions/privapp-permissions-carxust.xml \
        /system/etc/default-permissions/carxust-default-permissions.xml
    restorecon -R /system/priv-app/CarXust /system/etc/init/carxust.rc \
        /system/etc/carxust-boot.sh /system/etc/permissions/privapp-permissions-carxust.xml \
        /system/etc/default-permissions/carxust-default-permissions.xml 2>/dev/null || true
'

# O helper CAN nativo é opcional: use OBD-II/Bluetooth se não existir.
if [ -f "$CARXUST_ROOT/rom/native/carxust-can" ]; then
    log "Instalando helper CAN nativo..."
    "${ADB[@]}" push "$CARXUST_ROOT/rom/native/carxust-can" /system/bin/carxust-can
    "${ADB[@]}" shell 'chown 0:0 /system/bin/carxust-can && chmod 755 /system/bin/carxust-can'
fi

ok "CarXust instalado em /system/priv-app"

if [ "$DO_REBOOT" = "1" ]; then
    log "Reiniciando..."
    "${ADB[@]}" reboot
else
    warn "Reinicie manualmente para aplicar as permissões privilegiadas."
fi
