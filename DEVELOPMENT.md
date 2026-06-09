# Nwerf Development Guide

This document outlines the standard development workflows, branching strategy, and automated release pipelines for the Nwerf Android project. Whether you are a solo developer returning to the project after months away or onboarding new contributors, follow these conventions to maintain stability.

## Branching Strategy

The repository utilizes a strict two-branch system to protect the stable release:

1. **`main`**: The Stable Branch. 
   - Code on `main` is guaranteed to compile and be free of major crashes.
   - Do NOT push experimental features or untested code directly to `main`.
2. **`dev`**: The Active Development Branch.
   - All new features, cloud integrations, and bleeding-edge changes must be pushed here first.
   - Once a feature is verified working on `dev`, it can be merged into `main`.

## Commit Conventions

We strictly follow **Conventional Commits**. This makes the Git history highly readable and allows automated tools to generate changelogs.

Every commit message must be prefixed with a type:
- `feat:` for adding a new feature (e.g., `feat: integrate Firebase Firestore`)
- `fix:` for fixing a bug (e.g., `fix: resolve Android Lint compiler crash`)
- `docs:` for updating documentation (e.g., `docs: add DEVELOPMENT.md`)
- `ci:` for updating GitHub Actions or build scripts (e.g., `ci: upgrade to setup-gradle action`)
- `chore:` for routine maintenance tasks (e.g., `chore: update dependencies`)

*Example: `fix: disable R8 minification to prevent instant crash on launch`*

## CI/CD Pipeline

Nwerf uses a highly robust, automated GitHub Actions workflow (`android.yml`).

### What happens on every push?
Every time you push code to `main` or `dev`, the CI pipeline will run automatically:
1. **Setup**: Configures JDK 17 and initializes advanced Gradle caching (`gradle/actions/setup-gradle`).
2. **Static Analysis**: Runs Android Lint (`./gradlew lintDebug`). Note: We use `continue-on-error: true` so legacy warnings do not break the build.
3. **Testing**: Runs local unit tests (`./gradlew testDebugUnitTest`).
4. **Compilation**: Compiles the Debug APK and the Release APK.
5. **Artifacts**: Uploads the Debug APK as an artifact that expires in 14 days.

### How to Trigger a Public Release

The CI pipeline does **NOT** create a public GitHub Release for standard pushes. To generate a signed APK and publish it to the GitHub Releases page, you must use Git Tags.

The workflow listens specifically for tags starting with the letter `v`.

**To create a Pre-Release (from `dev` branch):**
1. Commit your changes to `dev`.
2. Create a tag with a suffix like `-dev`, `-alpha`, or `-beta`:
   ```bash
   git tag v0.2.0-dev
   git push origin v0.2.0-dev
   ```
3. The pipeline will automatically build the app, sign it using the repository secrets, and publish it as a "Pre-release" on GitHub.

**To create a Stable Release (from `main` branch):**
1. Merge your verified code into `main`.
2. Create a standard semantic version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. The pipeline will automatically build, sign, and publish it as a "Latest Release" on GitHub.

## Local Environment

If you need to compile the project locally without the CI:
- **Java**: Ensure `JAVA_HOME` points to JDK 17.
- **Local Properties**: You must define `AURA_HOST`, `AURA_ACCESS_KEY`, and `AURA_ACCESS_SECRET` in your `local.properties` file for the ACRCloud audio recognition feature to compile.
- **Firebase**: Ensure the `google-services.json` file is present in the `app/` directory.

### Disabling Minification
If you encounter instant runtime crashes (`ClassNotFoundException`), check if R8 minification is enabled in `build.gradle.kts`. When using complex serialization or Firebase dependencies without custom Proguard rules, it is often safer to set `isMinifyEnabled = false` for the release build type.
