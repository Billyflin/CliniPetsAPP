# Clinipets Android Application

This is an Android application built with Kotlin and Jetpack Compose. It leverages several modern Android development libraries and practices.

## Project Overview

The Clinipets application is an Android client that interacts with a backend API. It uses OpenAPI Generator to create the API client from `backend-openapi.yaml`. The application features:

*   **Modern UI:** Built with Jetpack Compose.
*   **Networking:** Utilizes Retrofit2 and OkHttp3 for API communication.
*   **Secure Authentication:** Integrates Google Sign-In with Credential Manager and uses AndroidX Security Crypto for secure JWT storage.
*   **Location Services:** Incorporates Google Maps for location-based functionalities.
*   **Data Persistence:** Employs Datastore Preferences for lightweight data storage.
*   **Asynchronous Operations:** Manages concurrency with Kotlin Coroutines.

## Building and Running

This project uses Gradle as its build system.

### Prerequisites

*   Android SDK (API Level 36)
*   Java Development Kit (JDK) 21
*   `GOOGLE_SERVER_CLIENT_ID`, `BASE_URL_DEBUG`, and `MAPS_API_KEY` should be provided via `gradle.properties` or environment variables.

### Build Commands

*   **Clean Project:**
    ```bash
    ./gradlew clean
    ```
*   **Build Debug APK:**
    ```bash
    ./gradlew assembleDebug
    ```
*   **Build Release APK:**
    ```bash
    ./gradlew assembleRelease
    ```
*   **Run Tests:**
    ```bash
    ./gradlew test
    ./gradlew connectedAndroidTest
    ```
*   **Generate OpenAPI Client:** This task is automatically run before `preBuild`.
    ```bash
    ./gradlew openApiGenerate
    ```

### Running the Application

To run the application on an emulator or a connected device, use Android Studio or the following command:

```bash
./gradlew installDebug
```

## Development Conventions

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **API Client Generation:** OpenAPI Generator from `backend-openapi.yaml`
*   **Dependency Management:** Gradle with `libs.versions.toml` (though not explicitly read, it's implied by `libs` aliases)
*   **Secure Storage:** AndroidX Security Crypto for sensitive data like JWTs.
*   **Environment Variables:** Sensitive keys and URLs are managed through `gradle.properties` or environment variables.
