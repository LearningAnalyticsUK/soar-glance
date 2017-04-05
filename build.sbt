import sbtassembly.MergeStrategy
//Use the typelevel compiler for partial unification
scalaOrganization in ThisBuild := "org.typelevel"
scalaVersion in ThisBuild := "2.11.8"

//Separate seqs of dependencies into separate lazy values to convey intent more clearly
lazy val langFixes = Seq(
  "com.github.mpilquist" %% "simulacrum" % "0.10.0",
  "org.typelevel" %% "machinist" % "0.6.1",
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val testingDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.typelevel" %% "discipline" % "0.7.2" % "test"
)

lazy val altStdLib = Seq(
  "org.typelevel" %% "cats" % "0.9.0",
  "io.monix" %% "monix" % "2.2.1",
  "io.monix" %% "monix-cats" % "2.2.1",
  "com.jsuereth" %% "scala-arm" % "2.0"
)

//Template of common settings shared by all modules
def SoarProject(name: String): Project = {
  Project(name, file(name)).settings(
    version := "0.1-SNAPSHOT",
    organization := "uk.ac.ncl.la",
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
    ),
    libraryDependencies ++= (langFixes ++ testingDependencies ++ altStdLib),
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
}

//Core module of the project - any commonly depended code will be placed here.
lazy val core = SoarProject("core")
  .settings(name := "Soar Core", moduleName := "soar-core")

//Lazy vals defining dependencies common to modules containing spark batch jobs
lazy val commonSparkBatch = Seq(
  "org.apache.spark" %% "spark-core" % "2.1.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "2.1.0",
  "org.apache.spark" %% "spark-mllib" % "2.1.0",
  "com.github.scopt" %% "scopt" % "3.5.0"
)

//Lazy vals defining dependencies common to modules containing web servers
lazy val commonServer = Seq(
  "com.github.finagle" %% "finch-core" % "0.14.0",
  "com.github.finagle" %% "finch-circe" % "0.14.0",
  "io.circe" %% "circe-generic" % "0.7.0"
)

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

//Module which creates the model training spark job when built
lazy val model = SoarProject("model")
  .dependsOn(core)
  .settings(
    name := "Soar Model Generator",
    moduleName := "soar-model",
    libraryDependencies ++= commonSparkBatch,
    commonAssembly("uk.ac.ncl.la.soar.model.ScorePredictor", "model.jar")
  )

//Module which contains code for the empirical evaluation of Soar, and an explanation of its methodology
lazy val evaluation = SoarProject("evaluation")
  .dependsOn(core)
  .settings(
    name := "Soar Evaluation",
    moduleName := "soar-eval"
  )

lazy val evaluation-cli = SoarProject("evaluation-cli")
  .dependsOn(core, evaluation)
  .settings(
    name := "Soar Evaluation CLI",
    moduleName := "soar-eval-cli",
    libraryDependencies ++= commonSparkBatch,
    commonAssembly("uk.ac.ncl.la.soar.eval.cli.Main", "soar-eval-cli.jar")
  )

lazy val evaluation-server = SoarProject("evaluation-server")
  .dependsOn(core, evaluation)
  .settings(
    name := "Soar Evaluation Server",
    moduleName := "soar-eval-server",
    libraryDependencies ++= commonServer,
    commonAssembly("uk.ac.ncl.la.soar.eval.server.Main", "soar-eval-server.jar")
  )

//Add some command aliases for testing/compiling all modules, rather than aggregating tasks from root indiscriminately
addCommandAlias("testAll", "; core/test; model/test; evaluation/test")
addCommandAlias("compileAll", ";core/compile; model/compile; evaluation/compile")
