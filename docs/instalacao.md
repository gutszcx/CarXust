# Instalação e uso

## 1. Requisitos

- Android 10 (API 29) ou superior; recomendado Android 11+.
- Para o cluster em segunda tela: duas saídas de vídeo reconhecidas pelo
  Android (Raspberry Pi 4/5, por exemplo).
- Para OBD-II: adaptador ELM327/STN1110 Bluetooth SPP.
- Para CAN: interface SocketCAN no kernel (`can0`) e root, ou uma ROM CarXust.

## 2. Instalar o APK

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Também é possível copiar o APK para o dispositivo e instalar manualmente
(permita "instalar apps desconhecidos" para o gerenciador de arquivos).

### Permissões

Na primeira abertura o app solicita:

- **Localização** — GPS do painel/mapa;
- **Bluetooth** (Android 12+) — conexão com o ELM327;
- **Notificações** (Android 13+) — serviço em primeiro plano;
- **Mídia/áudio** (Android 13+) — músicas locais;
- **Telefone** — intent de discagem.

Se alguma for negada, o app continua funcionando; a fonte correspondente
mostra o erro em **Diagnóstico**.

### Transferência automática de permissões (ROM)

Em builds de ROM, `rom/overlay/carxust-default-permissions.xml` concede as
permissões runtime no primeiro boot. Em instalação por ADB, conceda
manualmente ou use:

```bash
adb shell pm grant com.carxust android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.carxust android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.carxust android.permission.BLUETOOTH_SCAN
adb shell pm grant com.carxust android.permission.POST_NOTIFICATIONS
adb shell pm grant com.carxust android.permission.READ_MEDIA_AUDIO
```

## 3. Primeiro uso

1. Abra **Ajustes** (ícone de engrenagem na barra inferior).
2. Escolha a **fonte de dados**:
   - `Simulador` — não precisa de hardware;
   - `OBD-II` — informe o MAC do adaptador;
   - `SocketCAN` — interface (`can0`) e bitrate (500000);
   - `Android Automotive` — apenas em AAOS com permissões `android.car.*`.
3. Ajuste unidades, cor de destaque, nome do motorista e limite de velocidade.
4. Ative o **cluster** e escolha o display secundário, se houver.
5. Volte ao painel e confirme o status **ONLINE** no topo.

As preferências são salvas automaticamente.

## 4. OBD-II

1. Ligue a ignição e conecte o ELM327 na porta OBD-II.
2. No Android, pareie o adaptador em **Configurações → Bluetooth**. O PIN
   costuma ser `1234` ou `0000`.
3. Descubra o MAC:

   ```bash
   adb shell dumpsys bluetooth_manager | grep -i -A2 elm
   ```

   Ou use um app de terminal Bluetooth. O endereço tem o formato
   `AA:BB:CC:DD:EE:FF`.
4. Em **Ajustes → OBD-II**, cole o MAC e selecione a fonte `OBD-II`.
5. Abra **Diagnóstico** para ver os PIDs recebidos e falhas de conexão.

O app inicializa o adaptador com `ATZ`, `ATE0`, `ATL0`, `ATS0`, `ATH0` e
`ATSP0` (detecção automática de protocolo).

## 5. SocketCAN

A interface precisa existir no kernel (driver MCP2515, CAN nativo etc.) e o
bitrate deve corresponder ao barramento do veículo:

```bash
su
ip link set can0 down
ip link set can0 up type can bitrate 500000
candump can0
```

O `CanSource` tenta, nesta ordem:

1. `candump -L can0` (can-utils);
2. `/system/bin/carxust-can can0` (helper incluído na ROM);
3. configuração automática via `su -c "ip link set ..."`.

Em **Ajustes → SocketCAN**, informe `can0` e `500000`. Ative "mostrar frames
crus" para ver as linhas no diagnóstico.

> Sem root, o app não consegue abrir sockets CAN nem configurar a interface.
> Nesse caso use OBD-II, simulador ou a fonte Android Automotive.

## 6. Cluster em segunda tela

1. Conecte o segundo display antes de abrir o app (HDMI1 no Raspberry Pi 4).
2. Em **Ajustes**, ative **Cluster**.
3. Se houver mais de um display, escolha o ID correto.
4. O status aparece no rodapé de Ajustes: `Cluster ativo • HDMI Screen`.
5. Use **Prévia do cluster** para testar na tela principal.

No Raspberry Pi 4 com AAOS, o `display_settings.xml` da ROM já reserva a
porta HDMI1 para o cluster (`dontMoveToTop="true"`). Em Android comum, o
segundo display precisa ser reconhecido e permitir `Presentation`
(`android.software.activities_on_secondary_displays`).

## 7. Inicialização automática

- O `VehicleService` sobe junto com o app e mantém a telemetria ativa.
- `BootReceiver` inicia o serviço no boot se **Iniciar com o sistema** estiver
  ligado.
- Na ROM, `carxust-boot.sh` define o CarXust como HOME e desativa a lockscreen.

Para definir manualmente como launcher padrão:

```bash
adb shell cmd package set-home-activity com.carxust/.MainActivity
```

## 8. Atualização

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Em ROM, reinstale o APK em `/system/priv-app/CarXust/CarXust.apk` e reinicie.
As preferências são preservadas (mesmo `applicationId`).

## 9. Desinstalação

```bash
adb uninstall com.carxust
```

Em ROM, remova `/system/priv-app/CarXust`, o XML de permissões e o
`carxust.rc`; depois reinicie.

## 10. ROM

- Raspberry Pi 4/5: [rom-build.md](rom-build.md#raspberry-pi-45-aaos).
- Orange Pi Zero 3: [rom-build.md](rom-build.md#orange-pi-zero-3).

## Problemas comuns

Consulte [solucao-de-problemas.md](solucao-de-problemas.md).
