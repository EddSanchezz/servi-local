# ServiLocal

Aplicación Android en Kotlin y Jetpack Compose para conectar a la comunidad con servicios locales en Armenia y el Quindío. La interfaz sigue los prototipos del proyecto y, en esta fase, usa datos de demostración en memoria.

## Arquitectura

El código separa infraestructura, dominio, funciones de producto y navegación. Los ViewModels exponen estado mediante `StateFlow`; la UI lo observa con `collectAsStateWithLifecycle`. La navegación está en `AppNavHost` y la sesión y preferencias de la aplicación están en `AppViewModel`.

```text
app/src/main/java/com/servilocal/
├── core/
│   ├── data/repository/MockDataRepository.kt  # datos de demostración en memoria
│   ├── di/RepositoryModule.kt
│   └── ui/                         # tema y componentes compartidos
├── domain/
│   ├── model/                      # entidades del ERD y modelos para la UI
│   ├── repository/                 # contratos
│   └── usecase/                    # casos de uso
├── features/
│   ├── app/                        # sesión y preferencias globales
│   ├── auth/                       # acceso, registro y recuperación
│   ├── chat/
│   ├── explore/                    # búsqueda y detalle
│   ├── home/
│   ├── moderation/
│   ├── notifications/
│   ├── onboarding/
│   ├── posts/                      # creación de publicaciones
│   └── profile/
└── navigation/AppNavHost.kt        # grafo raíz y grafo interno
```

Flujo de datos previsto: `Compose` → `ViewModel` → casos de uso → interfaces de repositorio. Hilt conecta los contratos con implementaciones singleton en memoria. Navigation Compose tiene un grafo raíz para onboarding/autenticación y un dashboard con navegación distinta según el rol: Home es la pestaña inicial del usuario y Auditoría la del moderador.

En esta fase, la aplicación usa datos de demostración en memoria: no hay persistencia, autenticación remota, mapas reales ni carga de imágenes. El PDF de procesos y reparto está en la raíz. La carpeta `/docs` está excluida de Git y no se incluye en esta copia del proyecto.

El módulo de publicaciones permite buscar por texto, filtrar por categoría y zona, y limitar por distancias calculadas desde coordenadas aproximadas de prueba en Armenia. No usa GPS ni geocodificación real. Desde el perfil se pueden consultar y eliminar publicaciones propias; editar y finalizar está permitido al autor para publicaciones aprobadas. La moderación ofrece una cola FIFO de pendientes, aprobación, rechazo con motivo obligatorio y resolución de publicaciones/reportes.

## Requisitos

- Android Studio con JDK 17 o superior.
- Gradle Wrapper incluido en el repositorio (Gradle 9.6.0).
- Android Gradle Plugin 9.4.1 y Kotlin 2.2.10.
- Android SDK Platform 36.1 y Build Tools 36.0.0 (los define `app/build.gradle.kts`).

En Windows, configura `sdk.dir` en `local.properties` con la ruta local del SDK. Este archivo no se sube a Git. Para instalar en un dispositivo Android, activa las opciones de desarrollador y la depuración USB; para usar el emulador, crea/inicia un AVD compatible desde Android Studio o el SDK Manager.

### GPS y mapas

El feed usa OpenStreetMap mediante osmdroid. La pestaña Mapa muestra tiles reales, marcadores personalizados agrupados por proximidad y la ubicación GPS del dispositivo cuando se concede el permiso. Las coordenadas de publicaciones creadas sin geocodificación se calculan solo para zonas demo conocidas; para producción se debe añadir un servicio de geocodificación y respetar sus políticas de uso. Carto Voyager puede activarse posteriormente con un token del proveedor; sin token la app usa OSM como fallback para no mostrar mosaicos rotos.

### Firebase Storage e imágenes

La carga remota de imágenes requiere un proyecto Firebase real y no se puede completar solo desde este repositorio. Para activarla:

1. Crea un proyecto en Firebase y registra el paquete `com.aistudio.servilocal.quindio`.
2. Habilita Cloud Storage y Authentication. Firebase Storage requiere el plan Blaze según la documentación actual de Firebase.
3. Descarga `google-services.json` y colócalo en `app/google-services.json`; no lo subas a Git.
4. Define reglas de Storage que permitan a usuarios autenticados subir solo sus propias imágenes.

La implementación actual conserva fotos como datos de la publicación demo; la integración Firebase queda pendiente de ese archivo y de las reglas del bucket. No se han incorporado secretos ni claves ficticias.

## Compilar y probar

En Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

En macOS o Linux:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Las pruebas unitarias de `app/src/test` cubren la sesión, validación de acceso y registro, creación de publicaciones, filtros y horarios. Las pruebas instrumentadas de `app/src/androidTest` verifican la configuración básica del proyecto en Android.

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. Para instalarlo en un dispositivo conectado por ADB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
