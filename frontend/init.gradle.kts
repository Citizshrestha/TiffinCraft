// Gradle initialization script to force JDK usage
// This ensures the correct JDK is used regardless of environment

import org.gradle.internal.jvm.Jvm

gradle.beforeProject {
    // Force Java home to the system JDK with jlink
    val javaHome = file("D:/DevTools/Java/jdk-21.0.5.11-hotspot")
    
    // Log which Java is being used
    logger.lifecycle("=================================================")
    logger.lifecycle("Forcing Gradle to use JDK: ${javaHome.absolutePath}")
    logger.lifecycle("Current Java: ${Jvm.current().javaHome}")
    logger.lifecycle("=================================================")
}

// Set Java toolchain to use JDK 21
allprojects {
    plugins.withId("com.android.application") {
        configure<com.android.build.api.dsl.CommonExtension<*, *, *, *, *, *>> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
}
