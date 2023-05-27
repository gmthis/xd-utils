plugins{

}

version = "0.1"

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))

    implementation(kotlinx.coroutines)
}

tasks.test{
    useJUnitPlatform()
}

kotlin{
    jvmToolchain(17)
}