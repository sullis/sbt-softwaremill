package com.softwaremill

import sbt._, Keys._
import xerial.sbt.Sonatype.SonatypeKeys._
import com.typesafe.sbt.SbtPgp.autoImportImpl.PgpKeys
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep
import sbtrelease.ReleaseStateTransformations._

import java.util.regex.Pattern

class Publish {
  object Release {
    def steps(organization: String): Seq[ReleaseStep] = Seq(
      checkSnapshotDependencies,
      inquireVersions,
      // publishing locally so that the pgp password prompt is displayed early
      // in the process
      releaseStepCommand("publishLocalSigned"),
      runClean,
      runTest,
      setReleaseVersion,
      updateVersionInDocs(organization),
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    )

    // based on https://github.com/EECOLOR/sbt-release-custom-steps/blob/master/src/main/scala/org/qirx/sbtrelease/UpdateVersionInFiles.scala
    def updateVersionInDocs(organization: String): ReleaseStep = { s: State =>
      val readmeFile            = file("README.md")
      val readme                = IO.read(readmeFile)
      val regexStr              = s""""$organization" %{1,2} "[\\w\\.-]+" % "([\\w\\.-]+)""""
      val currentVersionPattern = regexStr.r

      currentVersionPattern.findFirstMatchIn(readme) match {
        case Some(currentVersionInReadmeGroups) =>
          val currentVersionInReadme = currentVersionInReadmeGroups.group(1)

          val releaseVersion = s.get(versions).get._1

          val settings = Project.extract(s)
          val vcs      = settings.get(releaseVcs).get

          def replaceInFile(f: File): Unit = {
            s.log.info(s"Replacing $currentVersionInReadme with $releaseVersion in ${f.name}")

            val oldFile = IO.read(f)
            val newFile = oldFile.replaceAll(Pattern.quote(currentVersionInReadme), releaseVersion)
            IO.write(f, newFile)

            vcs.add(f.getAbsolutePath) !! s.log
          }

          def replaceDocsInDirectory(d: File) {
            Option(d.listFiles()).foreach(_.foreach { f =>
              if (f.isDirectory) {
                replaceDocsInDirectory(f)
              } else if (f.getName.endsWith(".rst") || f.getName.endsWith(".md")) {
                replaceInFile(f)
              }
            })
          }

          replaceInFile(readmeFile)
          replaceDocsInDirectory(file("doc"))
          replaceDocsInDirectory(file("docs"))

        case None =>
          s.log.warn(s"Current version not found in readme, skipping")
      }

      s
    }
  }

  lazy val ossPublishSettings = Seq(
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
    publishArtifact in Test := false,
    publishMavenStyle := true,
    sonatypeProfileName := "com.softwaremill",
    pomIncludeRepository := { _ =>
      false
    },
    organizationHomepage := Some(url("https://softwaremill.com")),
    homepage := Some(url("http://softwaremill.com/open-source")),
    licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/softwaremill/" + name.value),
        "scm:git:git@github.com/softwaremill/" + name.value + ".git"
      )
    ),
    autoAPIMappings := true,
    developers := List(
      Developer(
        id = "softwaremill",
        name = "SoftwareMill",
        email = "info@softwaremill.com",
        url = new URL("https://softwaremill.com")
      )
    ),
    // sbt-release
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseIgnoreUntrackedFiles := true,
    releaseProcess := Release.steps(organization.value)
  )

  lazy val noPublishSettings = Seq(publish := {}, publishLocal := {}, publishArtifact := false)
}

object Publish extends Publish
