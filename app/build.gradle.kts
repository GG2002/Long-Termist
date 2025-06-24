plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.cyc.yearlymemoir"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cyc.yearlymemoir"
        minSdk = 32
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }

    splits {
        abi {
            // 开启 ABI (CPU 架构) 拆分
            isEnable = true

            // 如果你想排除某些不常用的架构，可以取消注释
            reset() // 移除默认列表
            include("arm64-v8a")  // 指定要打包的架构

            // 对于通用 APK，所有架构都会被包含，但你也可以选择不生成它
            // universalApk false // 设置为 true 会额外生成一个包含所有 .so 的通用包
        }
    }

    packaging {
        resources {
            // 注意：sqlite-jdbc 可能没有专门的 android 版本，这种方法可能导致崩溃
            // 所以全排除然后依赖系统库是更安全的做法（如果还能跑的话）
            excludes += "/org/sqlite/native/Windows/**"
            excludes += "/org/sqlite/native/Mac/**"
            // 同时，JDBC 驱动还可能带有一些元数据文件，也可以一并排除
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.runtime.android)

    // Material Design 3
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.window)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose.jvmstubs)
    debugImplementation("androidx.compose.ui:ui-tooling")

    // 安卓小卡片
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // 无障碍模式工具库
    implementation("com.github.ven-coder.Assists:assists-base:v3.2.17")

    // To recognize Latin script
    // implementation("com.google.mlkit:text-recognition:16.0.1")
    // To recognize Chinese script
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    // datastore 数据持久化组件
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 图表组件
    implementation("io.github.ehsannarmani:compose-charts:0.1.7")
    // 阴历转换
    implementation("cn.6tail:lunar:1.6.3")
    // 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    // 定时任务
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    // SQLite JDBC driver
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    // MySQL JDBC driver
    implementation("mysql:mysql-connector-java:8.0.33")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}