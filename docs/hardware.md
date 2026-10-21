# Hardware

## Placas

### Raspberry Pi 4B / 5

- **RPi 4B**: 2/4/8 GB, 2× micro-HDMI, Gigabit Ethernet, Wi-Fi/BT, USB 3.0.
- **RPi 5**: mais desempenho, 2× micro-HDMI, PCIe, requer fonte 5 V/5 A.
- Indicadas para o build **AAOS** com cluster em HDMI secundário.
- Armazenamento: cartão microSD A2 (32 GB+) ou SSD USB/NVMe (RPi5).
- Áudio: HDMI, USB DAC ou Bluetooth.

Numeração dos displays no Android: o primário costuma ser `port:0` e o
secundário `port:1`. Confirme com:

```bash
adb shell dumpsys display | grep -i -E "port:|DisplayDeviceInfo"
```

### Orange Pi Zero 3

- Allwinner H618, 1/2/4 GB, **1× HDMI**, Wi-Fi/BT (quando equipado), USB.
- Ótima para um painel único (7"/10") com OBD-II ou CAN.
- Para dois displays, use Raspberry Pi 4/5, Orange Pi 5 ou uma solução de
  display USB suportada pelo Android (a maioria dos adaptadores DisplayLink
  **não** funciona sem driver).
- Fonte 5 V/3 A; evite alimentar pelo computador de bordo sem proteção.

## Displays

- **Painel principal**: 7" a 10,1", HDMI, 1024×600 ou 1280×800, touch USB
  capacitivo. Evite painéis com touch resistivo se a interface for usada em
  movimento.
- **Cluster**: 7" a 12,3", HDMI. Em RPi4/5 use a segunda saída HDMI.
- Resolução: ajuste no firmware/BSP (`config.txt`, `cmdline.txt` ou
  `display_settings.xml`). Teste `wm size` e `wm density` via ADB.
- Brilho: prefira painéis com controle por PWM/backlight; o app não controla
  brilho de hardware.
- Toque no cluster raramente é necessário; o cluster é somente leitura.

## CAN

### Interface física

Opções comuns:

- **MCP2515 + transceptor** (TJA1050, SN65HVD230 ou MCP2562) via SPI;
- **MCP2515** em módulo SPI pronto (atenção ao cristal de 8 MHz/16 MHz e ao
  `interrupt` GPIO);
- **USB-CAN** (gs_usb/candleLight) — suporte depende do kernel Android;
- CAN nativo do SoC, quando disponível.

Ligação:

```text
MCU/veículo        Interface
CAN_H  ─────────── CAN_H
CAN_L  ─────────── CAN_L
GND    ─────────── GND (referência comum)
```

- Use par trançado e termine o barramento com **120 Ω** nas duas pontas.
- Em bancada, um terminador de 120 Ω já basta para curtas distâncias.
- Não conecte CAN_H/CAN_L invertidos; verifique com multímetro/analisador.
- Alimente o transceptor com 5 V ou 3,3 V conforme o módulo (o MCP2515 é
  5 V; use level shifter se o SPI do SoC for 3,3 V).

### OBD-II

- Conector OBD-II: CAN High no pino **6**, CAN Low no pino **14**, terra no
  pino **4/5**, +12 V no pino **16**.
- Para leitura somente, use um ELM327 na porta OBD em vez de soldar no
  barramento.
- **Nunca** transmita frames de controle (direção, freio, acelerador) sem
  conhecer o protocolo e os limites do veículo.

## OBD-II Bluetooth

- Prefira adaptadores com **STN1110/STN2120** ou ELM327 genuíno; clones
  baratos falham em protocolos CAN mais novos.
- Bluetooth SPP (serial). O app usa o MAC configurado em Ajustes.
- Alcance típico de 5–10 m; mantenha o adaptador longe de fontes de ruído.
- O adaptador consome da porta OBD; em carros com bateria fraca, desconecte
  quando o veículo ficar parado por longos períodos.

## Áudio

- Saída HDMI (TV/monitor com alto-falantes), USB DAC ou Bluetooth A2DP.
- Em AAOS, o `car_audio_configuration.xml` do device define zonas/routing.
  Ajuste-o se o áudio tocar no dispositivo errado.
- Evite ligar a saída de fone diretamente em entrada de potência sem
  pré-amplificador/isolamento.

## Alimentação e instalação no veículo

- Converta 12 V para 5 V com um **DC-DC automotivo** (5 V/5 A para RPi5,
  5 V/3 A para RPi4/Zero 3), com fusível na entrada.
- Use proteção contra inversão de polaridade, surtos de ignição e
  "load dump". Módulos veiculares prontos (ex.: reguladores automotivos)
  são mais seguros que conversores de bancada.
- Chave de ignição/ACC: use um sinal de "acessorios" para ligar/desligar via
  relé ou módulo de atraso; não corte a alimentação abruptamente durante
  gravação do cartão.
- Aterre a placa no mesmo ponto de terra do chassi, evitando loops.
- Prenda os cabos e use conectores automotivos; vibração solta contatos.

> Este projeto é para uso em bancada ou instalação auxiliar. Ele **não**
> substitui instrumentos obrigatórios, airbags, ABS ou ECU. Qualquer
> integração que interfira no barramento do veículo é de responsabilidade do
> instalador.
