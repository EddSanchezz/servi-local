# ServiLocal

Aplicación Android en Kotlin y Jetpack Compose para conectar a la comunidad con servicios locales en Armenia y el Quindío. La interfaz sigue los prototipos del proyecto y, en esta fase, usa datos de demostración en memoria.

## Arquitectura

El código separa infraestructura, dominio, funciones de producto y navegación. Cada función mantiene su ViewModel y estado de UI; la navegación y la sesión global tienen responsabilidades distintas.

```text
app/src/main/java/com/example/
├── core/
│   ├── data/repository/MockDataRepository.kt
│   ├── di/RepositoryModule.kt
│   └── ui/                         # tema y componentes compartidos
├── domain/
│   ├── model/
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

Flujo de datos: `Compose` → `ViewModel` (`UiState`/`StateFlow`) → caso de uso → contrato de repositorio → implementación de demostración. Los repositorios se enlazan con Hilt. La navegación usa Navigation Compose con un back stack raíz para onboarding y autenticación, y un grafo anidado para el área de usuario o moderación.

En esta fase no hay persistencia, autenticación remota, mapas ni carga de imágenes. Las guías de clase se conservan localmente en `/docs`; esta carpeta está excluida de Git.

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
