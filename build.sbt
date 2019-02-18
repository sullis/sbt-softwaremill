import com.softwaremill.Publish
import sbt.addSbtPlugin
import sbt._
import Keys._
import sbtsoftwaremill.BuildInfo

val commonSettings = Publish.ossPublishSettings ++ Seq(
  scalaVersion := "2.12.8",
  organization := "com.softwaremill.sbt-softwaremill",
  sbtVersion in Global := {
    scalaBinaryVersion.value match {
      case "2.10" => "0.13.17"
      case "2.12" => "1.2.8"
    }
  }
)

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(
    name         := "sbt-softwaremill",
    description  := "Common build configuration for SBT projects",
    sbtPlugin    := true,
    libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.6.0"
  )
  .settings(
    addSbtPlugin("com.jsuereth"      % "sbt-pgp"          % BuildInfo.sbtPgpVersion),
    addSbtPlugin("com.github.gseitz" % "sbt-release"      % BuildInfo.sbtReleaseVersion),
    addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"     % BuildInfo.sbtSonatypeVersion),
    addSbtPlugin("org.wartremover"   % "sbt-wartremover"  % "2.4.1"),
    addSbtPlugin("com.geirsson"      % "sbt-scalafmt"     % "1.5.1"),
    addSbtPlugin("io.spray"          % "sbt-revolver"     % "0.9.1"),
    addSbtPlugin("io.get-coursier"   % "sbt-coursier"     % "1.0.3"),
    addSbtPlugin("com.dwijnand"      % "sbt-reloadquick"  % "1.0.0"),
    addSbtPlugin("com.softwaremill.clippy" % "plugin-sbt" % "0.6.0"),
    addSbtPlugin("com.timushev.sbt"  % "sbt-updates"      % "0.4.0")
  )