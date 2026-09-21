# ServiLocal

Aplicación Android en **Kotlin** con arquitectura **MVVM + Clean Architecture**, Compose (Material 3), Hilt (inyección de dependencias), Room (persistencia local) y Navigation Compose.

## Stack Tecnológico

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Kotlin | 2.2.10 | Lenguaje principal |
| Android Gradle Plugin | 9.1.1 | Build de Android |
| Gradle | 9.3.1 | Automatización de builds |
| Jetpack Compose BOM | 2024.09.00 | UI declarativa |
| Material 3 | incluido | Diseño UI |
| Hilt | 2.51.1 | Inyección de dependencias |
| Room | 2.7.0 | Base de datos local |
| Navigation Compose | 2.8.9 | Navegación entre pantallas |
| Retrofit + Moshi | 2.12.0 | Llamadas HTTP/API |
| OkHttp | 4.10.0 | Cliente HTTP |
| Coil | 2.7.0 | Carga de imágenes |
| Firebase AI / AppCheck | BOM 34.17.0 | IA y seguridad Firebase |
| Kotlin Coroutines | 1.10.2 | Programación asíncrona |
| DataStore Preferences | 1.1.7 | Almacenamiento de preferencias |
| Roborazzi | 1.59.0 | Capturas de pantalla en tests |
| Robolectric | 4.16.1 | Tests unitarios Android |
| JDK | 17+ (recomendado 21) | Compilación y ejecución |

## Requisitos Previos

- **JDK 17 o superior** (recomendado **JDK 21**). Ya incluido en Android Studio como **JBR** (JetBrains Runtime):
  - Ruta: `C:/Program Files/Android/Android Studio/jbr`
  - Para verificar: `"C:/Program Files/Android/Android Studio/jbr/bin/java.exe" -version`
- **Android Studio** (Meerkat o superior) o **VS Code** con extensiones Kotlin y Android.

## Configuración de JDK en VS Code

Si usas VS Code, configura `java.jdt.ls.java.home` en `.vscode/settings.json`:

```json
{
  "java.jdt.ls.java.home": "C:/Program Files/Android/Android Studio/jbr"
}
```

Alternativamente, configura la variable de entorno del sistema:

```
JAVA_HOME = C:\Program Files\Android\Android Studio\jbr
```

## Arquitectura

El proyecto sigue **MVVM** en la capa de presentación y **Clean Architecture** en las capas de datos y dominio:

```
com.example/
├── MainActivity.kt              ── Punto de entrada
├── data/                        ── Capa de datos (repositorios, DAOs, DTOs)
│   ├── local/                   ── Persistencia local (Room)
│   │   ├── database/
│   │   └── entity/
│   ├── remote/                  ── API remotas (Retrofit)
│   │   └── dto/
│   ├── mapper/                  ── Mapeo DTO ↔ Entity
│   ├── model/                   ── Modelos de datos (ServicePost, UserProfile, etc.)
│   └── repository/              ── Repositorios (MockDataRepository)
├── domain/                      ── Capa de dominio (lógica de negocio)
│   ├── model/                   ── Modelos de dominio (AuthSession, Post)
│   ├── repository/              ── Interfaces de repositorio
│   │   ├── AuthRepository.kt
│   │   ├── PostRepository.kt
│   │   ├── ChatRepository.kt
│   │   ├── ModerationRepository.kt
│   │   └── NotificationRepository.kt
│   └── usecase/                 ── Casos de uso (lógica aplicable)
│       ├── auth/
│       ├── posts/
│       ├── chat/
│       ├── moderation/
│       └── notification/
├── di/                          ── Módulos de inyección (Hilt)
├── ui/                          ── Capa de presentación (MVVM)
│   ├── navigation/              ── Rutas y NavHost
│   ├── auth/                    ── Pantallas y ViewModel de autenticación
│   ├── explore/                 ── Pantalla de exploración/feed
│   ├── chat/                    ── Pantallas de chat
│   ├── moderation/              ── Panel de moderación
│   ├── profile/                 ── Perfil de usuario
│   ├── screens/                 ── Composición de pantallas
│   ├── theme/                   ── Temas, colores y tipografía (Material 3)
│   └── viewmodel/               ── ViewModels (ServiLocalViewModel)
└── viewmodel/                   ── ViewModel principal legacy (refactorizable)
```

**Flujo de datos:** `UI(Compose)` → `ViewModel` (estado `StateFlow`) → `Repository` (interface) → `DAO/Room` o `API(Retrofit)`.

## Estructura de carpetas del proyecto

```
servi-local/
├── app/
│   ├── build.gradle.kts         ── Configuración del módulo app
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/    ── Todo el código fuente
│       └── res/                 ── Recursos (layouts, values, etc.)
├── gradle/
│   └── wrapper/                 ── Gradle Wrapper (9.3.1)
├── .vscode/
│   ├── settings.json            ── Configuración de VS Code (JDK, Kotlin)
│   ├── extensions.json          ── Extensiones recomendadas
│   ├── tasks.json               ── Tareas Gradle (build, test, clean)
│   └── launch.json              ── Configuración de depuración
├── .env                         ── Variables de entorno (API keys, etc.)
├── .env.example                 ── Ejemplo de variables de entorno
├── build.gradle.kts             ── Buildscript de nivel raíz
├── settings.gradle.kts          ── Configuración de Gradle (incluye :app)
├── gradle.properties            ── Propiedades de Gradle
├── gradlew / gradlew.bat        ── Gradle Wrapper
└── README.md
```

## Compilar y ejecutar

### Con Gradle (línea de comandos)

```bash
# Compilar en debug
.\gradlew.bat :app:assembleDebug

# Ejecutar tests unitarios
.\gradlew.bat :app:testDebugUnitTest

# Limpiar build
.\gradlew.bat clean

# Build completo (todas las tareas)
.\gradlew.bat build
```

### Con Android Studio

1. Abrir el proyecto en Android Studio
2. Esperar a que sincronice Gradle (barra de progreso inferior)
3. Seleccionar un emulador o dispositivo conectado
4. Presionar ▶ (Run) o `Shift+F10`

### Con VS Code

1. Abrir la carpeta en VS Code
2. Instalar extensiones recomendadas (`.vscode/extensions.json`)
3. Abrir terminal integrada (`Ctrl+` `` ` ``)
4. Ejecutar `.\gradlew.bat :app:assembleDebug`

## Testing

El proyecto incluye tests en `app/src/test/`:

- **Robolectric tests** (`ExampleRobolectricTest.kt`) — tests Android sin emulador
- **Roborazzi screenshot tests** (`GreetingScreenshotTest.kt`) — capturas de pantalla para validar UI
- **JUnit tests** — tests unitarios puros

```bash
# Ejecutar todos los tests
.\gradlew.bat :app:testDebugUnitTest
```

## Configuración importante

### Firma de debug

El `build.gradle.kts` incluye un `debugConfig` signing config con un keystore local:

```kotlin
signingConfigs {
    create("debugConfig") {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }
}
```

### Secretos (API keys, Firebase)

Las claves API se configuran en `.env` (ver `.env.example` para los nombres correctos). El plugin `secrets-gradle-plugin` las lee automáticamente.

## Dependencias (gradle/libs.versions.toml)

Todas las versiones están centralizadas en `gradle/libs.versions.toml`:

- `agp = "9.1.1"` — Android Gradle Plugin
- `kotlin = "2.2.10"` — Kotlin
- `composeBom = "2024.09.00"` — Compose BOM
- `navigationCompose = "2.8.9"` — Navigation Compose
- `roomRuntime = "2.7.0"` — Room
- `retrofit = "2.12.0"` — Retrofit
- `firebaseBom = "34.17.0"` — Firebase BOM
- `hilt = "2.51.1"` — Hilt DI

## Notas de desarrollo

- **Minificación/ProGuard**: desactivada (`isMinifyEnabled = false`). No se usa R8/minify para mantener el build simple y rápido.
- **compileOptions**: Java 11 (compatibilidad de bytecode). El JDK de compilación es 17+ (recomendado 21).
- **Google Services**: el plugin `google-services` está presente con estrategia `WARN` (no falla si no hay `google-services.json`).

---

ServiLocal — Aplicación Android MVVM + Clean Architecture.
