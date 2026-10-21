# Exemplo de firmware ESP32 (PlatformIO)

Este é um esqueleto mínimo para enviar quadros `CHAVE=VALOR` ao CarXust. 
Adapte a leitura para o seu adaptador (ELM327 via UART, ou CAN direto com `mcp_can`/`CAN`).

## platformio.ini

```ini
[env:esp32dev]
platform = espressif32
board = esp32dev
framework = arduino
monitor_speed = 115200
```

## main.cpp

```cpp
#include <Arduino.h>
#include <WiFi.h>

const char* AP_SSID = "CarXust-OBD";
const char* AP_PASS = ""; // deixe vazio para rede aberta

WiFiServer server(3333);

void setup() {
  Serial.begin(115200);
  WiFi.softAP(AP_SSID, AP_PASS);
  Serial.print("AP IP: ");
  Serial.println(WiFi.softAPIP());
  server.begin();
}

void sendFrame(WiFiClient &client) {
  client.println("HELLO");
  client.println("CAR=EXEMPLO");
  client.println("FW=1.0");
  // Envie apenas chaves que o veículo realmente possui
  // client.println("SPEED=62");
  // client.println("RPM=2150");
  // client.println("COOLANT=89");
  client.println("END");
}

void loop() {
  WiFiClient client = server.available();
  if (client) {
    Serial.println("Cliente conectado");
    while (client.connected()) {
      // Em produção, atualize a cada 250-500ms com dados reais
      sendFrame(client);
      delay(500);
    }
    Serial.println("Cliente desconectado");
    client.stop();
  }
  delay(10);
}
```

## Como testar

1. Grave no ESP32 (PlatformIO/Arduino IDE).
2. Conecte o painel CarXust à rede `CarXust-OBD` ou use o IP `192.168.4.1`.
3. Em Ajustes → Fonte de telemetria → ESP32, configure host `192.168.4.1` e porta `3333`.
4. Abra a tela **Sensores do painel do carro**. Os grupos enviados vão aparecer.
5. Para ocultar um grupo, basta não enviar nenhuma chave daquele grupo.
