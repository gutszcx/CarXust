#!/usr/bin/env bash
#
# Build de uma ROM Android Automotive (AAOS) para Raspberry Pi 4/5 usando
# o projeto raspberry-vanilla (Android 17).
#
# Requisitos (host x86_64, Ubuntu 22.04+):
#   - ~400 GB livres, 16 GB de RAM (32 GB recomendado), 8+ núcleos
#   - repo, git, curl, JDK 17, build-essential, kpartx, dosfstools,
#     e2fsprogs, fdisk, mtools, rsync
#   - sudo (o rpi4-mkimg.sh usa loop devices)
#
# Uso:
#   ./rom/rpi4/build-rpi4-aosp.sh --apk app/build/outputs/apk/debug/app-debug.apk
#   ./rom/rpi4/build-rpi4-aosp.sh --dir ~/aosp-rpi4 --no-mkimg
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../common/functions.sh
source "$SCRIPT_DIR/../common/functions.sh"

AOSP_DIR="${AOSP_DIR:-$HOME/aosp-rpi4}"
JOBS="${JOBS:-$(nproc)}"
BRANCH="${BRANCH:-android-17.0.0_r1}"
MANIFEST_BRANCH="${MANIFEST_BRANCH:-android-17.0}"
LUNCH_TARGET="${LUNCH_TARGET:-aosp_rpi4_car-cp2a-userdebug}"
APK=""
DO_SYNC=1
DO_MKIMG=1

usage() {
    cat <<'EOF'
Uso: build-rpi4-aosp.sh [opções]

  --dir DIR        diretório da árvore AOSP (padrão: ~/aosp-rpi4)
  --apk ARQUIVO    APK do CarXust para integrar na ROM
  --jobs N         paralelismo do repo sync/make (padrão: nproc)
  --branch REF     tag/branch do AOSP (padrão: android-17.0.0_r1)
  --no-sync        não executa repo sync
  --no-mkimg       não gera a imagem flashável ao final
  --yes            assume "sim" em confirmações
  -h, --help       mostra esta ajuda
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --dir) AOSP_DIR="$2"; shift 2 ;;
        --apk) APK="$2"; shift 2 ;;
        --jobs) JOBS="$2"; shift 2 ;;
        --branch) BRANCH="$2"; shift 2 ;;
        --no-sync) DO_SYNC=0; shift ;;
        --no-mkimg) DO_MKIMG=0; shift ;;
        --yes) CARXUST_ASSUME_YES=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) die "opção desconhecida: $1" ;;
    esac
done

export CARXUST_ASSUME_YES="${CARXUST_ASSUME_YES:-0}"

[ "$(uname -s)" = "Linux" ] || die "o build do AOSP requer Linux"
[ "$(uname -m)" = "x86_64" ] || die "o build do AOSP requer host x86_64 (atual: $(uname -m))"
require_cmd git curl repo
require_cmd make
[ "$DO_MKIMG" = "1" ] && require_cmd sudo

log "Árvore AOSP: $AOSP_DIR"
log "Alvo: $LUNCH_TARGET • jobs: $JOBS"

mkdir -p "$AOSP_DIR"
cd "$AOSP_DIR"

free_kb="$(available_kb "$AOSP_DIR")"
if [ -n "$free_kb" ] && [ "$free_kb" -lt $((200 * 1024 * 1024)) ]; then
    die "espaço insuficiente em $AOSP_DIR ($(human_gb "$free_kb")); são necessários ~400 GB"
fi
if [ -n "$free_kb" ] && [ "$free_kb" -lt $((300 * 1024 * 1024)) ]; then
    warn "espaço livre: $(human_gb "$free_kb"); o build completo pode falhar (recomendado 400 GB)"
fi

if [ ! -d .repo ]; then
    log "Inicializando repo ($BRANCH)..."
    repo init -u https://android.googlesource.com/platform/manifest -b "$BRANCH"
    mkdir -p .repo/local_manifests
    curl -fL --retry 3 \
        -o .repo/local_manifests/manifest_brcm_rpi.xml \
        "https://raw.githubusercontent.com/raspberry-vanilla/android_local_manifest/$MANIFEST_BRANCH/manifest_brcm_rpi.xml"
else
    ok "repo já inicializado"
fi

if [ "$DO_SYNC" = "1" ]; then
    log "Sincronizando fontes (isso pode levar horas)..."
    repo sync -c -j"$JOBS" --no-tags
else
    warn "repo sync ignorado (--no-sync)"
fi

require_file "$AOSP_DIR/device/brcm/rpi4/aosp_rpi4_car.mk"

if [ -n "$APK" ]; then
    log "Integrando CarXust no produto..."
    "$SCRIPT_DIR/integrate-carxust.sh" --aosp "$AOSP_DIR" --apk "$APK" --yes
else
    if find_apk "" >/dev/null 2>&1; then
        found_apk="$(find_apk "")"
        log "APK encontrado: $found_apk"
        "$SCRIPT_DIR/integrate-carxust.sh" --aosp "$AOSP_DIR" --apk "$found_apk" --yes
    else
        warn "APK não encontrado; a ROM será gerada sem o CarXust (use --apk)"
    fi
fi

log "Configurando ambiente de build..."
# shellcheck disable=SC1091
source build/envsetup.sh
lunch "$LUNCH_TARGET"

log "Compilando bootimage/systemimage/vendorimage..."
make bootimage systemimage vendorimage -j"$JOBS"

if [ "$DO_MKIMG" = "1" ]; then
    log "Gerando imagem flashável (rpi4-mkimg.sh)..."
    if [ -x ./device/brcm/rpi4/mkimg.sh ]; then
        ./device/brcm/rpi4/mkimg.sh
    else
        warn "device/brcm/rpi4/mkimg.sh não encontrado; gere a imagem manualmente"
    fi
fi

ok "Build concluído. Imagens em: $AOSP_DIR/out/target/product/rpi4/"
