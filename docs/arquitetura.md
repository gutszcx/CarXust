# Arquitetura

## Visão geral

O CarXust é um único APK Android com três camadas principais:

```text
┌─────────────────────────────────────────────────────────────┐
│ UI (Jetpack Compose)                                        │
│  MainActivity ── telas ── NavRail ── widgets                │
│  ClusterPresentation (segunda tela)                         │
└───────────────▲─────────────────────────▲───────────────────┘
                │ StateFlow               │ StateFlow
┌───────────────┴──────────┐   ┌──────────┴───────────────────┐
│ VehicleRepository        │   │ MediaRepository              │
│  estado + fonte ativa    │   │  ExoPlayer + MediaStore      │
└───────────────▲──────────┘   └──────────────────────────────┘
                │ VehiclePatch
┌───────────────┴──────────────────────────────────────────────┐
│ Fontes: Mock | OBD-II | SocketCAN | CarProperty (VHAL) + GPS │
└──────────────────────────────────────────────────────────────┘
```

- `CarXustApp` cria as dependências globais (`CarXustPrefs`,
  `VehicleRepository`, `MediaRepository`) e inicia o `VehicleService`.
- `MainActivity` observa os `StateFlow` e desenha a interface.
- `DisplayRouter` detecta displays secundários e abre um
  `ClusterPresentation` com o cluster.
- As fontes de telemetria rodam em corrotinas de I/O e emitem
  `VehiclePatch`; o repositório aplica o patch de forma atômica no estado.

## Estado do veículo

`VehicleState` é um `data class` imutável com todos os dados exibidos:
velocidade, RPM, marcha, bateria, autonomia, potência, clima, pneus, portas,
GPS, ADAS, origem dos dados etc.

`VehiclePatch` contém apenas os campos que uma fonte deseja atualizar
(todos nulos por padrão). `applyPatch()` faz o merge campo a campo e atualiza
`lastUpdateMs`. Isso permite que diferentes fontes (CAN + GPS, por exemplo)
atualizem partes independentes do estado sem sobrescrever o restante.

```kotlin
val patch = VehiclePatch(speedKph = 42f, gear = Gear.DRIVE)
val novo = estado.applyPatch(patch)
```

O estado é publicado em `VehicleRepository.state: StateFlow<VehicleState>`,
portanto a UI e o cluster sempre leem o mesmo snapshot.

### ADAS

As mensagens `0x200` (geometria) e `0x201` (flags) são parciais. O
`CanSource` mantém um `AdasState` acumulado e usa `mergeAdas()` antes de
publicar o patch, evitando que uma mensagem apague os dados da outra.

## Fontes de telemetria

Todas implementam `VehicleSource`:

```kotlin
interface VehicleSource {
    val name: String
    fun start(onPatch: (VehiclePatch) -> Unit, onStatus: (SourceStatus) -> Unit)
    fun stop()
}
```

| Fonte | Arquivo | Dependência | Uso |
|---|---|---|---|
| Simulador | `MockVehicleSource.kt` | nenhuma | demonstração/testes |
| OBD-II | `ObdSource.kt` + `ObdPids.kt` | Bluetooth SPP + ELM327 | carros com porta OBD |
| SocketCAN | `CanSource.kt` + `CanDecoder.kt` | `candump` ou `carxust-can` + root | instalação automotiva |
| VHAL | `CarPropertySource.kt` | AAOS + `android.car` | ROM Raspberry Pi car |
| GPS | `GpsSource.kt` | `LocationManager` | mapa/posição |

O `VehicleRepository` observa `CarXustPrefs.config` e reinicia a fonte quando
tipo, interface, bitrate, MAC ou modo raw mudam (`collectLatest` +
`distinctUntilChanged`). O GPS é independente e pode rodar junto com qualquer
fonte.

O `ObdSource` reconecta automaticamente a cada 5 s; o `CanSource` tenta
`candump` e, se necessário, o helper `carxust-can`, configurando `can0` com
`ip link` (via `su` quando disponível). O `CarPropertySource` usa reflexão
para o mesmo APK funcionar fora do AAOS.

## Preferências

`CarXustPrefs` encapsula `SharedPreferences` (`carxust_prefs`) e expõe
`StateFlow<UserConfig>`. Campos:

- unidades (km/mi), cluster ligado/desligado e display escolhido;
- fonte (`MOCK`, `OBD`, `CAN`, `CAR_PROPERTY`), MAC OBD, interface/bitrate CAN;
- cor de destaque, nome do motorista, autostart;
- exibição de frames CAN crus e limite de velocidade do painel.

`update { it.copy(...) }` grava de forma síncrona em memória e assíncrona no
disco (`apply()`), notificando a UI na hora.

## Interface

`MainActivity` usa uma única Activity com Compose e navegação por estado
(`CarScreen`), sem fragments. Telas:

- `DashboardScreen` — painel principal;
- `NavigationScreen` — mapa esquemático, GPS e coordenadas;
- `MediaScreen` — player, fila, músicas locais e rádios;
- `ClimateScreen` — temperatura, ventilador, assentos;
- `PhoneScreen` — discagem via intent;
- `AppsScreen` — apps instalados;
- `DiagnosticsScreen` — status das fontes, frames CAN e sensores;
- `SettingsScreen` — configuração e informações do sistema.

Componentes compartilhados ficam em `ui/components` (top bar, nav rail,
widgets de clima/mídia, gráficos do veículo, mapa).

### Cluster

`DisplayRouter` registra um `DisplayManager.DisplayListener` e procura um
display que não seja o padrão e não esteja desligado. O display pode ser
fixado nas preferências (`clusterDisplayId`) ou o primeiro disponível é usado.
O cluster é uma `Presentation` (`ClusterPresentation`) que infla um
`ComposeView` com `ClusterScreen`, o mesmo `VehicleState` e o estado do player.

`MainActivity` também oferece `ClusterPreviewActivity` para visualizar o
cluster na tela principal (útil em bancada).

## Mídia

`MediaRepository` usa ExoPlayer (Media3) com:

- áudio local via `MediaStore.Audio` (título, artista, álbum, duração);
- rádios de internet (`RadioCatalog`, streams SomaFM);
- play/pause, próximo/anterior, seek, shuffle/repeat e volume;
- atualização de posição a cada 500 ms e tratamento de erros.

O player vive no processo da aplicação e continua tocando com a tela apagada
quando o serviço em primeiro plano está ativo.

## Serviço e boot

- `VehicleService`: foreground service `specialUse`, notificação persistente e
  `START_STICKY`, mantendo o processo vivo para telemetria/mídia.
- `BootReceiver`: em `BOOT_COMPLETED` ou `MY_PACKAGE_REPLACED`, inicia o
  serviço se `autoStart` estiver ligado.
- Em ROM, `carxust-boot.sh` (init) define o app como HOME, desativa lockscreen,
  ajusta rotação/immersive e sobe as interfaces CAN. Todas as ações são
  best-effort e não bloqueiam o boot (`exec_background`).

## Threading e erros

- UI: main thread; Compose observa `StateFlow` com
  `collectAsStateWithLifecycle`.
- Fontes: `Dispatchers.IO`; mídia: `Dispatchers.Main` (ExoPlayer).
- O repositório usa `SupervisorJob`, então a falha de uma fonte não derruba as
  demais.
- Erros de conexão viram `SourceStatus` e aparecem no topo/diagnóstico; o app
  nunca depende de uma fonte específica para abrir.

## Decisões e limites

- **Reflexão no VHAL** evita dependência de `android.car` no compileSdk e
  permite o mesmo APK em aparelhos comuns.
- **CAN proprietário** simplifica a integração com MCUs; para um carro real,
  adapte `CanDecoder` ao DBC da montadora.
- **Sem banco de dados**: preferências em SharedPreferences e mídia via
  MediaStore são suficientes para o escopo.
- **Sem mapas online**: o mapa é esquemático e usa GPS; integração com Google
  Maps/Navigation SDK exigiria chave e serviços proprietários.
