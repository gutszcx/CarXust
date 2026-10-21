#!/usr/bin/env bash
#
# Grava a imagem gerada pelo build do Raspberry Pi 4/5 em um cartão SD
# (ou SSD USB). ATENÇÃO: sobrescreve todo o conteúdo do dispositivo.
#
# Uso:
#   sudo ./rom/rpi4/flash-rpi4-sd.sh --image ~/aosp-rpi4/out/target/product/rpi4/RaspberryVanillaAOSP17-*-rpi4_car.img --device /dev/sdX
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../common/functions.sh
source "$SCRIPT_DIR/../common/functions.sh"

IMAGE=""
DEVICE=""
AOSP_DIR="${AOSP_DIR:-$HOME/aosp-rpi4}"
CARXUST_ASSUME_YES="${CARXUST_ASSUME_YES:-0}"

usage() {
    cat <<'EOF'
Uso: flash-rpi4-sd.sh --device /dev/sdX [--image ARQUIVO]

  --image ARQUIVO  imagem .img (padrão: a mais recente em out/target/product/rpi4)
  --device DISPOS.  dispositivo de bloco de destino (ex.: /dev/sdb)
  --yes             não pede confirmação
  -h, --help        mostra esta ajuda
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --image) IMAGE="$2"; shift 2 ;;
        --device) DEVICE="$2"; shift 2 ;;
        --yes) CARXUST_ASSUME_YES=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) die "opção desconhecida: $1" ;;
    esac
done

export CARXUST_ASSUME_YES
require_cmd lsblk
require_cmd dd
require_cmd sudo

if [ -z "$IMAGE" ]; then
    IMAGE="$(find "$AOSP_DIR/out/target/product/rpi4" -maxdepth 1 -name 'RaspberryVanillaAOSP*-rpi4_car.img' -printf '%T@ %p\n' 2>/dev/null \
        | sort -nr | awk 'NR==1 {print $2}')"
fi
[ -n "$IMAGE" ] || die "imagem não encontrada; passe --image CAMINHO"
require_file "$IMAGE"

if [ -z "$DEVICE" ]; then
    echo "Dispositivos de bloco disponíveis:"
    lsblk -d -o NAME,SIZE,MODEL,TRAN
    die "informe o dispositivo com --device (ex.: /dev/sdb)"
fi
[ -b "$DEVICE" ] || die "não é um dispositivo de bloco: $DEVICE"

case "$DEVICE" in
    *[0-9]) die "$DEVICE parece uma partição; use o disco inteiro (ex.: /dev/sdb)" ;;
esac

if findmnt -rn -S "$DEVICE" >/dev/null 2>&1 || findmnt -rn -S "${DEVICE}1" >/dev/null 2>&1; then
    die "$DEVICE (ou uma partição) está montado; desmonte antes de gravar"
fi

ROOT_SRC="$(findmnt -n -o SOURCE / 2>/dev/null || true)"
if [ -n "$ROOT_SRC" ] && [[ "$ROOT_SRC" == "$DEVICE"* ]]; then
    die "$DEVICE contém o sistema em execução"
fi

size_bytes="$(lsblk -bndo SIZE "$DEVICE" | head -1)"
if [ -n "$size_bytes" ] && [ "$size_bytes" -lt $((15 * 1000 * 1000 * 1000)) ]; then
    warn "dispositivo menor que 16 GB; a imagem do RPi4 tem ~15,4 GB"
fi

log "Imagem:  $IMAGE ($(du -h "$IMAGE" | cut -f1))"
log "Destino: $DEVICE ($(lsblk -dno SIZE,MODEL "$DEVICE" | xargs))"
confirm "TODOS os dados em $DEVICE serão apagados. Continuar?" || die "cancelado"

sudo dd if="$IMAGE" of="$DEVICE" bs=4M status=progress conv=fsync
sync
ok "Imagem gravada. Insira o cartão no Raspberry Pi e ligue."
