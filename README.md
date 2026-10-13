# Rotine Water

Aplicativo Android, escrito em Java, para controlar o consumo diário de água. O usuário define uma meta em mililitros, registra cada volume bebido ao longo do dia e acompanha o progresso em um componente gráfico desenhado à mão com `Canvas`, o `WaterProgressView`.

Projeto acadêmico, feito para praticar views customizadas, animações e persistência local no Android.

## Funcionalidades

- **Meta diária editável**: pode ser alterada a qualquer momento, sem perder os registros já feitos no dia.
- **Registro de consumo**: informe o volume em ml e cada registro guarda o volume e o horário.
- **Gráfico circular animado**: o arco de progresso cresce suavemente a cada novo registro.
- **Alerta de meta ultrapassada**: o progresso pode passar de 100%. Nesse caso o arco e o texto ficam vermelhos, o gráfico mostra `+X ml` e a tela exibe uma faixa de alerta. O alerta some sozinho se a meta for aumentada.
- **Dois modos de leitura**: tocar no gráfico alterna entre percentual (`85%`) e absoluto (`1700/2000 ml`).
- **Remoção de registros**: um toque longo em um item da lista pede confirmação e remove o registro, recalculando o acumulado a partir dos registros restantes.
- **Persistência**: meta, registros e acumulado continuam disponíveis depois de fechar e reabrir o app. O modo de exibição do gráfico também sobrevive à rotação da tela.
- **Acessibilidade**: o gráfico expõe uma descrição (`contentDescription`) com o percentual e os valores em ml para leitores de tela.
- **Novo dia automático**: o app compara a data do sistema com a data dos registros salvos. Em um novo dia, os registros anteriores são descartados e a meta é mantida.

## Tecnologias

| Item | Detalhe |
|---|---|
| Linguagem | Java 11 |
| Plataforma | Android (`minSdk` 24, `compileSdk` e `targetSdk` 37) |
| Build | Gradle 9.6 (wrapper) e Android Gradle Plugin 9.4.1 |
| UI | AppCompat e Material 3 (tema claro) |
| Desenho | `View` customizada com `Canvas` e `Paint` |
| Animação | `ValueAnimator` |
| Persistência | `SharedPreferences` com lista de registros em JSON (`org.json`) |

## Estrutura

```
app/src/main/
├── AndroidManifest.xml
├── java/br/edu/ucsal/rotinewater/
│   ├── MainActivity.java        Tela única: liga a interface ao armazenamento e ao gráfico
│   ├── WaterProgressView.java   Componente gráfico reutilizável
│   ├── WaterEntry.java          Modelo de um registro (volume + horário)
│   └── WaterStorage.java        Leitura e gravação dos dados
└── res/
    ├── layout/activity_main.xml
    └── values/                  attrs, colors, strings e themes

app/src/androidTest/
└── java/br/edu/ucsal/rotinewater/WaterStorageTest.java   Testes da persistência
```

O fluxo de dados é sempre o mesmo: `MainActivity` → `WaterStorage` → `MainActivity` → `WaterProgressView`. O gráfico só recebe números e desenha, e o armazenamento não conhece a tela.

## Usando o `WaterProgressView` em outro layout

Atributos disponíveis em XML:

| Atributo | Tipo | Descrição |
|---|---|---|
| `app:progressColor` | color | Cor do arco de progresso |
| `app:textColor` | color | Cor dos textos |
| `app:maxValue` | integer | Meta diária em ml |

```xml
<br.edu.ucsal.rotinewater.WaterProgressView
    android:layout_width="200dp"
    android:layout_height="200dp"
    app:progressColor="#2196F3"
    app:textColor="#000000"
    app:maxValue="2000" />
```

Métodos públicos:

```java
waterView.setMaxValue(2000);      // meta em ml
waterView.setCurrentValue(1500);  // consumo acumulado em ml, anima até o novo valor
waterView.setProgress(75);        // define o progresso direto em percentual (aceita mais de 100)
waterView.setAbsoluteMode(true);  // mostra "1500/2000 ml" em vez do percentual
boolean over = waterView.isOverGoal();
```

## Como executar

Pré-requisitos: Android Studio recente (compatível com AGP 9.4), um JDK 17 ou superior para iniciar o Gradle e o Android SDK 37 instalado. O projeto fixa o JDK 25 para o daemon do Gradle (`gradle/gradle-daemon-jvm.properties`), que é baixado automaticamente quando necessário.

**Pelo Android Studio:** abra a pasta do projeto, aguarde a sincronização do Gradle e rode em um emulador ou aparelho com Android 7.0 (API 24) ou superior.

**Pela linha de comando:**

```bash
./gradlew assembleDebug
```

O APK é gerado em `app/build/outputs/apk/debug/`.

## Testes

Os testes de `WaterStorage` (persistência, total acima da meta, remoção de registros e descarte de dias anteriores) rodam em um aparelho ou emulador:

```bash
./gradlew connectedDebugAndroidTest
```

## Autor

[MadureiraHenrique](https://github.com/MadureiraHenrique)
