import sbtdocker.Instructions._
import ReleaseTransformations._

lazy val buildSettings = Seq(
  organization := "ru.pavkin.ihavemoney",
  scalaVersion := "2.12.2"
)

lazy val releaseSettings = Seq(releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  releaseStepCommand("writeBackend/dockerBuildAndPush"),
  releaseStepCommand("readBackend/dockerBuildAndPush"),
  releaseStepCommand("readFrontend/dockerBuildAndPush"),
  releaseStepCommand("writeFrontend/dockerBuildAndPush"),
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
))

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions,
  testOptions in Test += Tests.Argument("-oF"),
  scalacOptions in(Compile, console) := compilerOptions,
  scalacOptions in(Compile, test) := compilerOptions,
  resolvers ++= Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
    Resolver.jcenterRepo,
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val allSettings = buildSettings ++ baseSettings

lazy val postgreSQLVersion = "42.0.0"
lazy val funCQRSVersion = "0.4.11"
lazy val shapelessVersion = "2.3.2"
lazy val catsVersion = "0.9.0"
lazy val enumeratumVersion = "1.5.12"
lazy val simulacrumVersion = "0.10.0"
lazy val circeVersion = "0.7.0"
lazy val akkaVersion = "2.4.17"
lazy val akkaPersistenceJDBCVersion = "2.4.17.1"
lazy val akkaHttpVersion = "10.0.5"
lazy val akkaHttpCorsVersion = "0.2.1"
lazy val akkaHttpCirceVersion = "1.15.0"
lazy val jwtCirceVersion = "0.12.1"
lazy val scalaCheckVersion = "1.13.4"
lazy val scalaTestVersion = "3.0.1"

lazy val journal_db_host = sys.props.getOrElse("ihavemoney_writeback_db_host", "127.0.0.1")
lazy val journal_db_port = sys.props.getOrElse("ihavemoney_writeback_db_port", "5432")
lazy val journal_db_name = sys.props.getOrElse("ihavemoney_writeback_db_name", "ihavemoney-write")
lazy val journal_db_user = sys.props.getOrElse("ihavemoney_writeback_db_user", "admin")
lazy val journal_db_password = sys.props.getOrElse("ihavemoney_writeback_db_password", "changeit")

lazy val read_db_host = sys.props.getOrElse("ihavemoney_readback_db_host", "127.0.0.1")
lazy val read_db_port = sys.props.getOrElse("ihavemoney_readback_db_port", "5432")
lazy val read_db_name = sys.props.getOrElse("ihavemoney_readback_db_name", "ihavemoney-read")
lazy val read_db_user = sys.props.getOrElse("ihavemoney_readback_db_user", "admin")
lazy val read_db_password = sys.props.getOrElse("ihavemoney_readback_db_password", "changeit")

lazy val writeback_host = sys.props.getOrElse("ihavemoney_writeback_host", "127.0.0.1")
lazy val writeback_port = sys.props.getOrElse("ihavemoney_writeback_port", "9101")

lazy val readback_host = sys.props.getOrElse("ihavemoney_readback_host", "127.0.0.1")
lazy val readback_port = sys.props.getOrElse("ihavemoney_readback_port", "9201")

lazy val writefront_host = sys.props.getOrElse("ihavemoney_writefront_host", "127.0.0.1")
lazy val writefront_http_port = sys.props.getOrElse("ihavemoney_writefront_http_port", "8101")
lazy val writefront_tcp_port = sys.props.getOrElse("ihavemoney_writefront_tcp_port", "10101")

lazy val readfront_host = sys.props.getOrElse("ihavemoney_readfront_host", "127.0.0.1")
lazy val readfront_http_port = sys.props.getOrElse("ihavemoney_readfront_http_port", "8201")
lazy val readfront_tcp_port = sys.props.getOrElse("ihavemoney_readfront_tcp_port", "10201")

lazy val testDependencies = libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test",
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
  "io.strongtyped" %% "fun-cqrs-test-kit" % funCQRSVersion % "test"
)

lazy val protobufSettings = PB.targets in Compile := Seq(
  scalapb.gen(grpc = false) -> (sourceManaged in Compile).value
)

lazy val iHaveMoney = project.in(file("."))
  .settings(buildSettings)
  .settings(releaseSettings)
  .aggregate(domainJVM, serialization, writeBackend, writeFrontend, readBackend, frontendProtocolJVM, readFrontend, jsApp, tests)

lazy val domain = crossProject.in(file("domain"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    moduleName := "domain",
    name := "domain",
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "ru.pavkin.ihavemoney"
  )
  .settings(allSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full),
      compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
      "com.github.mpilquist" %%% "simulacrum" % simulacrumVersion,
      "com.chuusai" %%% "shapeless" % shapelessVersion,
      "org.typelevel" %%% "cats-core" % catsVersion,
      "com.beachape" %%% "enumeratum" % enumeratumVersion
    )
  )
  .jvmSettings(libraryDependencies ++= Seq(
    "io.strongtyped" %% "fun-cqrs-core" % funCQRSVersion,
    "com.github.t3hnar" %% "scala-bcrypt" % "3.0",
    "com.pauldijou" %% "jwt-circe" % jwtCirceVersion
  ))
  .jvmSettings(testDependencies)

lazy val domainJVM = domain.jvm
lazy val domainJS = domain.js

lazy val serialization = project.in(file("serialization"))
  .settings(
    moduleName := "serialization",
    name := "serialization"
  )
  .settings(allSettings: _*)
  .settings(protobufSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.chuusai" %% "shapeless" % shapelessVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    )
  )
  .dependsOn(domainJVM)

lazy val writeBackend = project.in(file("write-backend"))
  .settings(
    moduleName := "write-backend",
    name := "write-backend"
  )
  .settings(allSettings: _*)
  .settings(
    flywayUrl := s"jdbc:postgresql://$journal_db_host:$journal_db_port/$journal_db_name",
    flywayUser := journal_db_user,
    flywayPassword := journal_db_password,
    flywayBaselineOnMigrate := true,
    flywayLocations := Seq("filesystem:write-backend/src/main/resources/db/migrations")
  )
  .settings(
    libraryDependencies ++= Seq(
      "io.strongtyped" %% "fun-cqrs-akka" % funCQRSVersion,
      "com.github.dnvriend" %% "akka-persistence-jdbc" % akkaPersistenceJDBCVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
      "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion,
      "org.postgresql" % "postgresql" % postgreSQLVersion,
      "org.apache.commons" % "commons-email" % "1.4"
    )
  )
  .settings(testDependencies)
  .settings(
    mainClass in assembly := Some("ru.pavkin.ihavemoney.writeback.WriteBackend"),
    assemblyJarName in assembly := "writeback.jar"
  )
  .enablePlugins(DockerPlugin)
  .settings(Seq(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = artifact.name
      val applicationConf = "application.conf"
      val resources = (resourceDirectory in Compile).value / applicationConf
      val entry = Seq(
        "java",
        s"-Dconfig.file=$applicationConf",
        "-jar",
        artifactTargetPath
      )
      new Dockerfile {
        from("openjdk:8-jre")
        env(
          "ihavemoney_writeback_host" → "127.0.0.1",
          "ihavemoney_writeback_port" → "9101",
          "ihavemoney_writeback_db_host" → "127.0.0.1",
          "ihavemoney_writeback_db_port" → "5432",
          "ihavemoney_writeback_db_name" → "ihavemoney-write",
          "ihavemoney_writeback_db_user" → "admin",
          "ihavemoney_writeback_db_password" → "changeit",
          "ihavemoney_writefront_host" → "127.0.0.1",
          "ihavemoney_writefront_http_port" → "8101",
          "ihavemoney_writeback_smtp_user" → "example@gmail.com",
          "ihavemoney_writeback_smtp_password" → "changeit"
        )
        copy(artifact, artifactTargetPath)
        copy(resources, applicationConf)
        addInstruction(Raw("expose", s"$$ihavemoney_writeback_port"))
        entryPoint(entry: _*)
      }
    },
    imageNames in docker := Seq(
      ImageName(s"vpavkin/ihavemoney-${name.value}:latest"),
      ImageName(s"vpavkin/ihavemoney-${name.value}:${(version in ThisBuild).value}")
    )
  ))
  .dependsOn(domainJVM, serialization)

lazy val writeFrontend = project.in(file("write-frontend"))
  .settings(
    moduleName := "write-frontend",
    name := "write-frontend"
  )
  .settings(allSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "ch.megard" %% "akka-http-cors" % akkaHttpCorsVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion
    )
  )
  .settings(testDependencies)
  .settings(
    mainClass in assembly := Some("ru.pavkin.ihavemoney.writefront.WriteFrontend"),
    assemblyJarName in assembly := "writefront.jar"
  )
  .enablePlugins(DockerPlugin)
  .settings(Seq(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = artifact.name
      val applicationConf = "application.conf"
      val resources = (resourceDirectory in Compile).value / applicationConf
      val entry = Seq(
        "java",
        s"-Dconfig.file=$applicationConf",
        "-jar",
        artifactTargetPath
      )
      new Dockerfile {
        from("openjdk:8-jre")
        env(
          "ihavemoney_writeback_host" → "127.0.0.1",
          "ihavemoney_writeback_port" → "9101",
          "ihavemoney_writefront_host" → "127.0.0.1",
          "ihavemoney_writefront_http_port" → "8101",
          "ihavemoney_writefront_tcp_port" → "10101",
          "ihavemoney_secret_key" → "changeit",
          "ihavemoney_unsafe_routes_enabled" → "true"
        )
        copy(artifact, artifactTargetPath)
        copy(resources, applicationConf)
        addInstruction(Raw("expose", s"$$ihavemoney_writefront_tcp_port"))
        addInstruction(Raw("expose", s"$$ihavemoney_writefront_http_port"))
        entryPoint(entry: _*)
      }
    },
    imageNames in docker := Seq(
      ImageName(s"vpavkin/ihavemoney-${name.value}:latest"),
      ImageName(s"vpavkin/ihavemoney-${name.value}:${(version in ThisBuild).value}")
    )
  ))
  .dependsOn(domainJVM, frontendProtocolJVM, serialization)

lazy val readBackend = project.in(file("read-backend"))
  .settings(
    moduleName := "read-backend",
    name := "read-backend"
  )
  .settings(allSettings: _*)
  .settings(
    flywayUrl := s"jdbc:postgresql://$read_db_host:$read_db_port/$read_db_name",
    flywayUser := read_db_user,
    flywayPassword := read_db_password,
    flywayBaselineOnMigrate := true,
    flywayLocations := Seq("filesystem:read-backend/src/main/resources/db/migrations")
  )
  .settings(
    libraryDependencies ++= Seq(
      "io.strongtyped" %% "fun-cqrs-akka" % funCQRSVersion,
      "com.github.dnvriend" %% "akka-persistence-jdbc" % akkaPersistenceJDBCVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
      "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion,
      "org.postgresql" % "postgresql" % postgreSQLVersion
    )
  )
  .settings(testDependencies)
  .settings(
    mainClass in assembly := Some("ru.pavkin.ihavemoney.readback.ReadBackend"),
    assemblyJarName in assembly := "readback.jar"
  )
  .enablePlugins(DockerPlugin)
  .settings(Seq(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = artifact.name
      val applicationConf = "application.conf"
      val resources = (resourceDirectory in Compile).value / applicationConf
      val entry = Seq(
        "java",
        s"-Dconfig.file=$applicationConf",
        "-jar",
        artifactTargetPath
      )
      new Dockerfile {
        from("openjdk:8-jre")
        env(
          "ihavemoney_writeback_db_user" → "admin",
          "ihavemoney_writeback_db_password" → "changeit",
          "ihavemoney_writeback_db_host" → "127.0.0.1",
          "ihavemoney_writeback_db_port" → "5432",
          "ihavemoney_writeback_db_name" → "ihavemoney-write",
          "ihavemoney_readback_db_user" → "admin",
          "ihavemoney_readback_db_password" → "changeit",
          "ihavemoney_readback_db_host" → "127.0.0.1",
          "ihavemoney_readback_db_port" → "5432",
          "ihavemoney_readback_db_name" → "ihavemoney-read",
          "ihavemoney_readback_host" → "127.0.0.1",
          "ihavemoney_readback_port" → "9201"
        )
        copy(artifact, artifactTargetPath)
        copy(resources, applicationConf)
        addInstruction(Raw("expose", s"$$ihavemoney_readback_port"))
        entryPoint(entry: _*)
      }
    },
    imageNames in docker := Seq(
      ImageName(s"vpavkin/ihavemoney-${name.value}:latest"),
      ImageName(s"vpavkin/ihavemoney-${name.value}:${(version in ThisBuild).value}")
    )
  ))
  .dependsOn(domainJVM, serialization)

lazy val frontendProtocol = crossProject.in(file("frontend-protocol"))
  .settings(
    moduleName := "frontend-protocol",
    name := "frontend-protocol"
  )
  .settings(allSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion
  ))
  .jsSettings(libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.0")
  .dependsOn(domain)

lazy val frontendProtocolJS = frontendProtocol.js
lazy val frontendProtocolJVM = frontendProtocol.jvm

lazy val readFrontend = project.in(file("read-frontend"))
  .settings(
    moduleName := "read-frontend",
    name := "read-frontend"
  )
  .settings(allSettings: _*)
  .settings(addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"))
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "ch.megard" %% "akka-http-cors" % akkaHttpCorsVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion,
      "com.norbitltd" %% "spoiwo" % "1.2.0"
    ),
    testDependencies
  )
  .settings(
    mainClass in assembly := Some("ru.pavkin.ihavemoney.readfront.ReadFrontend"),
    assemblyJarName in assembly := "readfront.jar"
  )
  .enablePlugins(DockerPlugin)
  .settings(Seq(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = artifact.name
      val applicationConf = "application.conf"
      val resourceFiles = (resources in Compile).value
      val entry = Seq(
        "java",
        s"-Dconfig.file=$applicationConf",
        "-jar",
        artifactTargetPath
      )
      new Dockerfile {
        from("openjdk:8-jre")
        env(
          "ihavemoney_readback_host" → "127.0.0.1",
          "ihavemoney_readback_port" → "9201",
          "ihavemoney_readfront_host" → "127.0.0.1",
          "ihavemoney_readfront_http_port" → "8201",
          "ihavemoney_readfront_tcp_port" → "10201",
          "ihavemoney_writefront_host" → "127.0.0.1",
          "ihavemoney_writefront_port" → "8101"
        )
        copy(artifact, artifactTargetPath)
        resourceFiles.foreach(copy(_, "/"))
        addInstruction(Raw("expose", s"$$ihavemoney_readfront_tcp_port"))
        addInstruction(Raw("expose", s"$$ihavemoney_readfront_http_port"))
        entryPoint(entry: _*)
      }
    },
    imageNames in docker := Seq(
      ImageName(s"vpavkin/ihavemoney-${name.value}:latest"),
      ImageName(s"vpavkin/ihavemoney-${name.value}:${(version in ThisBuild).value}")
    )
  ))
  .settings(
    (resourceGenerators in Compile) +=
      (packageJSDependencies in Compile in jsApp,
        fastOptJS in Compile in jsApp,
        packageScalaJSLauncher in Compile in jsApp)
        .map((f1, f2, f3) ⇒ Seq(f1, f2.data, f3.data)).taskValue
  )
  .dependsOn(domainJVM, serialization, frontendProtocolJVM)

lazy val jsApp = project.in(file("js-app"))
  .settings(
    moduleName := "js-app",
    name := "js-app"
  )
  .settings(allSettings: _*)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSStage in Test := FastOptStage,
    jsEnv := PhantomJSEnv().value,
    scalaJSOptimizerOptions in fastOptJS ~= {
      _.withDisableOptimizer(true)
    }
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % "0.11.3",
      "com.github.japgolly.scalajs-react" %%% "extra" % "0.11.3",
      "com.github.japgolly.scalacss" %%% "core" % "0.5.1",
      "com.github.japgolly.scalacss" %%% "ext-react" % "0.5.1",
      "io.suzaku" %%% "diode" % "1.1.1",
      "io.suzaku" %%% "diode-react" % "1.1.1",
      "org.querki" %%% "jquery-facade" % "1.0",
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion
    ),
    jsDependencies ++= Seq(
      "org.webjars.bower" % "react" % "15.0.1" / "react-with-addons.js"
        minified "react-with-addons.min.js"
        commonJSName "React",

      "org.webjars.bower" % "react" % "15.0.1" / "react-dom.js"
        minified "react-dom.min.js"
        dependsOn "react-with-addons.js"
        commonJSName "ReactDOM",

      "org.webjars" % "jquery" % "2.2.3" / "jquery.js"
        minified "jquery.min.js",

      "org.webjars" % "bootstrap" % "3.3.6" / "bootstrap.js"
        minified "bootstrap.min.js"
        dependsOn "jquery.js",

      "org.webjars.bower" % "md5" % "0.3.0" / "src/md5.js"
        minified "build/md5.min.js"
    ),
    persistLauncher in Compile := true
  )
  .dependsOn(frontendProtocolJS)

lazy val tests = project.in(file("tests"))
  .settings(
    description := "Tests",
    name := "tests"
  )
  .settings(allSettings: _*)
  .settings(testDependencies)
  .settings(
    fork := true
  )
  .dependsOn(
    domainJVM, serialization, writeBackend, writeFrontend, readBackend, frontendProtocolJVM, readFrontend
  )
