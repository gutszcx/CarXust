# ESP32 + OBD2 (CarXust)

O painel CarXust se conecta ao ESP32 por TCP (cliente → servidor). O firmware deve ler o barramento OBD-II/CAN e enviar quadros textuais `CHAVE=VALOR` terminados em `END`.

## Conexão

- **Modo AP (padrão)**: SSID `CarXust-OBD`, IP `192.168.4.1`, porta `3333`
- **Modo STA**: conectar na mesma rede Wi-Fi do painel. Configure IP/porta em *Ajustes → Fonte de telemetria → ESP32*.
- O app envia `HELLO CarXust\n` ao conectar. O firmware pode responder com `HELLO ...` (opcional).

## Protocolo

Um quadro por ciclo de atualização:

```text
HELLO
CAR=Marca/Modelo
FW=1.0
SPEED=62.5
RPM=2150
GEAR=D
COOLANT=89.0
FUEL=58
VOLT=13.9
CHG=1
MIL=0
LAMBDA=0.99
MAF=14.2
MAP=36
TPMS_FL=2.32
TPMS_FR=2.31
TPMS_RL=2.28
TPMS_RR=2.29
PAD_FRONT=65
PAD_REAR=48
STEER_ANGLE=2.5
YAW=0.4
LATACC=0.12
BSL=0
BSR=0
LANE=1
LANE_OFFSET=0.02
LEAD=1
LEAD_DIST=38
LEAD_SPEED=62
DOOR_FL=0
DOOR_FR=0
DOOR_RL=0
DOOR_RR=0
TRUNK=0
HOOD=0
SEATBELT_DRIVER=1
SEATBELT_PASS=1
LIGHTS=0
HIGHBEAM=0
RAIN=0
WIPER=0
PARK_FL=95
PARK_RR=140
FLUID_BRAKE=85
FLUID_COOL=92
FLUID_WASHER=60
AIRBAG=0
END
```

**Regras:**
- Linhas `CHAVE=VALOR`. Chaves em maiúsculas.
- Linhas vazias e comentários `#` podem ser ignoradas.
- **Não envie** chaves ausentes. Se o veículo não tem o sensor, simplesmente não envie aquela linha. O painel oculta o grupo.
- O quadro termina com `END`.
- Valores booleanos aceitos: `0/1`, `true/false`, `on/off`, `ligado/desligado`.

## Tabela de chaves

| Chave | Unidade | Grupo | Observação |
|---|---|---|---|
| `SPEED`, `VSS` | km/h | VSS | Usa `SPEED` se disponível. |
| `RPM` | rpm | RPM |  |
| `GEAR` | P,R,N,D,S | GEAR (do patch) |  |
| `COOLANT` | °C | Temperatura da água |  |
| `OILP` | bar | Pressão do óleo |  |
| `FUEL` | % | Nível de combustível |  |
| `MIL` | bool | Check engine / injeção | Check engine aceso. |
| `LAMBDA` | — | Injeção | Sonda lambda. |
| `MAF` | g/s | Injeção |  |
| `MAP` | kPa | Injeção |  |
| `WHEEL_FL/FR/RL/RR` | km/h | ABS | Rotação das rodas. |
| `YAW` | °/s | ESP / estabilidade | Guinada. |
| `LATACC` | m/s² | ESP / estabilidade | Aceleração lateral. |
| `TPMS_FL/FR/RL/RR` | bar | TPMS | Pressão dos pneus. |
| `PAD_FRONT`, `PAD_REAR` | % | Pastilha de freio |  |
| `STEER_ANGLE` | ° | Direção elétrica |  |
| `STEER_TORQUE` | Nm | Direção elétrica |  |
| `SEATBELT_DRIVER`, `SEATBELT_PASS` | bool | Cinto de segurança |  |
| `DOOR_FL/FR/RL/RR`, `TRUNK`, `HOOD` | bool | Portas / capô / mala |  |
| `BATT` | % | Bateria / alternador |  |
| `VOLT` | V | Bateria / alternador |  |
| `CHG` | bool | Bateria / alternador | Alternador carregando. |
| `LIGHTS` | bool | Luz e chuva | Faróis. |
| `HIGHBEAM` | bool | Luz e chuva | Luz alta. |
| `RAIN` | bool | Luz e chuva | Sensor de chuva. |
| `WIPER` | bool | Luz e chuva | Limpador ligado. |
| `BSL`, `BSR` | bool | Ponto cego / faixa | Ponto cego. |
| `LANE` | bool | Ponto cego / faixa | Faixa detectada. |
| `LANE_OFFSET` | m | Ponto cego / faixa | Desvio em relação à faixa. |
| `LEAD` | bool | Ponto cego / faixa | Carro à frente presente. |
| `LEAD_DIST` | m | Ponto cego / faixa | Distância ao veículo à frente. |
| `LEAD_SPEED` | km/h | Ponto cego / faixa | Velocidade do veículo à frente. |
| `PARK_FL/FR/RL/RR` | cm | Sensor de estacionamento |  |
| `FLUID_BRAKE` | % | Fluidos | Freio. |
| `FLUID_COOL` | % | Fluidos | Arrefecimento. |
| `FLUID_WASHER` | % | Fluidos | Lavador. |
| `AIRBAG` | bool | Airbag | `true` = airbag disparado. |
| `ODO` | km | Odômetro |  |
| `TRIP` | km | Viagem (opcional) |  |
| `CONSUMPTION` | kWh/100km ou valor bruto | Consumo médio (opcional) |  |
| `AMBIENT` | °C | Temperatura externa (opcional) |  |
| `TURN` | OFF/LEFT/RIGHT/HAZARD | Setas (opcional) |  |
| `PARKBRAKE` | bool | Freio de mão (opcional) |  |

## Sugestão de firmware (PlatformIO)

Crie `hardware/esp32/platformio.ini`:

```ini
[env:esp32dev]
platform = espressif32
board = esp32dev
framework = arduino
monitor_speed = 115200
lib_deps =
  sandeepmistry/CAN
  https://github.com/ayushsharma82/ElegantOTA
```

### Ideia de implementação

1. Conectar ao barramento CAN/ELM327.
2. Iniciar Wi-Fi AP (`CarXust-OBD`) ou STA.
3. Servidor TCP na porta `3333` (`WiFiServer`).
4. A cada 250–500 ms (ou ao receber dados), montar o quadro com as chaves disponíveis e enviar por `client.println`.
5. Enviar `HELLO` ao iniciar quadro ou apenas texto; o app processa `HELLO`, lê linhas até `END`.
6. Enviar `END` no final do quadro.

> O app CarXust já está pronto para esse protocolo. Basta implementar no ESP32.
