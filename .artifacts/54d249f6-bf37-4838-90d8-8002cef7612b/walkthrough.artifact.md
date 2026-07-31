# Walkthrough - Build Error Fix

I have fixed the build error where Gradle was incorrectly looking for a `jlink` executable in a non-existent VS Code extension directory.

## Changes Made

### Gradle Configuration

#### [gradle.properties](file:///home/leminhhieu/personal/Chiikaiwa-Mobile/gradle.properties)

I added the `org.gradle.java.home` property to explicitly point Gradle to a valid JDK 21 installation on your system.

```diff
 org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
+org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64
```

## Verification Results

### Automated Tests
- Executed `gradle_sync`, which finished successfully. This confirms that Gradle can now find the necessary JDK tools and the previous `IllegalArgumentException` is resolved.

> [!TIP]
> If you ever change your system JDK or move the installation, you may need to update this path in `gradle.properties`. Alternatively, ensuring your `JAVA_HOME` environment variable is correctly set and exported in your shell profile (e.g., `~/.bashrc` or `~/.zshrc`) can also help prevent such issues in the future.
