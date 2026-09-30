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
}

application {
    mainClass.set("uz.nixchat.client.NixChatApp")
}
