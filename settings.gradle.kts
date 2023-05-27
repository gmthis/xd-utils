rootProject.name = "xd-utils"
include("event-bus")

dependencyResolutionManagement{
    versionCatalogs{
        create("kotlinx"){
            library("coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
        }
    }
}