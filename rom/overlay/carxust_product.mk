#
# Integração do CarXust no produto AOSP para Raspberry Pi 4 (AAOS).
#
# Este arquivo é herdado por device/brcm/rpi4/aosp_rpi4_car.mk pelo script
# rom/rpi4/integrate-carxust.sh. As variáveis CARXUST_PATH apontam para
# device/brcm/rpi4/carxust, onde o APK, o helper nativo e o overlay são
# copiados.
#

CARXUST_PATH := device/brcm/rpi4/carxust

# Torna os módulos Soong do CarXust (APK e helper CAN) visíveis ao produto.
PRODUCT_SOONG_NAMESPACES += $(CARXUST_PATH)

PRODUCT_PACKAGES += \
    CarXust \
    carxust-can

PRODUCT_COPY_FILES += \
    $(CARXUST_PATH)/overlay/carxust.rc:$(TARGET_COPY_OUT_SYSTEM)/etc/init/carxust.rc \
    $(CARXUST_PATH)/overlay/carxust-boot.sh:$(TARGET_COPY_OUT_SYSTEM)/etc/carxust-boot.sh \
    $(CARXUST_PATH)/overlay/privapp-permissions-carxust.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/privapp-permissions-carxust.xml \
    $(CARXUST_PATH)/overlay/carxust-default-permissions.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/default-permissions/carxust-default-permissions.xml

# Mantém o CarXust como candidato a launcher; o carxust-boot.sh tenta
# defini-lo como HOME no primeiro boot.
PRODUCT_PROPERTY_OVERRIDES += \
    persist.carxust.integrated=1
