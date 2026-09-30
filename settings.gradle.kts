plugins {
    // Automatically downloads JDK 21 for teammates who don't have it installed
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "nix-chat"

include("common", "server", "client")
