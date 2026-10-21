# Hardware suportado

## Raspberry Pi 4B+
- Android Automotive OS (build alvo `carxust_rpi4_car` / imagem AAOS)
- Usa `CAR_PROPERTY` (VHAL) via `CarPropertySource`. Também suporta `CAN`, `OBD-II` (ELM327 Bluetooth) ou `ESP32` via Wi-Fi.
- Saídas HDMI/DSI, CAN via HATs (MCP2515/RS485), GPS, Bluetooth, Wi-Fi.

## Orange Pi Zero 3+
- Linha Android/Linux customizada para projetos CarPC/CarDashboard. Suporte via `CAN` (SocketCAN, `can0`) ou `ESP32` via Wi-Fi.
- Recomendado usar a fonte `CAN` em builds Linux/Android com SocketCAN, ou conectar o ESP32 pela rede Wi-Fi (fonte `ESP32`).
- Configurações: `Ajustes → Fonte de telemetria` escolhe `CAN` ou `ESP32`.

## ESP32 + OBD2
- Conecte o ESP32 no conector OBD2 do veículo (via cabo OBD-II ou adaptador). 
- O firmware lê dados via ELM327 (protocolo OBD-II) ou diretamente no barramento CAN do veículo.
- Transmite por TCP Wi-Fi para o painel CarXust. Veja `docs/esp32.md` para protocolo e tabela de sensores.
- Padrão: AP `192.168.4.1:3333` (modo ponto de acesso) ou conectar na rede local do carro.

## Telas / exibição
- Usa `OverlayDisplayManager` para espelhar/clonar em telas secundárias (Android Automotive/CarPC).
- Painel principal e secundário rodam na mesma instância com configuração por tela.
