# OIDO — Entrena tu oído musical

Aplicación Android nativa para entrenar el reconocimiento de notas musicales por
sonido. El usuario escucha una nota de piano y elige la tecla correcta, sin ver
la respuesta antes de contestar.

> OIDO no es un curso de piano ni garantiza oído perfecto. Entrena
> **reconocimiento de tono, memoria auditiva e identificación consistente**.

---

## Características

- **Modo Entrenamiento**: 10, 20 o 30 preguntas por sesión. La nota se reproduce
  sola; el usuario responde tocando una de las 7 teclas (DO, RE, MI, FA, SOL, LA, SI).
- **Modo Exploración**: teclado libre donde sí se muestra el nombre de cada nota
  (para principiantes).
- **Retroalimentación inmediata**: "¡Correcto!" o "La nota era SOL", con la tecla
  correcta resaltada y el sonido repetido.
- **Progresión por niveles basada en rendimiento real** (precisión mínima sobre un
  número mínimo de ejercicios), no por cantidad de ejercicios:
  - Nivel 1: 2 notas (DO, SOL)
  - Nivel 2: 4 notas (DO, RE, MI, SOL)
  - Nivel 3: las 7 notas
  - Nivel 4: las 7 notas con menos ayuda (1 repetición por ejercicio)
- **Aprendizaje adaptativo**: prioriza notas débiles o poco practicadas, evitando
  siempre repetir la misma nota consecutivamente.
- **Estadísticas locales**: sesiones, precisión, racha diaria, mejor racha,
  precisión por nota, tiempo de respuesta y notas a mejorar.
- **100 % offline**: sin registro, sin backend, sin internet, sin APIs de pago.

---

## Requisitos

| Componente | Versión usada |
|---|---|
| JDK | 17 |
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.9 (wrapper incluido) |
| Kotlin | 2.0.21 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 (Android 7.0) |
| Jetpack Compose BOM | 2024.10.01 |

---

## Compilar

```powershell
# Windows (PowerShell)
.\gradlew.bat assembleDebug
```

```bash
# macOS / Linux
./gradlew assembleDebug
```

APK resultante:

```
app/build/outputs/apk/debug/app-debug.apk
```

Ejecutar los tests unitarios (52 tests: frecuencias, selección de notas,
aleatoriedad, evitación de repetición, validación, precisión, racha, progresión
de nivel y aprendizaje adaptativo):

```powershell
.\gradlew.bat testDebugUnitTest
```

---

## Instalar el APK en un dispositivo

1. En el teléfono: **Ajustes → Acerca del teléfono** → toca 7 veces
   **Número de compilación** para activar las **Opciones de desarrollador**.
2. **Ajustes → Sistema → Opciones de desarrollador** → activa **Depuración por USB**.
3. Conecta el teléfono por USB y acepta la alerta "Permitir depuración USB".
4. Instala:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Sin `adb`, transfiere `app-debug.apk` al teléfono y ábrelo para instalarlo
(deberás permitir "Instalar apps de orígenes desconocidos" para esa aplicación).

> Alternativa: abre el proyecto en Android Studio (**File → Open**) y pulsa
> **Run ▶** con el dispositivo conectado.

---

## Arquitectura

MVVM + Kotlin Coroutines, sin frameworks de inyección de dependencias.

```
app/src/main/java/com/oido/
├── MainActivity.kt              # Activity única + navegación + factorías
├── ViewModelFactories.kt        # Dependencias explícitas (sin DI framework)
├── audio/
│   ├── AudioEngine.kt           # Interfaz de reproducción
│   ├── ToneSynthesizer.kt       # Sintetizador de tono (Kotlin puro, testeable)
│   └── PianoAudioEngine.kt      # AudioTrack + hilo de escritura, sin clicks
├── domain/                      # Lógica pura, sin Android
│   ├── Note.kt                  # 7 notas + NoteFrequencyProvider (261.63…493.88 Hz)
│   ├── LevelConfig.kt           # Niveles y umbrales configurables
│   ├── ExerciseEngine.kt        # Sesión, validación y replays
│   ├── AdaptiveLearningEngine.kt# Selección ponderada (débiles + obsoletas + azar)
│   ├── SessionAccumulator.kt    # Estadísticas de la sesión
│   ├── LevelProgression.kt      # Desbloqueo por precisión real
│   └── StreakCalculator.kt      # Rachas diaria y de aciertos
├── data/
│   ├── OidoState.kt             # Estado serializable
│   ├── ProgressRepository.kt    # DataStore Preferences (persistencia local)
│   └── ProgressSummary.kt       # Reglas de presentación
└── ui/
    ├── theme/                   # Material 3 oscuro, paleta navy/piano
    ├── components/PianoKeyboard.kt  # Teclado reutilizable de 7 teclas
    ├── home/                    # HomeScreen + HomeViewModel
    ├── training/                # TrainingScreen + TrainingViewModel
    ├── results/                 # ResultsScreen
    ├── familiar/                # Modo exploración
    └── navigation/Screen.kt     # Rutas tipadas
```

### Motor de audio

Las notas se **sintetizan** (no hay samples empaquetados): serie armónica de 10
parciales con rolloff fijo —timbre idéntico en todas las notas—, ataque de 5 ms
con rampa raised-cosine, decaimiento exponencial (las notas agudas decaen más
rápido, como en un piano real) y release de 60 ms que garantiza silencio al
inicio y al final. Cada nota se normaliza al mismo pico (0.85) para que el
volumen no sea una pista.

`PianoAudioEngine` escribe los buffers en un `AudioTrack` desde un hilo propio;
al cambiar de nota aplica un fundido de salida de 10 ms para evitar clicks. La
interfaz `AudioEngine` mantiene el audio independiente de la UI.

### Integridad de la prueba auditiva

El objetivo **no se expone antes de responder**:

- `TrainingViewModel` conserva la nota objetivo dentro del motor; el estado de la
  UI solo la incluye tras `answer()`.
- Las descripciones de accesibilidad de las teclas indican la posición
  ("Tecla 3 de 7") hasta que se responde.
- Las etiquetas del teclado son fijas (DO…SI) y nunca cambian según la nota
  objetivo.
- Sin logs ni textos derivados de la nota antes de la respuesta.

---

## Limitaciones conocidas

- **Verificación en dispositivo**: el build y los 52 tests unitarios se ejecutaron
  en esta máquina; la reproducción de audio y el renderizado de la UI **no** se
  pudieron probar en un dispositivo físico desde este entorno. Si algún teléfono
  presenta latencia de audio, ajusta `CHUNK_SAMPLES` en `PianoAudioEngine`.
- Sin teclas negras, otras octaves, acordes o partituras en la v1 (a propósito,
  para no diluir el entrenamiento de tono). La arquitectura lo permite añadir.
- El progreso se borra si se desinstila la aplicación (DataStore local, sin
  respaldo en la nube, por diseño offline).
- Los desbloqueos no migran entre dispositivos.

---

## Roadmap (v2+)

1. Teclas negras (sostenidos/bemoles) como nivel avanzado.
2. Octavas adicionales (C3, C5) para ampliar el rango.
3. Modo "imagina la nota" (evocación mental antes de escuchar).
4. Recordatorios diarios de práctica con WorkManager.
