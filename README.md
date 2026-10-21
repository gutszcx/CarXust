# CarXust

Central multimídia e painel automotivo para Android, com interface inspirada
em veículos elétricos modernos. O mesmo APK roda em Android comum (tablet,
telefone, TV box) e em builds Android Automotive (AAOS) para Raspberry Pi 4/5
e Orange Pi Zero 3.

![status](https://img.shields.io/badge/vers%C3%A3o-1.0.0-red)
![Android](https://img.shields.io/badge/Android-10%2B%20(API%2029)-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF)

## Recursos

- **Painel do motorista** com velocidade, marcha, RPM, autonomia, bateria,
  consumo, temperatura, pressão dos pneus, portas, luzes e ADAS.
- **Cluster em segunda tela** via `Presentation`/`DisplayManager` (HDMI
  secundário do Raspberry Pi 4, por exemplo).
- **Multimídia** com Media3/ExoPlayer: músicas locais via MediaStore e rádios
  de internet pré-configuradas.
- **Clima**, **navegação** (mapa esquemático + GPS), **telefone** (intent de
  discagem), **aplicativos** instalados e **diagnóstico** em tempo real.
- **Quatro fontes de telemetria**:
  - `Simulador` (funciona sem hardware);
  - `OBD-II` via ELM327/STN1110 Bluetooth SPP;
  - `SocketCAN` via `candump`/`carxust-can` (500 kbit/s configurável);
  - `Android Automotive` via `CarPropertyManager` (VHAL, por reflexão).
- **ROM instalável** para Raspberry Pi 4 (AAOS, Android 17) e integração em
  BSP Android 12 do Orange Pi Zero 3.
- Interface em português, tema escuro, acentos configuráveis, unidades
  métricas ou imperiais, inicialização automática.

## Hardware suportado

| Placa | Android | Telas | Status da ROM |
|---|---|---|---|
| Raspberry Pi 4B (2/4/8 GB) | AAOS 17 (raspberry-vanilla) | 2× HDMI | build + integração por script |
| Raspberry Pi 5 | AAOS 17 (raspberry-vanilla) | 2× HDMI | build + integração por script |
| Orange Pi Zero 3 (1/2/4 GB) | Android 12 BSP (Allwinner) | 1× HDMI | instalação via ADB/repack |
| Tablet/celular/TV box | Android 10+ | 1 ou 2 | APK direto |

> O Orange Pi Zero 3 tem **apenas uma saída HDMI**. Para dois displays use
> Raspberry Pi 4/5, Orange Pi 5 ou um adaptador USB secundário suportado pelo
> Android. Veja [docs/hardware.md](docs/hardware.md).

## Início rápido (somente o app)

Pré-requisitos: JDK 17, Android SDK (API 35) e ADB.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No primeiro boot, conceda as permissões solicitadas (localização, Bluetooth,
notificações e mídia). Em **Ajustes → Fonte de dados**, escolha `Simulador`
para testar sem hardware. Para o cluster, conecte a segunda tela e ative
**Cluster** em Ajustes.

## ROM

### Raspberry Pi 4/5 (AAOS)

O build usa o projeto [raspberry-vanilla](https://github.com/raspberry-vanilla)
(Android 17) e exige host Linux x86_64, ~400 GB e 16 GB de RAM.

```bash
# 1. Gere o APK
./gradlew assembleDebug

# 2. Sincronize e compile o AOSP, integrando o CarXust
./rom/rpi4/build-rpi4-aosp.sh --apk app/build/outputs/apk/debug/app-debug.apk

# 3. Grave o cartão SD (CUIDADO: apaga o dispositivo)
sudo ./rom/rpi4/flash-rpi4-sd.sh \
  --image ~/aosp-rpi4/out/target/product/rpi4/RaspberryVanillaAOSP17-*-rpi4_car.img \
  --device /dev/sdX
```

### Orange Pi Zero 3 (Android 12 BSP)

Com um build `userdebug/eng` e ADB root:

```bash
./rom/orangepi-zero3/install-app.sh --apk app/build/outputs/apk/debug/app-debug.apk
```

Se o BSP não permitir `adb remount`, use
`rom/orangepi-zero3/repack-system.sh` ou integre o APK no build do BSP. Detalhes
em [docs/rom-build.md](docs/rom-build.md).

## Fontes de dados

### CAN proprietário (CarXust spec v1)

IDs de 11 bits, 500 kbit/s. Exemplo:

```text
can0 100#0DAC0BB80300   # 35,00 km/h, 3000 rpm, marcha D
can0 101#01F4006EFF9C00 # 50,0% SOC, 110 km, -10,0 kW
can0 104#1E1D1C1B       # pressões 5,0 / 4,9 / 4,8 / 4,7 bar
```

Tabela completa em [docs/can-obd.md](docs/can-obd.md). Para um veículo real,
mapeie os IDs da sua ECU em `CanDecoder.kt` ou use o VHAL do AAOS.

### OBD-II

- Adaptador ELM327/STN1110 com Bluetooth SPP.
- PIDs suportados: `0C` RPM, `0D` velocidade, `05` coolant, `11` acelerador,
  `2F` combustível, `42` tensão, `46` temperatura externa e `5B` bateria.
- Selecione o MAC do adaptador em **Ajustes → OBD-II**.

## Estrutura do projeto

```text
app/src/main/java/com/carxust/
├── core/        preferências, roteamento de displays, formatação
├── media/       Media3/ExoPlayer, MediaStore, rádios
├── service/     foreground service + autostart
├── ui/          Compose: telas, cluster, componentes e tema
├── vehicle/     estado, fontes (CAN/OBD/VHAL/GPS/mock) e decoders
├── MainActivity.kt
└── CarXustApp.kt

rom/
├── common/      funções compartilhadas
├── overlay/     init.rc, boot script, permissões, product.mk, Android.bp
├── native/      helper SocketCAN em C + Android.bp
├── rpi4/        build, integração e gravação da ROM
└── orangepi-zero3/  instalação ADB e repack de system.img

docs/            arquitetura, instalação, ROM, hardware, CAN/OBD e troubleshooting
```

## Documentação

- [Arquitetura](docs/arquitetura.md)
- [Instalação e uso](docs/instalacao.md)
- [Build da ROM](docs/rom-build.md)
- [Hardware](docs/hardware.md)
- [CAN e OBD-II](docs/can-obd.md)
- [Desenvolvimento e testes](docs/desenvolvimento.md)
- [Solução de problemas](docs/solucao-de-problemas.md)

## Limitações conhecidas

- Não há imagem de ROM pré-compilada no repositório; o build AOSP é feito na
  sua máquina.
- O cluster em segunda tela depende do Android expor o display (RPi4: HDMI1).
  Em Android comum, a segunda tela precisa ser reconhecida pelo sistema.
- `carxust-can` e a configuração de `can0` precisam de root/build de ROM.
  Em AAOS, prefira a fonte **Android Automotive**.
- O helper CAN é opcional; sem ele o app funciona com OBD-II ou simulador.
- O projeto não substitui os sistemas de segurança do veículo. Use em bancada
  ou em instalação devidamente isolada e validada.
