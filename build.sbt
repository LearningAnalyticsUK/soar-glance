import sbtassembly.MergeStrategy
import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}

/**
  * Build wide settings
  */

//Use the typelevel compiler for partial unification
scalaOrganization in ThisBuild := "org.typelevel"
//TODO: Upgrade to 2.11.9
scalaVersion in ThisBuild := "2.11.8"

/**
  * Build dependencies
  *
  * NOTE: Dependencies which are used by modules which compile/cross-compile to scala.js must be declared using `%%%`
  */

//Separate seqs of dependencies into separate lazy values to convey intent more clearly
lazy val langFixes = Seq(
  libraryDependencies += "com.github.mpilquist" %%% "simulacrum" % "0.10.0",
  libraryDependencies += "org.typelevel" %%% "machinist" % "0.6.1",
  libraryDependencies += compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
  libraryDependencies += compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val testingDependencies = Seq(
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test",
  libraryDependencies += "org.typelevel" %%% "discipline" % "0.7.2" % "test"
)

lazy val altStdLib = Seq(
  libraryDependencies += "org.typelevel" %%% "cats" % "0.9.0",
  libraryDependencies += "co.fs2" %%% "fs2-core" % "0.9.5"
//  libraryDependencies += "io.monix" %%% "monix-eval" % "2.3.0",
//  libraryDependencies += "io.monix" %%% "monix-cats" % "2.3.0"
)

lazy val commonDependencies = langFixes ++ testingDependencies ++ altStdLib

//Lazy val defining dependencies common to modules containing spark batch jobs
lazy val commonSparkBatch = Seq(
  libraryDependencies += "org.apache.spark" %% "spark-core" % "2.1.0" % "provided",
  libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.1.0",
  libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.1.0",
  libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"
)

//Lazy val defining dependencies common to modules containing web servers
lazy val commonCirce = Seq(
  libraryDependencies += "io.circe" %%% "circe-generic" % "0.7.0",
  libraryDependencies += "io.circe" %%% "circe-core" % "0.7.0",
  libraryDependencies += "io.circe" %%% "circe-parser" % "0.7.0"
)

lazy val commonDoobie = Seq(
  libraryDependencies += "org.tpolecat" %% "doobie-core-cats" % "0.4.1",
  libraryDependencies += "org.tpolecat" %% "doobie-postgres-cats" % "0.4.1",
  libraryDependencies += "org.tpolecat" %% "doobie-scalatest-cats" % "0.4.1"
)

lazy val commonServer = Seq(
  libraryDependencies += "com.github.finagle" %% "finch-core" % "0.14.0",
  libraryDependencies += "com.github.finagle" %% "finch-circe" % "0.14.0"
)

/**
  * Common helper methods factoring out project definition boilerplate
  */

//Template of common settings shared by all modules
//TODO: refactor into JS and JVM settings.
lazy val soarSettings = Seq(
  version := "0.1-SNAPSHOT",
  organization := "uk.ac.ncl.la",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  fork in test := true,
  parallelExecution in Test := false,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ypartial-unification",
    "-Xfuture"
  )
)

def soarProject(name: String): Project = {
  Project(name, file(name))
    .settings(soarSettings:_*)
    .settings(commonDependencies:_*)
}

def soarCrossProject(name: String, tpe: CrossType): CrossProject = {
  CrossProject(name, file(name), tpe)
    .settings(soarSettings:_*)
    .settings(commonDependencies:_*)
}

//Method defining common merge strategy for duplicate files when constructing executable jars using assembly
def commonAssembly(main: String, jar: String) = Seq(
  mainClass in assembly := Some(main),
  assemblyJarName in assembly := jar,
  assemblyMergeStrategy in assembly := {
      case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
      case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
      case PathList("javax", "inject", xs @ _*) => MergeStrategy.last
      case PathList("org", "apache", xs @ _*) => MergeStrategy.last
      case PathList("org", "aopalliance", xs @ _*) => MergeStrategy.last
      case PathList("org", "slf4j", "impl", xs @ _*) => MergeStrategy.last
      case PathList("com", "google", xs @ _*) => MergeStrategy.last
      case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
      case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
      case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
      case "about.html" => MergeStrategy.rename
      case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
      case "META-INF/mailcap" => MergeStrategy.last
      case "META-INF/mimetypes.default" => MergeStrategy.last
      case "plugin.properties" => MergeStrategy.last
      case "log4j.properties" => MergeStrategy.last
      case "overview.html" => MergeStrategy.rename
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
)

/**
  * Definition of modules
  */

//Core module of the project - any commonly depended code will be placed here.
lazy val core = soarCrossProject("core", CrossType.Pure)
  .settings(name := "Soar Core", moduleName := "soar-core")

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

//Module which creates the model training spark job when built
//This Module is JVM only - does this mean it should depend on the JVM version of the core module?
lazy val model = soarProject("model")
  .dependsOn(coreJVM)
  .settings(
    name := "Soar Model Generator",
    moduleName := "soar-model",
    libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0",
    commonAssembly("uk.ac.ncl.la.soar.model.ScorePredictor", "model.jar"))
  .settings(commonSparkBatch:_*)

//Module which contains code for the empirical evaluation of Soar, and an explanation of its methodology
lazy val glanceCore = soarCrossProject("glance-core", CrossType.Full)
  .dependsOn(core)
  .settings(
    name := "Soar Glance Core",
    moduleName := "soar-glance-core",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "shared" / "main" / "scala")
  .settings(commonDoobie:_*)

lazy val glanceCoreJS = glanceCore.js
lazy val glanceCoreJVM = glanceCore.jvm

//Also JVM only module. Depend on coreJVM and glanceJVM?
lazy val glanceCli = soarProject("glance-cli")
  .dependsOn(coreJVM, glanceCoreJVM)
  .settings(
    name := "Soar Glance CLI",
    moduleName := "soar-glance-cli",
    libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0",
    commonAssembly("uk.ac.ncl.la.soar.glance.cli.Main", "soar-glance-cli.jar"))
  .settings(commonSparkBatch:_*)

lazy val glanceWeb = soarCrossProject("glance-web", CrossType.Full)
  .dependsOn(core, glanceCore)
  .settings(
    name := "Soar Glance Web",
    moduleName := "soar-glance-web",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "shared" / "main" / "scala")
  .settings(commonAssembly("uk.ac.ncl.la.soar.glance.server.Main", "soar-glance-web.jar"))
  .settings(soarSettings:_*)
  .settings(commonCirce:_*)
  .jvmSettings(commonServer:_*)
  .jsSettings(
    libraryDependencies ++= Seq (
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "org.singlespaced" %%% "scalajs-d3" % "0.3.4",
      "org.webjars" % "bootstrap" % "3.3.7-1"
    ),
    scalaJSUseMainModuleInitializer := true)
  .enablePlugins(ScalaJSPlugin)

lazy val glanceWebJS = glanceWeb.js
lazy val glanceWebJVM = glanceWeb.jvm

//Add some command aliases for testing/compiling all modules, rather than aggregating tasks from root indiscriminately
addCommandAlias("testAll", "; core/test; model/test; glance-core/test")
addCommandAlias("compileAll", ";core/compile; model/compile; glance-core/compile")
