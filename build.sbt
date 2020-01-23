lazy val Scala212 = "2.12.10"

lazy val configVersion = "1.3.2"
lazy val catsVersion = "2.1.0"
lazy val playVersion = "2.7.2"
lazy val tikaVersion = "1.21"
lazy val betterFilesVersion = "3.8.0"
lazy val akkaVersion = "2.5.25"

lazy val IntegrationTest = config("it") extend Test

lazy val root = (project in file(".")).
  configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    name              := "common",
    organization := "io.mdcatapult.doclib",
    scalaVersion      := "2.12.10",
    coverageEnabled   := false,
    crossScalaVersions  := Scala212 :: Nil,
    scalacOptions     ++= Seq("-Ypartial-unification"),
    resolvers         ++= Seq(
      "MDC Nexus Releases" at "https://nexus.mdcatapult.io/repository/maven-releases/",
      "MDC Nexus Snapshots" at "https://nexus.mdcatapult.io/repository/maven-snapshots/"),
    credentials       += {
      val nexusPassword = sys.env.get("NEXUS_PASSWORD")
      if ( nexusPassword.nonEmpty ) {
        Credentials("Sonatype Nexus Repository Manager", "nexus.mdcatapult.io", "gitlab", nexusPassword.get)
      } else {
        Credentials(Path.userHome / ".sbt" / ".credentials")
      }
    },
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic"                  % "3.0.5",
      "org.scalatest" %% "scalatest"                  % "3.0.5" % "it, test",
      "org.scalamock" %% "scalamock"                  % "4.3.0" % "it, test",
      "org.scalacheck" %% "scalacheck"                % "1.14.1" % Test,
      "com.typesafe.akka" %% "akka-testkit"           % akkaVersion % "it, test",
      "com.typesafe.play" %% "play-json"              % playVersion,
      "com.typesafe" % "config"                       % configVersion,
      "org.typelevel" %% "cats-macros"                % catsVersion,
      "org.typelevel" %% "cats-kernel"                % catsVersion,
      "org.typelevel" %% "cats-core"                  % catsVersion,
      "io.lemonlabs" %% "scala-uri"                   % "1.4.5",
      "io.mdcatapult.klein" %% "queue"                % "0.0.14",
      "io.mdcatapult.klein" %% "mongo"                % "0.0.4",
      "com.github.scopt" %% "scopt"                   % "4.0.0-RC2",
      "commons-io" % "commons-io"                     % "2.6",
      "com.chuusai" %% "shapeless"                    % "2.3.3",
      "org.apache.commons" % "commons-compress"       % "1.18",
      "org.apache.tika" % "tika-core"                 % tikaVersion,
      "org.apache.tika" % "tika-parsers"              % tikaVersion,
      "org.apache.tika" % "tika-langdetect"           % tikaVersion,
      "org.apache.pdfbox" % "jbig2-imageio"           % "3.0.2",
      "com.github.jai-imageio" % "jai-imageio-jpeg2000" % "1.3.0",
      "org.xerial" % "sqlite-jdbc"                      % "3.25.2",
      "com.github.pathikrit"  %% "better-files"       % betterFilesVersion

    )
  )
  .settings(
    publishSettings: _*
  )

lazy val publishSettings = Seq(
  publishTo := {
    val version = if (isSnapshot.value) "snapshots" else "releases"
    Some("MDC Maven Repo" at s"https://nexus.mdcatapult.io/repository/maven-$version/")
  },
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)