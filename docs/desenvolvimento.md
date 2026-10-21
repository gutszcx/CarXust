# Desenvolvimento e testes

## Ambiente

- JDK 17;
- Android SDK com **API 35** e build-tools;
- Gradle 8.9 (wrapper incluso);
- `ANDROID_HOME`/`ANDROID_SDK_ROOT` configurados ou `local.properties` com
  `sdk.dir=/caminho/do/sdk`.

```bash
./gradlew assembleDebug      # APK de debug
./gradlew testDebugUnitTest  # testes JVM
./gradlew lintDebug          # lint (abortOnError = false)
```

O projeto usa AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01 e Java 17.

## Testes

`app/src/test/java/com/carxust/VehicleCoreTest.kt` cobre:

- conversão de velocidade e duração (`Formatters`);
- merge de `VehiclePatch` sem sobrescrever campos ausentes;
- parsing de frames `candump` (hash e bracket);
- decodificação de powertrain, pneus, carroceria, energia, clima e ADAS;
- frames desconhecidos/inválidos e round-trip de `CanDecoder.format`.

`ObdPidsTest.kt` cobre RPM, velocidade, acelerador, temperaturas, tensão,
combustível, vida da bateria, respostas ausentes e resposta negativa (`7F`).

Os testes são JVM puros: não precisam de emulador nem Android SDK além do
necessário para compilar o módulo.

## Estrutura de código

```text
app/src/main/java/com/carxust/
├── CarXustApp.kt          Application + dependências
├── MainActivity.kt        Activity Compose + navegação
├── ClusterPreviewActivity.kt
├── core/
│   ├── CarXustPrefs.kt    SharedPreferences + StateFlow<UserConfig>
│   ├── DisplayRouter.kt   detecção de display + Presentation
│   ├── ClusterPresentation.kt
│   └── Formatters.kt      unidades, tempo, pressão
├── media/MediaRepository.kt
├── service/               FGS + BootReceiver
├── ui/
│   ├── Navigation.kt      enum CarScreen
│   ├── cluster/           ClusterScreen
│   ├── components/        Chrome, widgets, MapCanvas, VehicleGraphics
│   ├── screens/           uma tela por arquivo
│   └── theme/             cores, tipografia, tema
└── vehicle/
    ├── VehicleState.kt    estado + patch + enums
    ├── VehicleSource.kt   interface
    ├── VehicleRepository.kt
    ├── MockVehicleSource.kt
    ├── GpsSource.kt
    ├── ObdSource.kt / ObdPids.kt
    ├── CanSource.kt / CanDecoder.kt
    └── CarPropertySource.kt
```

## Adicionar uma tela

1. Crie `ui/screens/MinhaScreen.kt` recebendo estado e callbacks (sem acessar
   o repositório diretamente, quando possível).
2. Adicione o item em `CarScreen` (`Navigation.kt`).
3. Trate a tela no `when` de `CarXustRoot` em `MainActivity.kt`.
4. Adicione o ícone no `NavRail` (`ui/components/Chrome.kt`).
5. Use os tokens de `ui/theme` em vez de cores hardcoded.

## Adicionar uma fonte de dados

1. Implemente `VehicleSource` (`start`, `stop`, `name`).
2. Emita `VehiclePatch` com apenas os campos que a fonte conhece.
3. Trate erros com `SourceStatus`; não lance exceção para a UI.
4. Registre o tipo em `VehicleSourceType` e no `when` de
   `VehicleRepository.restartSource`.
5. Exponha a configuração em `UserConfig`, `CarXustPrefs` e `SettingsScreen`.
6. Se possível, isole o parsing em um `object` puro (como `ObdPids`) e
   adicione testes JVM.

## Convenções

- Kotlin idiomático, `data class` imutável, `StateFlow` para estado.
- Sem lógica de negócio em Composables; callbacks sobem para a Activity/
  repositório.
- Textos de UI em português; nomes de código em inglês.
- Não adicione dependências sem verificar o `build.gradle.kts` e o tamanho do
  APK.
- Evite APIs ocultas do Android. O VHAL usa reflexão de forma isolada e
  tolerante a falhas.
- Novas permissões exigem atualização do `AndroidManifest.xml`, do XML de
  `privapp-permissions` (se privilegiada) e da documentação.

## Emulador e segunda tela

No emulador Android, ative **Opções do desenvolvedor → Simular telas
secundárias** para testar o cluster. No Raspberry Pi, use HDMI0 + HDMI1.
`ClusterPreviewActivity` mostra o cluster na tela principal sem display extra.

## Depuração

```bash
adb logcat -s CarXustVhal AndroidRuntime
adb shell dumpsys package com.carxust | grep -i permission
adb shell dumpsys display | grep -i port:
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN \
  -c android.intent.category.HOME
```

No app, **Diagnóstico** mostra fonte ativa, status, GPS, frames CAN e dados
brutos. Ative "mostrar frames crus" em Ajustes para ver o tráfego.

## Build da ROM

Scripts e validação: [rom-build.md](rom-build.md).

## Checklist antes de enviar mudanças

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- testar com fonte `Simulador` e, se possível, OBD/CAN;
- verificar cluster em segunda tela ou prévia;
- conferir permissões e autostart;
- atualizar docs quando mudar configuração, protocolo CAN ou ROM.
