# CAN e OBD-II

## Protocolo CAN do CarXust (spec v1)

- CAN clássico, **11 bits**, **500 kbit/s** (configurável), big-endian.
- IDs proprietários; adapte `CanDecoder.kt` ao DBC da sua ECU/MCU.
- Frames desconhecidos são ignorados; o app aceita `candump` ou o helper
  `carxust-can`.

### 0x100 — Powertrain

| Bytes | Campo | Escala | Exemplo |
|---|---|---|---|
| 0–1 | Velocidade | uint16, 0,01 km/h | `0DAC` = 35,00 km/h |
| 2–3 | RPM | uint16, 1 rpm | `0BB8` = 3000 rpm |
| 4 | Marcha | 0=P, 1=R, 2=N, 3=D, 4=S | `03` = D |
| 5 | Freio de estacionamento | 0/1 | `00` |

```text
can0 100#0DAC0BB80300
```

### 0x101 — Energia

| Bytes | Campo | Escala |
|---|---|---|
| 0–1 | SOC | uint16, 0,1 % |
| 2–3 | Autonomia | uint16, 1 km |
| 4–5 | Potência | int16, 0,1 kW (positivo = consumo) |
| 6 | Carregando | 1 = sim |

```text
can0 101#01F4006EFF9C00   # 50,0% • 110 km • -10,0 kW • não carregando
```

### 0x102 — Clima

| Byte | Campo | Escala |
|---|---|---|
| 0 | Temperatura externa | int8, offset -40 °C |
| 1 | Temperatura da cabine | int8, offset -40 °C |
| 2 | Temperatura da bateria | int8, offset -40 °C (não usado na UI) |
| 3 | Ventilador | 0–7 |
| 4 | Clima ligado | 0/1 |

### 0x103 — Carroceria

| Byte | Campo | Bits |
|---|---|---|
| 0 | Portas | bit0 FL, bit1 FR, bit2 RL, bit3 RR, bit4 porta-malas, bit5 capô |
| 1 | Luzes | bit0 farol, bit1 farol alto |
| 2 | Seta | 0=off, 1=esquerda, 2=direita, 3=pisca-alerta |
| 3 | Cinto | 1=afivelado |
| 4 | Pressão válida | reservado |

### 0x104 — Pneus

| Byte | Campo | Escala |
|---|---|---|
| 0 | Dianteiro esquerdo | 0,1 bar acima de 2,0 bar |
| 1 | Dianteiro direito | idem |
| 2 | Traseiro esquerdo | idem |
| 3 | Traseiro direito | idem |

`1E` = 30 → 5,0 bar.

### 0x200 — ADAS (geometria)

| Bytes | Campo | Escala |
|---|---|---|
| 0–1 | Offset de faixa | int16, 0,001 m |
| 2–3 | Distância do carro à frente | uint16, 0,1 m |
| 4–5 | Velocidade do carro à frente | uint16, 0,1 km/h |
| 6–7 | Cruise set | uint16, 0,1 km/h |

### 0x201 — ADAS (flags)

| Bit | Campo |
|---|---|
| 0 | Lane assist ativo |
| 1 | Carro à frente presente |
| 2 | Aviso de colisão |
| 3 | Cruise ativo |
| 4 | Ponto cego esquerdo |
| 5 | Ponto cego direito |

Mensagens `0x200` e `0x201` são parciais e mescladas pelo `CanSource`.

### Formato de entrada aceito

```text
(1712345678.123456) can0 100#0DAC0BB80300
can0 100#0DAC0BB80300
can0 100 [8] 0D AC 0B B8 03 00 00 00
```

O parser também aceita IDs de 29 bits (até 8 dígitos hexadecimais), embora o
decoder use apenas os IDs de 11 bits acima.

## Configurar SocketCAN

```bash
su
ip link set can0 down
ip link set can0 up type can bitrate 500000
ip -details link show can0
candump can0
```

Instale `can-utils` para `candump`/`cansend` (não incluído no Android por
padrão). Na ROM CarXust, o helper `/system/bin/carxust-can` faz a leitura sem
dependências:

```bash
carxust-can can0
```

O app tenta `candump -L can0` e, se não existir, `carxust-can can0`; também
tenta configurar a interface via `su -c "ip link set ..."`.

### Compilar o helper

No AOSP (recomendado):

```bash
m carxust-can
adb push $OUT/system/bin/carxust-can /system/bin/
adb shell chmod 755 /system/bin/carxust-can
```

Fora do AOSP:

```bash
aarch64-linux-gnu-gcc -O2 -Wall -Wextra -D_GNU_SOURCE \
  -o carxust-can rom/native/carxust-can.c
```

### Ler sem gravar no barramento

O helper apenas lê (`read()`); não transmite nada. Para diagnósticos, use
`candump` com filtros para reduzir carga:

```bash
candump can0,100:7FF,101:7FF,102:7FF,103:7FF,104:7FF,200:7FF,201:7FF
```

## OBD-II (ELM327/STN1110)

### Inicialização usada pelo app

```text
ATZ     reset
ATE0    eco desligado
ATL0    linefeed desligado
ATS0    espaços desligados
ATH0    cabeçalhos desligados
ATSP0   protocolo automático
```

### PIDs suportados

| PID | Grandeza | Fórmula | Observação |
|---|---|---|---|
| `01 0C` | RPM | `(A×256+B)/4` | gasolina/diesel |
| `01 0D` | Velocidade | `A` km/h | — |
| `01 05` | Coolant | `A−40` °C | — |
| `01 11` | Acelerador | `A×100/255` % | — |
| `01 2F` | Combustível | `A×100/255` % | não usado como bateria |
| `01 42` | Tensão do módulo | `(A×256+B)/1000` V | — |
| `01 46` | Temperatura ambiente | `A−40` °C | — |
| `01 5B` | Vida da bateria | `A` % | híbridos/EV; pode não existir |

Respostas têm prefixo `41` (ex.: `41 0C 1A F8`). O app remove eco,
`SEARCHING...`, `NO DATA`, cabeçalhos e o terminador `>`; respostas inválidas
retornam `null` e não sobrescrevem o estado.

### Limitações

- Apenas PIDs de **resposta única** (single-frame). ISO-TP multi-frame não é
  implementado.
- O app não limpa códigos de falha nem executa atuadores.
- Adaptadores clones podem responder de forma inconsistente; teste em
  **Diagnóstico**.
- A porta OBD fornece 12 V; use adaptador com proteção e não deixe plugado
  indefinidamente com o carro desligado.

## Adaptar a um veículo real

1. Obtenha o **DBC** ou a documentação dos IDs da montadora.
2. Identifique as mensagens de velocidade, RPM, marcha, SOC/combustível,
   temperaturas, portas e ADAS.
3. Ajuste `CanDecoder.kt` (escalas, offsets, signed/unsigned, endianness).
4. Adicione testes em `app/src/test/java/com/carxust/VehicleCoreTest.kt` com
   frames reais gravados por `candump`.
5. Valide com o veículo **parado** e, depois, em ambiente controlado.
6. Para AAOS, prefira o VHAL (`CarPropertySource`) em vez de decodificar CAN
   diretamente; o VHAL já normaliza as propriedades do veículo.

> Nunca transmita comandos de direção, frenagem ou aceleração sem conhecer o
> protocolo e as proteções do veículo. O CarXust foi projetado para leitura e
> exibição.
