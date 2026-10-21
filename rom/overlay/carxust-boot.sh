#!/system/bin/sh
#
# carxust-boot.sh - ajustes de integração do CarXust no sistema.
# Executado como root pelo init (ver carxust.rc).
#
# Todas as ações são best-effort: nunca aborta o boot por causa de um erro.

sleep 2

# --- Rótulos SELinux (útil após repack manual de system.img) ----------------
restorecon -R /system/priv-app/CarXust >/dev/null 2>&1 || true
restorecon /system/etc/init/carxust.rc /system/etc/carxust-boot.sh \
    /system/etc/permissions/privapp-permissions-carxust.xml \
    /system/etc/default-permissions/carxust-default-permissions.xml >/dev/null 2>&1 || true

# --- CarXust como launcher padrão -------------------------------------------
cmd package set-home-activity --user 0 com.carxust/.MainActivity >/dev/null 2>&1 || true

# --- Modo automotivo ---------------------------------------------------------
settings put secure lockscreen.disabled 1 >/dev/null 2>&1 || true
settings put system screen_off_timeout 2147483647 >/dev/null 2>&1 || true
settings put system accelerometer_rotation 0 >/dev/null 2>&1 || true
settings put system user_rotation 1 >/dev/null 2>&1 || true
settings put global policy_control immersive.full=com.carxust >/dev/null 2>&1 || true
settings put secure immersive_mode_confirmations confirmed >/dev/null 2>&1 || true

# --- Barramento CAN ----------------------------------------------------------
# Sobe quaisquer interfaces SocketCAN já registradas pelo kernel, em 500 kbit/s
# (ajuste CAN_BITRATE conforme o veículo/adaptador).
CAN_BITRATE=500000
for iface in can0 can1 can2 can3; do
    if [ -e "/sys/class/net/$iface" ]; then
        ip link set "$iface" down >/dev/null 2>&1 || true
        ip link set "$iface" up type can bitrate "$CAN_BITRATE" >/dev/null 2>&1 || true
        echo "carxust: $iface configurado em $CAN_BITRATE bit/s" >/dev/kmsg 2>/dev/null || true
    fi
done

exit 0
