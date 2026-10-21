# Solução de problemas

## O app fecha ao abrir

1. Confira a versão mínima: Android 10 (API 29).
2. Veja o log:

   ```bash
   adb logcat -s AndroidRuntime CarXustVhal
   ```

3. Se a falha for em `android.car`, selecione outra fonte em Ajustes (o app
   tenta VHAL por reflexão e deve cair para a fonte configurada).
4. Limpe os dados e reabra:

   ```bash
   adb shell pm clear com.carxust
   ```

## Nenhum dado no painel

- Verifique o status no topo: `ONLINE` ou `SEM SINAL`.
- Em **Diagnóstico**, confira a mensagem da fonte.
- `Simulador` deve funcionar sempre; se não funcionar, o problema é no app.
- OBD: confirme pareamento, MAC e ignição ligada.
- CAN: confirme `can0` ativa e bitrate correto.
- VHAL: apenas em AAOS com permissões `android.car.*`.

## OBD não conecta

```bash
adb shell dumpsys bluetooth_manager | grep -i -A3 elm
adb logcat -s CarXustVhal
```

- O adaptador precisa suportar **Bluetooth SPP** (não BLE-only).
- Remova o pareamento antigo e pareie de novo (PIN `1234`/`0000`).
- Confirme `BLUETOOTH_CONNECT` concedida (Android 12+).
- Teste com outro app de OBD para isolar o adaptador.
- Adaptadores clones podem não responder a `ATSP0`; teste `ATSP3`/`ATSP6`
  manualmente em um terminal serial.

## CAN sem permissão

Sintomas: `Permission denied`, `SocketCAN indisponível`, `candump não
disponível`.

- O app precisa de root para abrir SocketCAN.
- Instale `can-utils` ou o helper `carxust-can`.
- Configure a interface:

  ```bash
  su
  ip link set can0 down
  ip link set can0 up type can bitrate 500000
  ```

- Verifique o driver:

  ```bash
  dmesg | grep -i can
  ls /sys/class/net
  ```

- Em AAOS, prefira a fonte **Android Automotive** (VHAL), que não exige root.

## Frames aparecem no diagnóstico, mas o painel não muda

- O frame pode estar em outro ID/escala. Compare com
  [can-obd.md](can-obd.md).
- Ative "mostrar frames crus" e valide o formato (`can0 100#...`).
- O decoder ignora frames com menos bytes que o esperado.
- Para ADAS, `0x200` e `0x201` são mesclados; verifique ambos.

## Cluster não aparece

- Confirme a segunda tela:

  ```bash
  adb shell dumpsys display | grep -i -E "port:|HDMI|displayId"
  ```

- Ative o cluster em Ajustes e escolha o display correto.
- Em RPi4, o HDMI1 precisa estar conectado **antes** do boot para o Android
  criar o display.
- Verifique se a ROM inclui
  `android.software.activities_on_secondary_displays.xml`.
- `ClusterPreviewActivity` funciona sem segunda tela; se ela abrir, o
  problema é a detecção/`Presentation`.
- Em Android comum, algumas ROMs bloqueiam `Presentation`; teste em outro
  dispositivo.

## O app não inicia como HOME

```bash
adb shell cmd package set-home-activity com.carxust/.MainActivity
```

Se o comando falhar:

- confirme que o app está instalado e tem a categoria `HOME`;
- em ROM, verifique `/system/etc/init/carxust.rc` e o log do init:

  ```bash
  adb shell dmesg | grep -i carxust
  adb logcat | grep -i carxust
  ```

- O `carxust-boot.sh` roda como root via init; se o SELinux bloquear, use a
  definição manual acima.

## Permissões privilegiadas não aplicadas

- O APK precisa estar em `/system/priv-app/CarXust/` (não em `/data/app`).
- O XML deve estar em `/system/etc/permissions/privapp-permissions-carxust.xml`.
- Confira:

  ```bash
  adb shell dumpsys package com.carxust | grep -i -A20 "requested permissions"
  adb logcat | grep -i privapp
  ```

- Em ROM, o módulo `android_app_import` usa `certificate: "platform"` e
  `privileged: true`; não troque o APK por um assinado com outra chave sem
  re-assinar.
- Se houver erro de allowlist, adicione a permissão ao XML e reinicie.

## Mídia local vazia

- Conceda `READ_MEDIA_AUDIO` (Android 13+) ou `READ_EXTERNAL_STORAGE`
  (até Android 12).
- O MediaStore só indexa arquivos de áudio com metadados; copie para
  `Music/` e aguarde a indexação.
- Rádios de internet exigem rede; streams SomaFM podem ser bloqueados por
  firewall/DNS.

## GPS não fixa

- Conceda localização precisa.
- Teste ao ar livre; em ambiente fechado o GPS pode não fixar.
- Em placas sem GPS, use um receptor USB/Bluetooth compatível ou um mock
  location para demonstração.
- O app mostra `hasFix` e precisão em **Diagnóstico**.

## Notificação do serviço não aparece

- Conceda `POST_NOTIFICATIONS` (Android 13+).
- O canal `carxust_vehicle` tem importância baixa; verifique se não foi
  silenciado nas configurações do sistema.
- O serviço é `specialUse`; algumas ROMs restritivas matam FGS. Adicione o
  app à lista de exceções de bateria.

## Erros de build

- **`Unresolved reference` em `android.car`** — não adicione dependência;
  o app usa reflexão. Verifique se o código novo importou `android.car`.
- **`AAPT2`/SDK não encontrado** — configure `ANDROID_HOME` e instale a API
  35/build-tools.
- **`compileSdk 35` requer AGP 8.6+** — o projeto usa 8.7.3.
- **Assinatura** — o build de release sem keystore gera
  `app-release-unsigned.apk`; para instalar via ADB use o APK de debug ou
  configure um keystore.
- **Gradle lento/memória** — ajuste `org.gradle.jvmargs` em
  `gradle.properties` (padrão atual: 2 GB; use 4 GB em hosts maiores).

## ROM não dá boot após repack

- A imagem pode ter AVB/verity ativo; desative a verificação ou re-assine o
  vbmeta.
- Rótulos SELinux ausentes podem impedir o init de ler o `carxust.rc`; rode
  `restorecon` via ADB ou use a instalação por ADB em vez de repack.
- Verifique a imagem com `e2fsck -f` e o tamanho das partições.
- Tenha sempre o cartão original para recuperação.

## Ainda com problema?

Colete:

```bash
adb shell getprop > props.txt
adb logcat -d > logcat.txt
adb shell dumpsys display > display.txt
adb shell dumpsys package com.carxust > package.txt
```

e descreva placa, versão do Android, fonte de dados e o que apareceu em
**Diagnóstico**.
