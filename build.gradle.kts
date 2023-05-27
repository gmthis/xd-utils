import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.libsDirectory

plugins{
    kotlin("jvm") version "1.8.21" apply false
    id("idea")
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

allprojects{
    group = "xd.util"
}

subprojects{
    apply(plugin = "kotlin")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories{
        mavenCentral()
    }
}

tasks.register("jar"){
    group = "build"
    val outputDir = File("output")
    if (outputDir.exists().not()){
        outputDir.mkdirs()
    }
    subprojects.forEach{
        dependsOn(it.tasks.getAt("jar"))
        doLast {
            val directory = it.libsDirectory.get()
            directory.asFile.listFiles().forEach { file ->
                file.copyTo(File("${outputDir.path}/${file.name}"), true)
            }
        }
    }
}

tasks.register("shadowJar"){
    group = "build"
    val outputDir = File("output")
    subprojects.forEach{
        dependsOn(it.tasks.getAt("shadowJar"))
        doLast {
            val directory = it.libsDirectory.get()
            directory.asFile.listFiles().forEach { file ->
                file.copyTo(File("${outputDir.path}/${file.name}"), true)
            }
        }
    }
}

tasks.register("clean"){
    group = "build"
    doLast {
        val outputDir = File("output")
        if (outputDir.exists()){
            delete(project.files(outputDir.list(), outputDir.path))
        }
    }
}

idea{
    module{
        isDownloadSources = true
    }
}