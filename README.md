# ServiLocal

## 1. Descripción del proyecto

ServiLocal es el proyecto final de la asignatura **Desarrollo de Aplicaciones para Dispositivos Móviles** del Programa de Ingeniería de Sistemas y Computación de la Universidad del Quindío, impartida por el docente Carlos Andrés Flórez V. Consiste en una aplicación móvil nativa desarrollada en **Kotlin con Jetpack Compose**.

La propuesta corresponde a la **Temática 2: Marketplace de Servicios Locales**, cuyo propósito es conectar a personas que necesitan contratar servicios locales con proveedores confiables cerca de su ubicación. La entidad principal del sistema es la **oferta de servicio**.

La aplicación resuelve un problema concreto: muchos emprendedores y negocios pequeños no tienen un canal digital sencillo para darse a conocer, mostrar sus servicios y ser contactados por clientes de su zona. ServiLocal les permite publicar sus ofertas, aparecer en un feed y en un mapa, recibir solicitudes de servicio y conversar con los clientes, mientras la comunidad cuenta con herramientas de reporte y moderación para mantener la confianza.

**Categorías de servicio**

- **Hogar:** plomería, electricidad y limpieza.
- **Educación:** clases particulares y tutorías.
- **Mascotas:** paseo, cuidado y veterinaria.
- **Tecnología:** reparación y soporte.
- **Transporte:** mudanzas y mensajería.

**Características del marketplace**

- Cada oferta incluye un **rango de precio estimado**, una **zona de cobertura** y su ubicación geográfica.
- El cliente puede **solicitar un servicio directamente desde la publicación**.
- Las ofertas pasan por un proceso de **verificación por moderadores** para evitar información engañosa.
- El sistema de reputación usa los niveles **Principiante, Profesional, Experto y Maestro**.
- **Función innovadora:** el asistente de inteligencia artificial **"Jui"**, que ayuda a redactar y mejorar el título y la descripción de una publicación.

**Funcionalidades principales**

- Registro e inicio de sesión con correo/contraseña o con Google.
- Publicación de servicios con título, categoría, descripción, rango de precios, ubicación, zona de cobertura y hasta cinco fotos.
- Búsqueda y filtrado por texto, categoría, zona y distancia.
- Visualización de publicaciones en lista o en mapa interactivo.
- Solicitudes de servicio y chat entre cliente y proveedor.
- Comentarios, votación "Es importante"/"Me interesa", notificaciones, puntos, niveles e insignias.
- Reporte de publicaciones y flujo de moderación.
- Edición, finalización y eliminación de publicaciones propias.
- Perfil con estadísticas personales y opción de eliminar la cuenta.

---

## 2. Tecnologías del proyecto

| Tecnología | Uso en el proyecto |
|---|---|
| **Kotlin 2.2.10** | Lenguaje principal |
| **Jetpack Compose + Material 3** | Interfaz de usuario declarativa |
| **Navigation Compose 2.8.9** | Navegación entre pantallas y pestañas por rol |
| **Hilt 2.60.1 + KSP** | Inyección de dependencias |
| **Coroutines + StateFlow 1.10.2** | Asincronía y estado observable |
| **ViewModel + Lifecycle Compose** | Presentación y ciclo de vida |
| **Firebase BOM 34.5.0** | Authentication y Cloud Firestore |
| **Credential Manager 1.5.0 + Google ID** | Inicio de sesión con Google |
| **Cloudinary Android 3.1.3** | Almacenamiento y entrega de imágenes |
| **Retrofit 3.0.0 + Gson** | Consumo de la API de OpenRouter |
| **osmdroid 6.1.20** | Mapas con tiles de OpenStreetMap |
| **Play Services Location 21.3.0** | Ubicación del dispositivo |
| **Coil 2.7.0** | Carga de imágenes remotas |
| **JUnit 4 + Robolectric 4.16.1** | Pruebas unitarias |
| **Android Gradle Plugin 9.4.1 / Gradle 9.6.0** | Compilación |
| **minSdk 24 / targetSdk 36** | Compatibilidad de Android |

---

## 3. Arquitectura del proyecto

El proyecto sigue una arquitectura por capas combinada con organización por funcionalidades (*feature-based*), patrón **MVVM** y **Repository Pattern**. La regla de dependencia es:

```text
UI (Compose) → ViewModel → Caso de uso → Interfaz de repositorio → Implementación
```

Los ViewModels exponen estado con `StateFlow` y la UI lo observa con `collectAsStateWithLifecycle`. Hilt conecta cada interfaz de dominio con su implementación concreta (`core/di/RepositoryModule.kt`), de modo que el dominio no conoce Firebase, Cloudinary ni OpenRouter.

### Estructura de carpetas

```text
app/src/main/java/com/servilocal/
├── MainActivity.kt              # Punto de entrada; monta AppNavHost
├── ServiLocalApp.kt             # Application; inicializa Hilt y Cloudinary
├── core/
│   ├── constants/               # Recursos del asistente IA
│   ├── data/repository/         # Repositorios compartidos
│   ├── di/                      # FirebaseModule, NetworkModule, RepositoryModule, ResourceModule
│   ├── location/                # LocationProvider con FusedLocationProviderClient
│   ├── security/                # Utilidades de seguridad
│   ├── ui/                      # Tema y componentes Compose compartidos
│   └── util/                    # Validadores, permisos, imágenes, resultados
├── data/
│   ├── remote/openrouter/       # Definición de la API (Retrofit)
│   └── repository/
│       ├── cloudinary/          # Subida de imágenes
│       ├── firebase/            # Auth, Firestore, chats, posts y moderación
│       ├── memory/              # Sesión e insignias
│       └── openrouter/          # Asistente IA "Jui"
├── domain/
│   ├── exception/               # Errores de negocio
│   ├── model/                   # Entidades del ERD y modelos para la UI
│   ├── repository/              # Contratos (puertos)
│   └── usecase/                 # Casos de uso por feature
├── features/
│   ├── app/                     # Sesión y preferencias globales
│   ├── auth/                    # Login, registro y recuperación
│   ├── chat/                    # Conversaciones y mensajes
│   ├── home/                    # Feed, mapa y detalle de publicación
│   ├── moderation/              # Auditoría, reportes y métricas
│   ├── notifications/           # Notificaciones
│   ├── onboarding/              # Presentación inicial
│   ├── posts/                   # Crear y editar publicaciones
│   └── profile/                 # Perfil, ajustes y publicaciones propias
└── navigation/AppNavHost.kt     # Grafo raíz y grafo interno del dashboard
```

### Capas

- **`features/` (UI + presentación):** pantallas Compose y ViewModels. No acceden directamente a servicios externos.
- **`domain/` (reglas de negocio):** modelos, interfaces de repositorio y casos de uso. Es independiente del framework.
- **`data/` (infraestructura):** implementaciones de los repositorios que hablan con Firebase, Cloudinary y OpenRouter.
- **`core/` (transversal):** tema, componentes, utilidades, ubicación e inyección de dependencias.
- **`navigation/`:** grafo raíz (onboarding/autenticación) y grafo del dashboard, que cambia sus pestañas según el rol.

### Navegación

Se usan dos grafos de Navigation Compose:

- **Grafo raíz:** onboarding, login, registro, recuperación y dashboard.
- **Grafo del dashboard:** pestañas diferentes según el rol.
  - Usuario: Inicio, Mensajes, Publicar, Perfil.
  - Moderador: Auditoría, Reportes, Métricas, Staff.

---

## 4. Servicios del proyecto

| Servicio | Tipo | Objetivo en el proyecto | Problema que resuelve |
|---|---|---|---|
| **Firebase Authentication** | Backend de Google | Registrar e iniciar sesión con correo/contraseña o Google | Gestionar credenciales de forma segura sin almacenar contraseñas en la app |
| **Cloud Firestore** | Base de datos NoSQL | Guardar perfiles, publicaciones, chats, reportes y moderadores | Persistencia y sincronización en tiempo real entre usuarios |
| **Cloudinary** | CDN de imágenes | Almacenar fotos de perfil y publicaciones | Evitar guardar archivos binarios en Firestore y optimizar la entrega de imágenes |
| **OpenRouter** | API de modelos de IA | Asistente "Jui" para crear y mejorar publicaciones | Ayudar al usuario a redactar títulos y descripciones cuando no sabe cómo hacerlo |
| **osmdroid + OpenStreetMap** | Librería open source + tiles de OSM | Mostrar el mapa, los marcadores y la ubicación | Descubrir servicios cercanos sin depender de una API key de mapas |
| **Google Play Services Location** | SDK de ubicación | Obtener la ubicación del dispositivo | Centrar el mapa y calcular servicios cercanos |

### Firebase Authentication

Gestiona las cuentas de usuario. La app admite correo/contraseña y Google mediante Credential Manager. Firebase conserva la credencial y la app solo guarda el perfil en Firestore (`users/{uid}`). También se encarga de la recuperación de contraseña mediante un enlace profundo `servilocal://auth`.

**Problema que resuelve:** evita que la aplicación almacene o compare contraseñas por su cuenta y centraliza el inicio de sesión, el cierre de sesión y la recuperación de acceso.

### Cloud Firestore

Es la base de datos principal. Las colecciones usadas son:

- `users/{uid}`: perfil, rol, estado, puntos y nivel.
- `posts/{postId}`: publicaciones con estado de moderación, rango de precios, zona de cobertura, ubicación y fotos.
- `chats/{chatId}` y `chats/{chatId}/messages`: conversaciones y mensajes.
- `reports/{reportId}`: reportes de publicaciones.
- `notifications/{notificationId}`: bandeja de notificaciones por usuario, con estado de lectura y referencias a publicaciones, chats, comentarios o reportes.
- `moderators/{uid}`: datos del personal de moderación.

Las reglas de seguridad (`firestore.rules`) aplican denegación por defecto y validan rol, autoría y estados permitidos.

**Problema que resuelve:** mantiene los datos sincronizados en tiempo real y aplica reglas de acceso del lado del servidor.

### Cloudinary

Almacena las imágenes de perfil y de las publicaciones. La app sube la imagen con un *upload preset* unsigned, aplica una reducción a 1280×1280 y guarda en Firestore únicamente la URL HTTPS (`secure_url`) devuelta por Cloudinary.

**Problema que resuelve:** evita almacenar imágenes en la base de datos, reduce el peso de las respuestas y permite servir imágenes optimizadas.

### OpenRouter (asistente "Jui")

Es el servicio de inteligencia artificial del proyecto. El asistente ayuda a crear una publicación desde cero o a mejorar un título y una descripción existentes. Solo puede modificar `title` y `description`; no sugiere precios, categorías, ubicación ni fotos. Usa modelos gratuitos (`:free`) con una lista de respaldo si el modelo principal falla.

**Problema que resuelve:** reduce la barrera de escritura para emprendedores que no saben cómo describir su servicio.

### Mapas: osmdroid + OpenStreetMap

El mapa no usa Google Maps ni un proveedor de pago: se implementó con **osmdroid**, una librería open source, y **tiles de OpenStreetMap** (`TileSourceFactory.MAPNIK`). No requiere API key. La app muestra marcadores agrupados por proximidad, permite centrar el mapa en la ubicación del usuario y muestra la atribución correspondiente a OpenStreetMap/CartoDB. La ubicación se obtiene con `FusedLocationProviderClient` de Google Play Services, con respaldo del `LocationManager` del dispositivo.

**Problema que resuelve:** permite descubrir servicios cercanos geográficamente sin costos de licencia ni configuración de claves de mapas.

---

## 5. Roles del sistema

La app tiene dos roles definidos en `UserRole`: **Usuario** y **Moderador**. El rol se guarda en `users/{uid}.role` y la navegación cambia automáticamente según el rol activo.

### Usuario (`USER`)

Es quien hace uso del marketplace, ya sea como cliente o como proveedor. Puede:

- Registrarse e iniciar sesión con correo/contraseña o Google.
- Recuperar su contraseña mediante correo electrónico.
- Explorar el feed, buscar por texto y filtrar por categoría, zona o distancia.
- Ver publicaciones en lista o en el mapa.
- Crear publicaciones con fotos, rango de precios, ubicación, zona de cobertura y descripción.
- Editar, finalizar o eliminar sus propias publicaciones.
- Enviar solicitudes de servicio y conversar por chat con el proveedor.
- Comentar, votar "Es importante"/"Me interesa" y recibir notificaciones por comentarios, aprobaciones, rechazos o logros.
- Acumular puntos e insignias y avanzar por los niveles Principiante, Profesional, Experto y Maestro.
- Consultar estadísticas personales: publicaciones activas, finalizadas y pendientes.
- Reportar publicaciones que incumplan las normas.
- Editar su perfil y eliminar su cuenta.

### Moderador (`MODERATOR`)

Es el rol encargado de mantener la seguridad y el orden de la comunidad. Sus cuentas se precargan y se provisionan fuera de la app. Puede:

- Revisar la cola de publicaciones pendientes de verificación.
- Verificar publicaciones o rechazarlas registrando un motivo obligatorio; una publicación rechazada se oculta del feed público.
- Marcar cualquier publicación como resuelta o finalizada.
- Revisar los reportes enviados por los usuarios.
- Resolver reportes: descartarlos o declararlos procedentes.
- Rechazar la publicación reportada y, si corresponde, bloquear al proveedor.
- Consultar métricas y estadísticas de moderación.
- Gestionar su turno y su perfil de staff.

El registro de moderadores se mantiene separado en `moderators/{uid}` y debe provisionarse desde una sesión administrativa, nunca desde la app. Las reglas de Firestore impiden que un usuario común se asigne el rol de moderador o modifique su propio estado.

---

## 6. Puesta en marcha

### Requisitos

- Android Studio con JDK 17 o superior.
- Gradle Wrapper incluido (Gradle 9.6.0).
- Android Gradle Plugin 9.4.1 y Kotlin 2.2.10.
- Android SDK Platform 36.1 y Build Tools 36.0.0.

### `local.properties`

```properties
sdk.dir=C\:\\ruta\\a\\tu\\Android\\Sdk
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_UPLOAD_PRESET=tu_unsigned_upload_preset
OPENROUTER_API_KEY=tu_api_key
OPENROUTER_MODEL=inclusionai/ling-3.2-flash:free
OPENROUTER_FALLBACK_MODELS=baidu/ernie-5.5-flash-preview:free
```

`local.properties` y `google-services.json` no deben subirse a Git.

### Firebase

1. Crear un proyecto en Firebase y registrar el paquete `com.aistudio.servilocal.quindio`.
2. Habilitar los proveedores Email/Password y Google.
3. Habilitar Cloud Firestore.
4. Registrar las huellas SHA-1 y SHA-256 del certificado usado.
5. Descargar `google-services.json` y colocarlo en `app/google-services.json`.
6. Revisar y desplegar `firestore.rules`.

### Cloudinary

1. Crear una cuenta y un *upload preset* unsigned.
2. Configurar `CLOUDINARY_CLOUD_NAME` y `CLOUDINARY_UPLOAD_PRESET` en `local.properties`.

### OpenRouter

1. Crear una API key en OpenRouter.
2. Configurar `OPENROUTER_API_KEY` y, si se desea, `OPENROUTER_MODEL` y `OPENROUTER_FALLBACK_MODELS` en `local.properties`.
3. Si la API key está vacía, el asistente IA responde con un fallo controlado y la app sigue funcionando.

### Compilar y probar

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Las pruebas unitarias de `app/src/test` usan repositorios falsos para no consumir lecturas/escrituras de Firebase ni subidas de Cloudinary. Las pruebas instrumentadas de `app/src/androidTest` verifican la configuración básica del proyecto.
