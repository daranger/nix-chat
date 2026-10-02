plugins {
    application
    id("org.openjfx.javafxplugin")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls")
}

dependencies {
    implementation(project(":common"))
    implementation("io.github.mkpaz:atlantafx-base:2.0.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("uz.nixchat.client.NixChatApp")
}
