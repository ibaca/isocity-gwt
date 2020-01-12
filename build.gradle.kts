plugins {
    java
    `maven-publish`
    id("org.ajoberstar.git-publish") version "3.0.0-rc.1"
}

group = "com.intendia"
version = "1.0-SNAPSHOT"
description = "Isometric City board in GWT."

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11
tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/google-snapshots/") }
    maven { url = uri("https://raw.githubusercontent.com/intendia-oss/rxjava-gwt/mvn-repo/") }
    mavenLocal()
}

dependencies {
    implementation(enforcedPlatform("com.google.gwt:gwt:HEAD-SNAPSHOT"))
    implementation("com.google.gwt:gwt-user")
    implementation("com.google.gwt:gwt-dev")
    implementation("com.google.elemental2:elemental2-dom:1.0.0")
}

fun gwtClasspath() = sourceSets.main.get().run {
    files(java.srcDirs, resources.srcDirs, compileClasspath)
}

tasks.register<JavaExec>("gwtCompile") {
    dependsOn(tasks.classes)
    description = "GWT compiler"
    inputs.files(gwtClasspath())
    outputs.dir("$buildDir/gwtCompile/war/isocity")
    workingDir("$buildDir/gwtCompile")
    doFirst { workingDir.mkdirs() }
    main = "com.google.gwt.dev.Compiler"
    classpath(gwtClasspath())
    args = listOf("-sourceLevel", "11", "-draftCompile", "-failOnError", "isocity.IsoCity")
}

tasks.assemble {
    dependsOn("gwtCompile")
}

tasks.register<JavaExec>("gwtServe") {
    dependsOn(tasks.classes)
    description = "GWT serve"
    workingDir("$buildDir/gwtServe")
    doFirst { workingDir.mkdirs() }
    main = "com.google.gwt.dev.DevMode"
    classpath(gwtClasspath())
    args = listOf("-sourceLevel", "11", "-failOnError", "isocity.IsoCity")
}

gitPublish {
    repoUri.set("git@github.com:ibaca/isocity-gwt.git")
    branch.set("gh-pages")
    contents {
        from(tasks.named("gwtCompile"))
    }
}
