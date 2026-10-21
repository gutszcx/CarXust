#!/usr/bin/env bash
#
# Funções compartilhadas pelos scripts de ROM do CarXust.
# Uso: source "$(dirname "$0")/../common/functions.sh"
#
# Requer bash 4+. Não depende de systemd/apt: apenas coreutils e as
# ferramentas que cada script valida individualmente.

set -o pipefail

CARXUST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export CARXUST_ROOT

if [ -t 1 ]; then
    C_RESET=$'\033[0m'
    C_INFO=$'\033[1;36m'
    C_OK=$'\033[1;32m'
    C_WARN=$'\033[1;33m'
    C_ERR=$'\033[1;31m'
else
    C_RESET=""
    C_INFO=""
    C_OK=""
    C_WARN=""
    C_ERR=""
fi

log()  { printf '%s[carxust]%s %s\n' "$C_INFO" "$C_RESET" "$*"; }
ok()   { printf '%s[ ok ]%s %s\n' "$C_OK" "$C_RESET" "$*"; }
warn() { printf '%s[aviso]%s %s\n' "$C_WARN" "$C_RESET" "$*" >&2; }
die()  { printf '%s[erro]%s %s\n' "$C_ERR" "$C_RESET" "$*" >&2; exit 1; }

require_cmd() {
    local cmd
    for cmd in "$@"; do
        command -v "$cmd" >/dev/null 2>&1 || die "comando obrigatório não encontrado: $cmd"
    done
}

require_file() {
    [ -f "$1" ] || die "arquivo não encontrado: $1"
}

require_dir() {
    [ -d "$1" ] || die "diretório não encontrado: $1"
}

confirm() {
    local prompt="$1"
    local answer
    if [ "${CARXUST_ASSUME_YES:-0}" = "1" ]; then
        return 0
    fi
    read -r -p "$prompt [s/N] " answer
    case "$answer" in
        s|S|y|Y|sim|yes) return 0 ;;
        *) return 1 ;;
    esac
}

available_kb() {
    df -Pk "$1" 2>/dev/null | awk 'NR==2 {print $4}'
}

human_gb() {
    awk -v kb="$1" 'BEGIN { printf "%.1f GB", kb / 1024 / 1024 }'
}

sha256_of() {
    require_cmd sha256sum
    sha256sum "$1" | awk '{print $1}'
}

find_apk() {
    local preferred="$1"
    if [ -n "$preferred" ]; then
        require_file "$preferred"
        printf '%s\n' "$preferred"
        return 0
    fi
    local candidate
    for candidate in \
        "$CARXUST_ROOT/app/build/outputs/apk/release/app-release.apk" \
        "$CARXUST_ROOT/app/build/outputs/apk/debug/app-debug.apk"; do
        if [ -f "$candidate" ]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done
    return 1
}

# Copia os arquivos de overlay e o binário nativo para dentro de uma árvore
# de dispositivo AOSP. $1 = destino (ex.: device/brcm/rpi4/carxust)
stage_device_files() {
    local dest="$1"
    local apk="$2"
    require_file "$apk"
    mkdir -p "$dest/overlay" "$dest/native"

    cp "$CARXUST_ROOT/rom/overlay/carxust.rc" "$dest/overlay/"
    cp "$CARXUST_ROOT/rom/overlay/carxust-boot.sh" "$dest/overlay/"
    cp "$CARXUST_ROOT/rom/overlay/privapp-permissions-carxust.xml" "$dest/overlay/"
    cp "$CARXUST_ROOT/rom/overlay/carxust-default-permissions.xml" "$dest/overlay/"
    cp "$CARXUST_ROOT/rom/native/carxust-can.c" "$dest/native/"
    cp "$CARXUST_ROOT/rom/native/Android.bp" "$dest/native/"
    cp "$CARXUST_ROOT/rom/overlay/carxust_product.mk" "$dest/"
    cp "$CARXUST_ROOT/rom/overlay/Android.bp" "$dest/"
    cp "$apk" "$dest/CarXust.apk"
}
