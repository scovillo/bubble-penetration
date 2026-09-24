plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    js {
        nodejs()
        compilations["main"].packageJson {
            name = "@bubble/game-core"
            types = "index.d.ts"
        }
        binaries.library()
    }

    android {
        namespace = "org.codeberg.scovillo.bubble.core"
        compileSdk = 37
        minSdk = 14
        withHostTestBuilder {}.configure {}
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation("dev.whyoleg.cryptography:cryptography-core:0.6.0")
            implementation("dev.whyoleg.cryptography:cryptography-provider-optimal:0.6.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.named("jsNodeProductionLibraryDistribution") {
    // The generated package.json is required by backend tests and must be
    // recreated even when Gradle considers the distribution up to date.
    outputs.upToDateWhen { false }
    doLast {
        file("build/dist/js/productionLibrary/package.json").writeText(
            """{
              "name": "@bubble/game-core",
              "version": "0.0.0-local",
              "main": "index.js",
              "types": "index.d.ts"
            }
            """.trimIndent(),
        )
    }
}
