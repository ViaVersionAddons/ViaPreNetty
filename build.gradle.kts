plugins {
    java
}

group = "viaprenetty"
version = "0.2.0"

repositories {
    mavenCentral()
    maven("https://repo.viaversion.com")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.viaversion:viaversion:5.12.2")
    compileOnly("com.viaversion:viabackwards:5.12.2")
    compileOnly("com.viaversion:viarewind:4.2.0")
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.1.68.Final")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.jar {
    archiveFileName.set("ViaPreNetty.jar")
}
