lazy val commonSettings = Seq(
  organization := "org.jug",
  version := "1.0.0",
  scalaVersion := "2.11.6",
  resolvers ++= Seq(
    Resolver.mavenLocal,
    "Vaadin add-ons repository" at "https://maven.vaadin.com/vaadin-addons"
  )
)

lazy val vs_shared = (project in file("vs-shared"))
  .settings(commonSettings)

lazy val vs_frontend = (project in file("vs-frontend"))
  .settings(commonSettings)
  .settings(vaadinSettings)
  .settings(
    // fix vaadin plugin directory locations
    vaadinThemesDir <<= sourceDirectory(sd => Seq(sd / "main" / "resources" / "VAADIN" / "themes")),
    target in compileVaadinThemes := (classDirectory in Compile).value / "VAADIN" / "themes",
    resourceGenerators in Compile <+= compileVaadinThemes,
    // remove unnecessary .scss and /styles.css
    mappings in (Compile,packageBin) ~= {
      (ms: Seq[(File, String)]) => ms filter {
        case (_, toPath) => !(toPath.endsWith(".scss") || toPath.startsWith("styles.css"))
      }
    },
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.6",

      // akka
      "com.typesafe.akka" %% "akka-actor" % "2.3.9",
      "com.typesafe.akka" %% "akka-kernel" % "2.3.9",
      "com.typesafe.akka" %% "akka-remote" % "2.3.9",

      // spring boot with undertow
      "javax.inject" % "javax.inject" % "1",
      "org.springframework.boot" % "spring-boot-starter" % "1.2.4.RELEASE" exclude(
        "commons-logging", "commons-logging"
        ),
      "org.springframework.boot" % "spring-boot-starter-web" % "1.2.4.RELEASE" exclude(
        "org.hibernate", "hibernate-validator"
        ) exclude(
        "org.springframework.boot", "spring-boot-starter-tomcat"
        ) exclude(
        "org.springframework", "spring-webmvc"
        ),
      "org.springframework.boot" % "spring-boot-starter-undertow" % "1.2.4.RELEASE",

      // vaadin
      "javax.servlet" % "javax.servlet-api" % "3.1.0",
      "com.vaadin" % "vaadin-server" % "7.4.8" exclude(
        "commons-logging", "commons-logging"
        ),
      "com.vaadin" % "vaadin-client-compiled" % "7.4.8",
      "com.vaadin" % "vaadin-push" % "7.4.8",
      "com.vaadin" % "vaadin-themes" % "7.4.8",
      "com.vaadin" % "vaadin-spring-boot" % "1.0.0.beta3",
      "org.vaadin.addons" % "rinne" % "0.3.0"
    )
  )
  .dependsOn(vs_shared)

lazy val vs_backend = (project in file("vs-backend"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      // akka
      "com.typesafe.akka" %% "akka-actor" % "2.3.9",
      "com.typesafe.akka" %% "akka-kernel" % "2.3.9",
      "com.typesafe.akka" %% "akka-remote" % "2.3.9",

      // spring boot with jdbc
      "javax.inject" % "javax.inject" % "1",
      "org.springframework.boot" % "spring-boot-starter" % "1.2.4.RELEASE" exclude(
        "commons-logging", "commons-logging"
        ),
      "org.springframework.boot" % "spring-boot-starter-jdbc" % "1.2.4.RELEASE",

      "com.typesafe.slick" %% "slick" % "2.1.0",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc41" exclude(
        "org.slf4j", "slf4j-simple"
        )
    )
  )
  .dependsOn(vs_shared)

lazy val vs_root = (project in file(".")).aggregate(vs_shared, vs_frontend, vs_backend)