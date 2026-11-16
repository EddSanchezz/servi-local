# ServiLocal

Aplicación Android en Kotlin y Jetpack Compose para conectar a la comunidad con servicios locales en Armenia y el Quindío.

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

Flujo de datos: `Compose` → `ViewModel` → casos de uso → interfaces de repositorio. Hilt conecta los contratos con sus implementaciones. Navigation Compose tiene un grafo raíz para onboarding/autenticación y un dashboard con navegación distinta según el rol: Home es la pestaña inicial del usuario y Auditoría la del moderador.

La aplicación usa Firebase Authentication y Firestore para perfiles, publicaciones, likes, comentarios, chats/solicitudes, moderación, insignias ganadas y notificaciones dentro de la app. Puntos y nivel se guardan en el perfil del usuario; el otorgamiento por publicación usa un evento idempotente. Cloudinary almacena imágenes de perfil y publicaciones. El PDF de procesos y reparto está en la raíz.

El módulo de publicaciones permite buscar por texto, filtrar por categoría y zona, y limitar por distancia desde coordenadas de la publicación. Al crear o editar, se selecciona el punto en OpenStreetMap/osmdroid y se puede escribir una descripción; el GPS es opcional y solo se solicita al pulsar “Usar mi ubicación”. La dirección no se geocodifica automáticamente. Desde el perfil se consultan y eliminan publicaciones propias; editar y finalizar está permitido al autor para publicaciones aprobadas. La moderación ofrece una cola FIFO de pendientes, aprobación y rechazo con motivo obligatorio.

## Requisitos

- Android Studio con JDK 17 o superior.
- Gradle Wrapper incluido en el repositorio (Gradle 9.6.0).
- Android Gradle Plugin 9.4.1 y Kotlin 2.2.10.
- Android SDK Platform 36.1 y Build Tools 36.0.0 (los define `app/build.gradle.kts`).

En Windows, configura `sdk.dir` en `local.properties` con la ruta local del SDK. Este archivo no se sube a Git. Para instalar en un dispositivo Android, activa las opciones de desarrollador y la depuración USB; para usar el emulador, crea/inicia un AVD compatible desde Android Studio o el SDK Manager.

### GPS y mapas

El feed usa OpenStreetMap mediante osmdroid. La pestaña Mapa muestra tiles reales, marcadores personalizados agrupados por proximidad y la ubicación GPS del dispositivo cuando se concede el permiso. Las coordenadas de publicaciones creadas sin geocodificación se calculan solo para zonas demo conocidas; para producción se debe añadir un servicio de geocodificación y respetar sus políticas de uso. Carto Voyager puede activarse posteriormente con un token del proveedor; sin token la app usa OSM como fallback para no mostrar mosaicos rotos.

### Firebase

Firebase Authentication gestiona las cuentas. Firestore guarda perfiles en `users/{uid}`, insignias ganadas en `users/{uid}/badges/{badgeId}`, eventos de puntos en `users/{uid}/pointEvents/{eventId}`, publicaciones en `posts/{postId}`, apoyos en `likes/{postId}_{uid}`, comentarios en `comments/{commentId}` y notificaciones en `notifications/{notificationId}`. El inbox se consulta por UID y cada notificación conserva el actor y la referencia al evento que la originó. El registro de moderadores se mantiene separado en `moderators/{uid}` y solo debe provisionarse desde una sesión administrativa, nunca desde la app.

1. Crea un proyecto Firebase y registra el paquete `com.aistudio.servilocal.quindio`.
2. Habilita los proveedores Email/Password y Google, además de Firestore.
3. Registra las huellas SHA-1 y SHA-256 del certificado usado para instalar la aplicación.
4. Descarga `google-services.json` y colócalo en `app/google-services.json`.
5. Revisa `firestore.rules` antes de desplegarlas. Las reglas incluyen otorgamiento de puntos ligado a una publicación, likes/comentarios y la subcolección de insignias; deben probarse en Emulator y staging antes de desplegar a producción.

Los usuarios pueden registrarse con correo y contraseña o iniciar sesión con Google. `google-services.json` no debe subirse a Git.

Para probar Auth y Firestore sin escribir en producción, inicia los emuladores locales. En `local.properties` (ignorado por Git) activa el modo emulador solo para `debug`:

```properties
FIREBASE_EMULATOR_ENABLED=true
FIREBASE_EMULATOR_HOST=10.0.2.2
```

En PowerShell, inicia Auth y Firestore usando el mismo ID de proyecto que contiene `google-services.json`:

```powershell
$projectId = (Get-Content app\google-services.json | ConvertFrom-Json).project_info.project_id
npx --yes firebase-tools emulators:start --only auth,firestore --project $projectId
```

La app Android usa `10.0.2.2` para alcanzar los servicios del host desde el AVD. La variante `release` ignora esta opción y nunca se conecta a los emuladores.

### Cloudinary

Cloudinary se usa para almacenar las imágenes de perfil y publicaciones. La app conserva en Firestore únicamente las URLs HTTPS devueltas por Cloudinary. Configura sus datos en `local.properties`:

```properties
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_UPLOAD_PRESET=tu_unsigned_upload_preset
```

`local.properties` y las credenciales no deben subirse a Git.

## Compilar y probar

En Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas instrumentadas de Firebase (`:app:connectedDebugAndroidTest`) requieren Auth y Firestore Emulator activos y `FIREBASE_EMULATOR_ENABLED=true` en `local.properties`. No ejecutar esas pruebas contra producción.

En macOS o Linux:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Las pruebas unitarias de `app/src/test` usan repositorios falsos para que no consuman lecturas/escrituras de Firebase ni subidas de Cloudinary. Las pruebas instrumentadas verifican Auth/Firestore Emulator, duplicación de insignias, puntos, likes, comentarios, notificaciones persistentes y sus métricas. Ejecuta `connectedDebugAndroidTest` solo con `FIREBASE_EMULATOR_ENABLED=true` y los emuladores activos; nunca contra producción.

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. Para instalarlo en un dispositivo conectado por ADB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

