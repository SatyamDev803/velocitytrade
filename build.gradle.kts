plugins {
    id("java")
    id("application")
}

group = "com.velocitytrade"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // CORE: LMAX Disruptor
    implementation("com.lmax:disruptor:4.0.0")

    // PERSISTENCE: Chronicle Queue
    implementation("net.openhft:chronicle-queue:5.24ea4")

    // METRICS: HdrHistogram + Prometheus
    implementation("org.hdrhistogram:HdrHistogram:2.1.12")
    implementation("io.prometheus:simpleclient:0.16.0")
    implementation("io.prometheus:simpleclient_hotspot:0.16.0")
    implementation("io.prometheus:simpleclient_httpserver:0.16.0")

    // HTTP CLIENT: OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // HTTP SERVER: Undertow
    implementation("io.undertow:undertow-core:2.3.18.Final")
    implementation("io.undertow:undertow-websockets-jsr:2.3.18.Final")

    // JSON: Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    // UTILITIES: Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    // LOGGING: SLF4J + Logback
    implementation("org.slf4j:slf4j-api:2.0.16")  // Was 2.0.9
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // CONFIGURATION: SnakeYAML
    implementation("org.yaml:snakeyaml:2.2")

    // TESTING: JUnit 5 (Platform + Jupiter)
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.velocitytrade.Main")

    applicationDefaultJvmArgs = listOf(
        "-Xms2g",
        "-Xmx2g",
        "-XX:+UseZGC",
        "-XX:+AlwaysPreTouch",
        "-Xlog:gc*:file=logs/gc.log:time,uptime,level,tags",
        "-XX:+UseNUMA",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=logs/heap-dump.hprof"
    )
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    maxHeapSize = "1g"

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.velocitytrade.Main",
            "Implementation-Title" to "VelocityTrade",
            "Implementation-Version" to project.version
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
