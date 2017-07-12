import sbtassembly.MergeStrategy
import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}

/**
  * Build wide settings
  */
//Use the typelevel compiler for extra goodies
scalaOrganization in ThisBuild := "org.typelevel"
scalaVersion in ThisBuild := "2.11.8"
//TODO: Switch to Typelevel 4 for 2.11.11 when things settle down
////Should I enable this for all projects like this or only for Cross/JS Projects? Work out.
//enablePlugins(WorkbenchPlugin)

/**
  * Major dependency versions
  */


/**
  * Build dependencies
  *
  * NOTE: Dependencies which are used by modules which compile/cross-compile to scala.js must be declared using `%%%`
  */
lazy val langFixDeps = Seq(
  libraryDependencies += compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
  libraryDependencies += compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch),
  libraryDependencies += "com.github.mpilquist" %%% "simulacrum" % "0.10.0",
  libraryDependencies += "org.typelevel" %%% "machinist" % "0.6.1"
)

lazy val testingDeps = Seq(
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test",
  libraryDependencies += "org.typelevel" %%% "discipline" % "0.7.2" % "test"
)

lazy val altStdLibDeps = Seq(
  libraryDependencies += "org.typelevel" %%% "cats" % "0.9.0",
  libraryDependencies += "io.monix" %%% "monix-eval" % "2.3.0",
  libraryDependencies += "io.monix" %%% "monix-cats" % "2.3.0"
)

//Lazy val defining dependencies common to modules containing spark batch jobs
lazy val sparkBatchDeps = Seq(
  libraryDependencies += "org.apache.spark" %% "spark-core" % "2.1.0" % "provided",
  libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.1.0",
  libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.1.0",
  libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"
)

//Lazy vals defining dependencies common to modules containing web servers/clients
lazy val circeDeps = Seq(
  libraryDependencies += "io.circe" %%% "circe-generic" % "0.7.0",
  libraryDependencies += "io.circe" %%% "circe-core" % "0.7.0",
  libraryDependencies += "io.circe" %%% "circe-parser" % "0.7.0"
)

lazy val doobieDeps = Seq(
  libraryDependencies += "org.tpolecat" %% "doobie-core-cats" % "0.4.1",
  libraryDependencies += "org.tpolecat" %% "doobie-postgres-cats" % "0.4.1",
  libraryDependencies += "org.tpolecat" %% "doobie-scalatest-cats" % "0.4.1"
)

lazy val finchDeps = Seq(
  libraryDependencies += "com.github.finagle" %% "finch-core" % "0.14.0",
  libraryDependencies += "com.github.finagle" %% "finch-circe" % "0.14.0",
  libraryDependencies += "com.twitter"        %% "twitter-server" % "1.29.0"
)

//Lazy vals defining dependencies common to modules containing Javascript front ends
//TODO: Add jsDependencies commands to the below vals
lazy val scalaJSDeps = Seq(
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1",
  libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.1"
)

lazy val reactJSDeps = Seq(
  libraryDependencies += "com.github.japgolly.scalajs-react" %%% "core"           % "1.0.0",
  libraryDependencies += "com.github.japgolly.scalajs-react" %%% "extra"          % "1.0.0",
  libraryDependencies += "com.github.japgolly.scalajs-react" %%% "ext-cats"       % "1.0.0",
  libraryDependencies += "io.suzaku"                         %%% "diode"          % "1.1.2",
  libraryDependencies += "io.suzaku"                         %%% "diode-react"    % "1.1.2",
  libraryDependencies += "com.github.japgolly.scalacss"      %%% "core"           % "0.5.3",
  libraryDependencies += "com.github.japgolly.scalacss"      %%% "ext-react"      % "0.5.3"
)

lazy val commonDeps = langFixDeps ++ testingDeps ++ altStdLibDeps ++ circeDeps
lazy val commonBackendDeps = doobieDeps ++ finchDeps
lazy val commonFrontendDeps = scalaJSDeps ++ reactJSDeps


/**
  * Lazy vals containing Seqs of settings which are common to one or more modules build definitions
  */
lazy val commonSettings = Seq(
  version := "0.1-SNAPSHOT",
  organization := "uk.ac.ncl.la",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    "Twitter Maven" at "http://maven.twttr.com"
  ),
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

lazy val soarSettings = commonSettings ++ Seq(
  fork in test := true,
  parallelExecution in Test := false
)

lazy val soarJSSettings = commonSettings ++ Seq(
  parallelExecution := false
)

lazy val flywaySettings = Seq(
  flywayUrl  := "jdbc:postgresql:postgres",
  flywayUser := "postgres",
  flywayLocations := Seq(
    s"filesystem:${baseDirectory.value}/src/main/resources/db/migrations"
  )
)

/**
  * Common helper methods factoring out project definition boilerplate
  */
def soarProject(name: String): Project = {
  Project(name, file(name))
    .settings(soarSettings:_*)
    .settings(commonDeps:_*)
}

def soarCrossProject(name: String, tpe: CrossType): CrossProject = {
  CrossProject(name, file(name), tpe)
    .jvmSettings(soarSettings:_*)
    .jsSettings(soarJSSettings:_*)
    .settings(commonDeps:_*)
//    .settings(
//      //Work around for https://github.com/scala-js/scala-js/pull/2954
//      // Remove the dependency on the scalajs-compiler
//      libraryDependencies := libraryDependencies.value.filterNot(_.name == "scalajs-compiler"),
//      // And add a custom one
//      libraryDependencies += compilerPlugin("org.scala-js" % "scalajs-compiler" % "0.6.16" cross CrossVersion.patch)
//    )
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
    case "BUILD" => MergeStrategy.rename
    case "about.html" => MergeStrategy.rename
    case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
    case "META-INF/mailcap" => MergeStrategy.last
    case "META-INF/mimetypes.default" => MergeStrategy.last
    case "META-INF/io.netty.versions.properties" => MergeStrategy.last
    case "plugin.properties" => MergeStrategy.last
    case "log4j.properties" => MergeStrategy.last
    case "overview.html" => MergeStrategy.rename
    case "JS_DEPENDENCIES" => MergeStrategy.discard
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
lazy val model = soarProject("model")
  .dependsOn(coreJVM)
  .settings(
    name := "Soar Model Generator",
    moduleName := "soar-model",
    libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0",
    commonAssembly("uk.ac.ncl.la.soar.model.ScorePredictor", "model.jar"))
  .settings(sparkBatchDeps:_*)

//Module which contains code for the empirical evaluation of Soar, and an explanation of its methodology
lazy val glanceCore = soarCrossProject("glance-core", CrossType.Full)
  .dependsOn(core)
  .settings(
    name := "Soar Glance Core",
    moduleName := "soar-glance-core",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "shared" / "main" / "scala")
  .jvmSettings(doobieDeps:_*)
  .jvmSettings(libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.7.0")

lazy val glanceCoreJS = glanceCore.js
lazy val glanceCoreJVM = glanceCore.jvm

//TODO: Investigate intermittent heap space OOM error on assembly of this module
lazy val glanceCli = soarProject("glance-cli")
  .dependsOn(coreJVM, glanceCoreJVM)
  .settings(
    name := "Soar Glance CLI",
    moduleName := "soar-glance-cli",
    libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0",
    commonAssembly("uk.ac.ncl.la.soar.glance.cli.Main", "soar-glance-cli.jar"))
  .settings(sparkBatchDeps:_*)

//TODO: Fix this so that the js/jvm dependencies are separate and I can build a proper server jar

lazy val glanceWeb = soarCrossProject("glance-web", CrossType.Full)
  .settings(
    name := "Soar Glance Web",
    moduleName := "soar-glance-web",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "shared" / "main" / "scala")
  .jvmSettings(commonBackendDeps:_*)
  .jvmSettings(flywaySettings:_*)
  .jvmSettings(libraryDependencies += "org.flywaydb" % "flyway-core" % "4.0.3")
  .jsSettings(commonFrontendDeps:_*)
  .jsSettings(
    libraryDependencies ++= Seq (
      //What is the point of having these dependencies in libraryDependencies?
      "org.webjars" %   "bootstrap"  % "3.3.7-1",
      "org.webjars" %   "datatables" % "1.10.13"
    ),
    jsDependencies ++= Seq(
      "org.webjars.bower" % "react" % "15.5.4" / "react-with-addons.js" minified "react-with-addons.min.js",
      "org.webjars.bower" % "react" % "15.5.4" / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js",
      "org.webjars" % "jquery" % "1.11.1" / "jquery.js" minified "jquery.min.js",
      "org.webjars" % "bootstrap" % "3.3.7-1" / "bootstrap.js" minified "bootstrap.min.js" dependsOn "jquery.js",
      "org.webjars" % "datatables" % "1.10.13" / "jquery.dataTables.js" minified "jquery.dataTables.min.js" dependsOn "jquery.js",
      "org.webjars" % "datatables" % "1.10.13" / "dataTables.bootstrap.js" minified "dataTables.bootstrap.min.js" dependsOn "jquery.js",
      "org.webjars" % "chartjs" % "2.1.3" / "Chart.js" minified "Chart.min.js"
    ),
    scalaJSUseMainModuleInitializer := true)
  .enablePlugins(SbtWeb)
  .enablePlugins(WorkbenchPlugin)

lazy val glanceWebJS = glanceWeb.js
  .dependsOn(coreJS, glanceCoreJS)

lazy val glanceWebJVM = glanceWeb.jvm
  .dependsOn(coreJVM, glanceCoreJVM)
  .settings(
    (resources in Compile) += (fastOptJS in (glanceWebJS, Compile)).value.data,
    mainClass in Compile := Some("uk.ac.ncl.la.soar.glance.web.server.Main"))
  .settings(commonAssembly("uk.ac.ncl.la.soar.glance.web.server.Main", "soar-glance-web.jar"))

/**
  * Command Aliases to make using this sbt project from the console a little more palatable
  */
addCommandAlias("testAll", "; core/test; model/test; glance-core/test")
addCommandAlias("compileAll", ";coreJS/compile; coreJVM/compile; model/compile; glance-cli/compile; " +
  "glance-coreJS/compile; glance-coreJVM/compile; glance-webJS/compile; glance-webJVM/compile")
addCommandAlias("cleanGlance", "; coreJVM/clean; glance-coreJVM/clean; glance-webJVM/clean")
