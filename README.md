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

Flujo de datos habitual: `Compose` → `ViewModel` → repositorio. Algunas operaciones pasan por un caso de uso antes de llegar al repositorio. Hilt conecta los contratos de repositorio con `MockDataRepository`. Navigation Compose administra un grafo raíz para onboarding y autenticación y un grafo anidado para las pantallas de usuario o moderación. En el dashboard, Explorar es la pestaña inicial; `HomeScreen` está implementada, pero no forma parte de la barra de pestañas actual.

En esta fase, la aplicación usa datos de demostración en memoria: no hay persistencia, autenticación remota, mapas reales ni carga de imágenes. Las guías de clase y los archivos de referencia del ERD se conservan localmente en `/docs`; esta carpeta está excluida de Git.

## Requisitos

- Android Studio con JDK 17 o superior.
- Gradle Wrapper incluido en el repositorio (Gradle 9.6.0).
- Android Gradle Plugin 9.4.1 y Kotlin 2.2.10.

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
