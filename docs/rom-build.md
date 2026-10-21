# Build e integração da ROM

> Não há imagem pré-compilada neste repositório. O build AOSP é pesado e
> precisa ser feito em um host Linux x86_64.

## Raspberry Pi 4/5 (AAOS)

### Requisitos de host

- Ubuntu 22.04 LTS ou equivalente, **x86_64**;
- 16 GB de RAM (32 GB recomendado), 8+ núcleos;
- ~400 GB livres (o download completo passa de 100 GB);
- JDK 17, `repo`, `git`, `curl`, `make`;
- `sudo` e loop devices para gerar a imagem;
- pacotes extras do raspberry-vanilla:

  ```bash
  sudo apt-get install dosfstools e2fsprogs fdisk kpartx mtools rsync
  ```

### Build automatizado

```bash
./gradlew assembleDebug

./rom/rpi4/build-rpi4-aosp.sh \
  --apk app/build/outputs/apk/debug/app-debug.apk \
  --dir ~/aosp-rpi4
```

O script:

1. valida host, arquitetura e espaço em disco;
2. executa `repo init` no branch `android-17.0.0_r1` e baixa o
   `manifest_brcm_rpi.xml` do raspberry-vanilla;
3. executa `repo sync -c -j$(nproc)`;
4. chama `rom/rpi4/integrate-carxust.sh` (APK + overlay + helper CAN);
5. faz `lunch aosp_rpi4_car-cp2a-userdebug`;
6. compila `bootimage systemimage vendorimage`;
7. gera a imagem com `device/brcm/rpi4/mkimg.sh`.

Opções úteis:

```text
--dir DIR       árvore AOSP (padrão ~/aosp-rpi4)
--apk ARQUIVO   APK a integrar (padrão: build do Gradle)
--jobs N        paralelismo
--branch REF    branch/tag do AOSP (padrão android-17.0.0_r1)
--no-sync       pula repo sync
--no-mkimg      não gera a imagem final
--yes           assume sim
```

### Build manual

```bash
mkdir -p ~/aosp-rpi4 && cd ~/aosp-rpi4

repo init -u https://android.googlesource.com/platform/manifest \
  -b android-17.0.0_r1
mkdir -p .repo/local_manifests
curl -o .repo/local_manifests/manifest_brcm_rpi.xml -L \
  https://raw.githubusercontent.com/raspberry-vanilla/android_local_manifest/android-17.0/manifest_brcm_rpi.xml
repo sync -c -j$(nproc)

# Integração do CarXust (opcional se você usar o script de build)
/workspace/projects/CarXust/rom/rpi4/integrate-carxust.sh \
  --aosp ~/aosp-rpi4 \
  --apk /workspace/projects/CarXust/app/build/outputs/apk/debug/app-debug.apk

source build/envsetup.sh
lunch aosp_rpi4_car-cp2a-userdebug
make bootimage systemimage vendorimage -j$(nproc)
./device/brcm/rpi4/mkimg.sh
```

A imagem final fica em:

```text
~/aosp-rpi4/out/target/product/rpi4/RaspberryVanillaAOSP17-<data>-rpi4_car.img
```

### Como o app é integrado

`integrate-carxust.sh` copia para `device/brcm/rpi4/carxust/`:

- `CarXust.apk` (pré-compilado);
- `Android.bp` com `android_app_import` (`certificate: "platform"`,
  `privileged: true`) — o APK é **reassinado com a chave da plataforma**;
- `native/Android.bp` + `carxust-can.c` (binário em `/system/bin`);
- `overlay/carxust.rc`, `carxust-boot.sh`, permissões privadas e
  `default-permissions`;
- `carxust_product.mk`.

E adiciona ao final de `device/brcm/rpi4/aosp_rpi4_car.mk`:

```make
# CarXust
$(call inherit-product, device/brcm/rpi4/carxust/carxust_product.mk)
```

O script é idempotente. Para remover a integração, apague o diretório
`device/brcm/rpi4/carxust/` e a linha acima.

### Gravar o cartão

```bash
sudo ./rom/rpi4/flash-rpi4-sd.sh \
  --image ~/aosp-rpi4/out/target/product/rpi4/RaspberryVanillaAOSP17-*-rpi4_car.img \
  --device /dev/sdX
```

Confira o dispositivo com `lsblk` antes. A imagem tem ~15,4 GB; use cartão de
16 GB ou maior. O script recusa gravar em partição, dispositivo montado ou no
disco do sistema.

### Verificação pós-boot

```bash
adb devices
adb shell getprop ro.build.characteristics      # deve conter automotive
adb shell pm path com.carxust                    # /system/priv-app/CarXust/...
adb shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME
adb shell getprop persist.carxust.integrated      # 1
adb shell ls /system/etc/init/carxust.rc
adb shell dumpsys display | grep -i -E "hdmi|display"
```

No Raspberry Pi 4, o produto car já inclui
`android.software.activities_on_secondary_displays.xml`, necessário para o
cluster. O `display_settings.xml` do device reserva a porta HDMI1 para o
cluster.

## Orange Pi Zero 3

O BSP Android 12 do Orange Pi Zero 3 **não é AAOS**: não há `CarService` nem
VHAL. O app roda como launcher privilegiado e usa OBD-II, SocketCAN ou
simulador.

### Opção A — instalar via ADB (recomendada)

Requisitos: build `userdebug`/`eng`, ADB root, `/system` remontável.

```bash
./rom/orangepi-zero3/install-app.sh \
  --apk app/build/outputs/apk/debug/app-debug.apk
```

Se `adb remount` falhar:

```bash
adb root
adb disable-verity
adb reboot
adb root
adb remount
```

Depois repita o script. Para instalar o helper CAN, copie um binário arm64
para `rom/native/carxust-can` antes de rodar o script (veja abaixo).

### Opção B — repack de system.img

```bash
./rom/orangepi-zero3/repack-system.sh \
  --system /caminho/system.img \
  --apk app/build/outputs/apk/debug/app-debug.apk \
  --out /caminho/system-carxust.img
```

O script:

- converte imagem sparse (`simg2img`) quando necessário;
- monta a imagem com loop device (`sudo`);
- detecta `/system` na raiz ou como subdiretório;
- copia APK, overlay e helper opcional;
- ajusta dono/permissões, desmonta e roda `e2fsck`.

**Cuidados:**

- Se o BSP usa `super.img`/dynamic partitions, `system.img` isolado pode não
  ser suficiente; prefira ADB ou integre no build do BSP.
- AVB/verified boot pode rejeitar a imagem modificada. Desative a verificação
  (`adb disable-verity`, `fastboot --disable-verification flash vbmeta`) ou
  re-assine o vbmeta conforme o BSP.
- Rótulos SELinux de arquivos novos podem ficar ausentes. O
  `carxust-boot.sh` executa `restorecon` no boot, mas o `carxust.rc` só será
  lido se estiver rotulado corretamente. Teste com `getenforce` e
  `dmesg | grep avc`. Se necessário, rode `restorecon` manualmente via ADB
  antes de reiniciar.
- Faça backup do cartão original.

### Opção C — integrar no build do BSP

Se você compila o Android 12 do Zero 3 a partir do código da Orange Pi/
Allwinner:

1. Adicione o APK a um diretório de prebuilts do produto (por exemplo,
   `device/orangepi/zero3/carxust/`).
2. Crie um `Android.bp` com `android_app_import` (sem `certificate:
   "platform"` se não houver chave de plataforma no BSP; nesse caso o app
   continua funcionando, mas perde permissões privilegiadas).
3. Inclua o módulo em `PRODUCT_PACKAGES` do produto.
4. Copie `carxust.rc`, `carxust-boot.sh` e os XMLs de permissão para
   `/system/etc/...` via `PRODUCT_COPY_FILES`.
5. Compile e grave a imagem conforme a documentação do BSP.

O BSP usa `longan`/scripts próprios; os caminhos exatos variam por versão.
Valide com `getprop ro.build.version.release` e `getprop ro.build.type` antes
de assumir que `adb root` está disponível.

### Compilar o helper CAN para arm64

O helper é opcional. Em um host com toolchain cruzada:

```bash
aarch64-linux-gnu-gcc -O2 -Wall -Wextra -D_GNU_SOURCE \
  -o rom/native/carxust-can rom/native/carxust-can.c
file rom/native/carxust-can   # ELF 64-bit ARM aarch64
```

Também é possível compilar dentro do AOSP com o `Android.bp` fornecido
(`m carxust-can`).

## Solução de problemas de build

- **`repo: command not found`** — instale o `repo` e coloque no `PATH`.
- **Pouco espaço** — o sync completo e o build consomem centenas de GB;
  use `repo init --depth=1` e o `remove_projects.xml` do raspberry-vanilla
  para reduzir.
- **`lunch` falha** — confirme que o `manifest_brcm_rpi.xml` foi baixado para
  `.repo/local_manifests/` e que o sync terminou.
- **`mkimg.sh` falha** — verifique `sudo`, `kpartx`, loop devices e se
  `boot.img`, `system.img` e `vendor.img` existem em `out/target/product/rpi4`.
- **App não é HOME** — rode
  `adb shell cmd package set-home-activity com.carxust/.MainActivity` ou
  selecione o app em Configurações → Apps → Apps padrão.
- **Cluster não aparece** — confirme o segundo display em
  `adb shell dumpsys display` e as permissões de segunda tela.
- **VHAL sem dados** — no AAOS, verifique as permissões `android.car.*` e o
  `CarService`: `adb shell dumpsys car_service`.
