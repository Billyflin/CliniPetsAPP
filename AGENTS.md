# Repository Guidelines

## Project Structure & Module Organization
The Android application lives in `app/`, a single Gradle module configured in `app/build.gradle.kts`. Source code sits in `app/src/main/java/cl/clinipets`, with Compose themes in `ui/theme` and shared helpers in `util`. UI resources and assets belong in `app/src/main/res`. API clients are generated from `backend-openapi.yaml` into `app/build/generate-resources/main/src/main/kotlin` after running the OpenAPI task, so avoid editing those files directly. Unit and instrumentation test stubs live in `app/src/test/java` and `app/src/androidTest/java`; add new suites there.

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: compile the debug APK and trigger OpenAPI generation.
- `./gradlew openApiGenerate`: refresh Retrofit models from the contract.
- `./gradlew testDebugUnitTest`: execute JVM unit tests under `app/src/test`.
- `./gradlew connectedDebugAndroidTest`: run device or emulator instrumentation tests.
- `./gradlew lint`: verify Android lint checks before opening a PR.

## Coding Style & Naming Conventions
Kotlin files use 4-space indentation, trailing commas where the Kotlin style guide recommends, and explicit visibility on non-public APIs. Name composables and classes in PascalCase (`PetListScreen`), functions in camelCase, and constants in UPPER_SNAKE_CASE. Follow the existing package layout (`cl.clinipets.feature`) to keep navigation, data, and UI code grouped. Prefer immutable data classes, Hilt-injected constructors, and sealed `Result` wrappers for async responses. Use Android Studio's formatter before committing.

## Arquitectura Base
- Los modelos generados en `cl.clinipets.openapi.models` se usan en todas las capas; evita duplicar DTOs en dominio o presentación.
- Features viven bajo `cl.clinipets.feature.<feature>` con subpaquetes `data`, `domain` y `presentation`.
- `data` expone data sources que consumen las interfaces de Retrofit generadas (`cl.clinipets.openapi.apis`) y repositorios que devuelven un `Resultado<T>` sellado.
- `domain` define los contratos de repositorio (retornando modelos OpenAPI) y los casos de uso orquestados por la capa de presentación.
- `presentation` agrupa ViewModels con estado inmutable, y pantallas Compose conectadas vía Hilt.
- Los módulos de DI globales viven en `cl.clinipets.di` y publican `ApiClient`, las APIs generadas y los repositorios concretos.
- La capa de autenticación reside en `cl.clinipets.feature.auth` y controla la sesión mediante `SesionLocalDataSource` (DataStore) y un `AuthInterceptor` que añade el token Bearer a cada request.
- `AuthGate` ejecuta el flujo real de Google Sign-In utilizando `GoogleAuthProvider` (configurado con `BuildConfig.GOOGLE_SERVER_CLIENT_ID`) y envía el ID token recibido al backend a través del `AuthRepositorio`.

## Testing Guidelines
Write fast unit tests with JUnit4 and Mockito/Kotlin test utilities in `app/src/test/java`. Instrument UI flows with Espresso or Compose testing APIs in `app/src/androidTest/java`. Name tests after the behavior under test (`ReservationRepositoryTest`). Maintain coverage on new repositories and view models by asserting repository contracts and navigation start destinations. Always run the relevant Gradle test task locally and attach logs when a failure needs diagnosis.

## Commit & Pull Request Guidelines
Commits follow the repository convention `Type: Summary`, as seen with `Feat:` and `Chore:` prefixes (`Feat: Add Discovery ...`). Keep messages in the imperative mood and focus on one logical change. Pull requests should include: a concise description, linked Jira/GitHub issue IDs, screenshots or screen recordings for UI updates, a note on OpenAPI changes when applicable, and the Gradle commands you executed. Request review once CI is green and conflicts are resolved.

## Environment & Security Notes
Populate secrets via `local.properties` or environment variables (`BASE_URL_DEBUG`, `GOOGLE_SERVER_CLIENT_ID`, `MAPS_API_KEY`); never commit actual keys. The debug build falls back to safe defaults, but release builds expect explicit configuration. Verify that any new keystore or credential is ignored by Git before using it locally.
