#!/usr/bin/env bash
#
# Injeta o CarXust em um system.img ext4 já existente (Orange Pi Zero 3 /
# Android 12 BSP). Útil quando o BSP não tem ADB root/remount.
#
# ATENÇÃO: este script monta a imagem com loop device e exige sudo. Nem todo
# BSP Android 12 usa system.img separado; se o seu usa super.img/dynamic
# partitions, prefira install-app.sh (ADB) ou integre no build do BSP.
#
# Uso:
#   ./rom/orangepi-zero3/repack-system.sh \
#       --system /caminho/system.img \
#       --apk app/build/outputs/apk/debug/app-debug.apk \
#       --out /caminho/system-carxust.img
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../common/functions.sh
source "$SCRIPT_DIR/../common/functions.sh"

SYSTEM_IMG=""
APK=""
OUT=""
WORK=""
MOUNTED=0

usage() {
    cat <<'EOF'
Uso: repack-system.sh --system system.img [--apk APK] [--out SAIDA]

  --system ARQUIVO  system.img de origem (ext4, raw ou sparse)
  --apk ARQUIVO     APK do CarXust (padrão: último build do Gradle)
  --out ARQUIVO     imagem de saída (padrão: <origem>-carxust.img)
  -h, --help        mostra esta ajuda
EOF
}

cleanup() {
    if [ "$MOUNTED" = "1" ]; then
        sudo umount "$WORK/mnt" >/dev/null 2>&1 || true
    fi
    [ -n "$WORK" ] && rm -rf "$WORK"
}
trap cleanup EXIT

while [ $# -gt 0 ]; do
    case "$1" in
        --system) SYSTEM_IMG="$2"; shift 2 ;;
        --apk) APK="$2"; shift 2 ;;
        --out) OUT="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) die "opção desconhecida: $1" ;;
    esac
done

require_cmd sudo mount umount e2fsck cp
require_file "$SYSTEM_IMG"
[ -n "$OUT" ] || OUT="${SYSTEM_IMG%.img}-carxust.img"
[ "$OUT" != "$SYSTEM_IMG" ] || die "a saída não pode sobrescrever a imagem de origem"

if ! APK_PATH="$(find_apk "$APK")"; then
    die "APK não encontrado. Gere com ./gradlew assembleDebug ou passe --apk CAMINHO"
fi

WORK="$(mktemp -d /tmp/carxust-repack.XXXXXX)"
cp "$SYSTEM_IMG" "$WORK/system.img"

if [ "$(od -An -tx4 -N4 "$WORK/system.img" | tr -d ' \n')" = "ed26ff3a" ]; then
    require_cmd simg2img
    log "Convertendo imagem sparse para raw..."
    simg2img "$WORK/system.img" "$WORK/system.raw.img"
    mv "$WORK/system.raw.img" "$WORK/system.img"
fi

mkdir -p "$WORK/mnt"
log "Montando $WORK/system.img (sudo)..."
sudo mount -o loop,rw "$WORK/system.img" "$WORK/mnt"
MOUNTED=1

if [ -d "$WORK/mnt/system/priv-app" ]; then
    ROOT="$WORK/mnt/system"
elif [ -d "$WORK/mnt/priv-app" ]; then
    ROOT="$WORK/mnt"
else
    die "não foi possível localizar /system/priv-app na imagem"
fi
log "Raiz do sistema na imagem: ${ROOT#"$WORK/mnt"}"

STAGE="$WORK/stage"
stage_device_files "$STAGE" "$APK_PATH"

sudo mkdir -p \
    "$ROOT/priv-app/CarXust" \
    "$ROOT/etc/init" \
    "$ROOT/etc/permissions" \
    "$ROOT/etc/default-permissions"

sudo cp "$STAGE/CarXust.apk" "$ROOT/priv-app/CarXust/CarXust.apk"
sudo cp "$STAGE/overlay/carxust.rc" "$ROOT/etc/init/carxust.rc"
sudo cp "$STAGE/overlay/carxust-boot.sh" "$ROOT/etc/carxust-boot.sh"
sudo cp "$STAGE/overlay/privapp-permissions-carxust.xml" \
    "$ROOT/etc/permissions/privapp-permissions-carxust.xml"
sudo cp "$STAGE/overlay/carxust-default-permissions.xml" \
    "$ROOT/etc/default-permissions/carxust-default-permissions.xml"

if [ -f "$CARXUST_ROOT/rom/native/carxust-can" ]; then
    sudo cp "$CARXUST_ROOT/rom/native/carxust-can" "$ROOT/bin/carxust-can"
    sudo chmod 755 "$ROOT/bin/carxust-can"
fi

sudo chown -R 0:0 \
    "$ROOT/priv-app/CarXust" \
    "$ROOT/etc/init/carxust.rc" \
    "$ROOT/etc/carxust-boot.sh" \
    "$ROOT/etc/permissions/privapp-permissions-carxust.xml" \
    "$ROOT/etc/default-permissions/carxust-default-permissions.xml"

sudo chmod 644 \
    "$ROOT/priv-app/CarXust/CarXust.apk" \
    "$ROOT/etc/init/carxust.rc" \
    "$ROOT/etc/carxust-boot.sh" \
    "$ROOT/etc/permissions/privapp-permissions-carxust.xml" \
    "$ROOT/etc/default-permissions/carxust-default-permissions.xml"

sync
sudo umount "$WORK/mnt"
MOUNTED=0

log "Verificando o sistema de arquivos..."
set +e
sudo e2fsck -f -y "$WORK/system.img" >/dev/null
fsck_rc=$?
set -e
[ "$fsck_rc" -le 1 ] || die "e2fsck falhou (código $fsck_rc)"

cp "$WORK/system.img" "$OUT"
ok "Imagem gerada: $OUT"
warn "Rótulos SELinux de arquivos novos podem ser corrigidos no primeiro boot"
warn "(carxust-boot.sh executa restorecon). Teste antes de gravar no cartão."
