# Fix Build Error: Missing jlink Executable

The build is failing because Gradle is attempting to use a non-existent JDK path associated with a VS Code Java extension: `/home/leminhhieu/.vscode/extensions/redhat.java-1.55.0-linux-x64/jre/21.0.11-linux-x86_64/bin/jlink`. This path likely leaked into the environment or is a stale configuration.

## Proposed Changes

I will explicitly set the Gradle JDK path in the project's `gradle.properties` file to a valid JDK installed on the system. This will override any incorrect environment variables or IDE settings that are pointing to the missing VS Code JRE.

### [Component Name]

#### [MODIFY] [gradle.properties](file:///home/leminhhieu/personal/Chiikaiwa-Mobile/gradle.properties)
- Add `org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64` to ensure Gradle uses a valid JDK 21 installation.

## Verification Plan

### Automated Tests
- I will attempt to run a Gradle sync or a small build task (e.g., `./gradlew help` or `gradle_sync`) to verify that the error no longer occurs.
- Since I cannot run `gradlew` directly due to permission issues (and I should use built-in tools), I will use `gradle_sync` or `gradle_build` if applicable.

### Manual Verification
- The user can verify by rebuilding the project in Android Studio.
