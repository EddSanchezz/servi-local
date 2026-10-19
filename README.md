# ServiLocal

Aplicación Android en **Kotlin** con arquitectura **MVVM** y pantallas en Jetpack Compose (Material 3). En esta fase, `MockDataRepository` gestiona los datos en memoria; no se implementan base de datos, sesiones de usuario, carga de imágenes, permisos ni mapas reales.

## Stack Tecnológico

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Kotlin | 2.2.10 | Lenguaje principal |
| Android Gradle Plugin | 9.1.1 | Build de Android |
| Gradle | 9.3.1 | Automatización de builds |
| Jetpack Compose BOM | 2024.09.00 | UI declarativa |
| Material 3 | incluido | Diseño UI |
| Kotlin Coroutines | 1.10.2 | Programación asíncrona |
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

La aplicación separa infraestructura compartida, funciones de producto y navegación. Cada función mantiene su propio ViewModel y `UiState`; `AppViewModel` solo conserva el rol activo y las preferencias globales.

```
com.example/
├── MainActivity.kt
├── core/
│   ├── data/                    ── Modelos y repositorio de demostración
│   ├── domain/                  ── Modelos y contratos de repositorio
│   └── ui/                      ── Componentes compartidos y tema Material 3
├── features/
│   ├── onboarding/              ── Bienvenida e introducción
│   ├── home/                    ── Inicio del usuario
│   ├── auth/                    ── Acceso, registro y recuperación
│   ├── explore/                 ── Búsqueda y detalle de servicios
│   ├── posts/                   ── Creación de publicaciones
│   ├── chat/                    ── Conversaciones y mensajes
│   ├── notifications/           ── Filtros y acciones de notificación
│   ├── moderation/              ── Auditoría, reportes y estadísticas
│   └── profile/                 ── Perfil y preferencias
└── navigation/
    └── AppNavHost.kt            ── Rutas, argumentos y back stack Compose
```

**Flujo de datos:** `Pantalla Compose` → `ViewModel de la función` (`UiState`/`StateFlow`) → `MockDataRepository`. `AppNavHost` mantiene el back stack y las pantallas reciben eventos de navegación como callbacks.

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

El proyecto incluye pruebas de lógica de ViewModel y capturas de pantallas en `app/src/test/`:

- **`FeatureViewModelTest.kt`** — validación de acceso/registro, creación de publicaciones, preferencias y recursos Android con Robolectric.
- **`ServiLocalScreenshotTest.kt`** — capturas de bienvenida, inicio y detalle con Compose UI Test y Robolectric.
- Las capturas se guardan bajo `app/src/test/screenshots/`.

### Evidencias visuales

El PDF reúne el ícono de la aplicación y las capturas de onboarding, inicio y detalle del servicio: [servilocal_evidencias.pdf](output/pdf/servilocal_evidencias.pdf).

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
- Los conectores remotos y la persistencia quedan fuera del alcance de esta fase.

## Notas de desarrollo

- **Minificación/ProGuard**: desactivada (`isMinifyEnabled = false`). No se usa R8/minify para mantener el build simple y rápido.
- **compileOptions**: Java 11 (compatibilidad de bytecode). El JDK de compilación es 17+ (recomendado 21).
- **Google Services**: el plugin `google-services` está presente con estrategia `WARN` (no falla si no hay `google-services.json`).

---

ServiLocal — Aplicación Android MVVM con datos en memoria para fase 2.
