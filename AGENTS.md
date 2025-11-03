# Repository Guidelines

## Project Structure & Module Organization
The Android application lives in `app/`, with Gradle scripts at the repository root. UI layers are grouped under `app/src/main/kotlin/cl/clinipets/ui/` (e.g., `auth`, `mascotas`, `navigation`), while shared services stay in `core/`. Static resources sit in `app/src/main/res/`, and generated OpenAPI clients are added to `build/generate-resources/main/src/main/kotlin`. Unit tests belong in `app/src/test/`, instrumentation tests in `app/src/androidTest/`, and design artifacts (OpenAPI specs, UML, mockups) remain alongside `backend-openapi.yaml` at the root.

## Build, Test & Development Commands
- `./gradlew assembleDebug` compiles the debug APK using local settings from `gradle.properties` and `local.properties`.
- `./gradlew testDebugUnitTest` executes JVM unit tests under `app/src/test`.
- `./gradlew connectedDebugAndroidTest` runs instrumentation tests on an attached device or emulator.
- `./gradlew openApiGenerate` refreshes Retrofit clients from `backend-openapi.yaml`; rerun before committing API contract changes.

## Coding Style & Naming Conventions
Follow Kotlin official style with four-space indentation and trailing commas where helpful. Compose UI files group screen state, events, and previews in a single file, while view models reside in matching packages (`MascotasViewModel` for `MascotasScreen`). Class names use PascalCase, functions camelCase, and constants UPPER_SNAKE. Prefer `@Composable` previews tagged with `PreviewLight` and `PreviewDark` so designers can validate both themes. Run Android Studio’s “Reformat Code” before pushing.

## Navigation Guidelines
- Prefer Compose Navigation with typed destinations (2.8+). All main screens define `@Serializable` routes in `ui/navigation/AppNavGraph.kt`.
- Typed navigation is optional. For legacy or quick usage, equivalent string routes are also registered.
- Mascotas module routes:
  - Typed: `MascotasRoute`, `MascotaDetailRoute(id: String)`, `MascotaFormRoute(id: String? = null)`
  - Strings: `"mascotas"`, `"mascota/detalle/{id}"`, `"mascota/form"`, `"mascota/form/{id}"`
- Examples:
  - Typed: `navController.navigate(MascotaDetailRoute(id.toString()))`
  - String: `navController.navigate("mascota/detalle/${'$'}id")`
- Form accepts optional id. If `id` is null or inválido, the screen opens in create mode.

## Testing Guidelines
Target unit coverage for new view models, repositories, and use cases; add fake implementations if production code relies on coroutines or DataStore. Name tests using the `givenScenario_whenAction_thenOutcome` pattern. Instrumentation specs should live under `app/src/androidTest/kotlin` and clean up created data via API helpers. When modifying API calls, regenerate stubs and extend tests to ensure serialization stays in sync.

## Commit & Pull Request Guidelines
Existing history uses `Feat:`, `Fix:`, and `Refactor:` prefixes—continue this format (capitalize the tag and keep the subject ≤72 characters). Each PR should link the relevant Jira/issue, list functional changes, and include screenshots or screen recordings for UI updates. Note any required migrations or environment flag changes in the description, and wait for CI (Gradle checks) to pass before requesting review.

## Environment & Configuration
Populate `GOOGLE_SERVER_CLIENT_ID`, `MAPS_API_KEY`, and `BASE_URL_DEBUG` via `gradle.properties` or environment variables; avoid committing real secrets. When pointing to a local backend, update `BASE_URL_DEBUG` and pair it with the OpenAPI spec used for codegen to prevent contract drift. Always verify that release builds fall back to production URLs before merging.
