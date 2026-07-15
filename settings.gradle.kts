fun getLocalProperty(key: String, file: String = "local.properties"): String? {
    val properties = java.util.Properties()
    val localProperties = File(file)
    if (!localProperties.isFile) return null
    java.io.InputStreamReader(java.io.FileInputStream(localProperties), Charsets.UTF_8)
        .use { reader -> properties.load(reader) }
    return properties.getProperty(key)
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

val env: MutableMap<String, String> = System.getenv()
val gprUser = env["GPR_USER"] ?: getLocalProperty("gpr.user")
val gprKey = env["GPR_KEY"] ?: getLocalProperty("gpr.key")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Fallback: karoo-ext von GitHub Packages (braucht gpr.user/gpr.key in local.properties oder GPR_* env)
        maven {
            url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
            credentials {
                username = gprUser
                password = gprKey
            }
        }
    }
}

rootProject.name = "Karoo TBT Beep"
include("app")
