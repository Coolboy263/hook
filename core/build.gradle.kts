plugins {
    id("com.android.library")
    id("maven-publish")
}

dependencies {
    implementation("org.lsposed.lsplant:lsplant:4.0-aliucord.1")
    implementation("io.github.vvb2060.ndk:dobby:1.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test:runner:1.4.0")
}

android {
    compileSdk = 31
    buildToolsVersion = "32.0.0"
    ndkVersion = "24.0.8215888"

    buildFeatures {
        buildConfig = false
        prefab = true
    }

    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            for (item in abiFilters) {
                abiFilters.remove(item)
            }
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.18.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

afterEvaluate {
    publishing {
        publications {
            register(project.name, MavenPublication::class.java) {
                from(components["release"])
            }

            repositories {
                maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Coolboy263/hook")
                credentials {
                    username = project.findProperty("gpr.user") as String
                    password = project.findProperty("gpr.key") as String
                }
            }
            }
        }
    }
}