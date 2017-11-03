import sbtassembly.MergeStrategy
import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}

/**
  * Build wide settings
  */
//Use the typelevel compiler for extra goodies
scalaOrganization in ThisBuild := "org.typelevel"
scalaVersion in ThisBuild := "2.11.11-bin-typelevel-4"

/**
  * Repeated scala dependency versions
  */
lazy val catsVersion = "0.9.0"
lazy val monixVersion = "2.3.0"
lazy val sparkVersion = "2.1.0"
lazy val circeVersion = "0.8.0"
lazy val doobieVersion = "0.4.1"
lazy val finchVersion = "0.15.1"
lazy val sjsExtVersion = "0.9.1"
lazy val sjsReactVersion = "1.0.0"
lazy val diodeVersion = "1.1.2"
lazy val scalaCSSVersion = "0.5.3"
lazy val pureConfigVersion = "0.7.2"
lazy val kantanVersion = "0.2.1"

/**
  * Repeated js dependency versions
  */
lazy val reactVersion = "15.5.4"
lazy val datatablesVersion = "1.10.13"
lazy val bootstrapVersion = "3.3.7-1"

/**
  * Build dependencies
  *
  * NOTE: Dependencies which are used by modules which compile/cross-compile to scala.js must be declared using `%%%`
  */
lazy val langFixDeps = Seq(
  libraryDependencies ++= Seq(
    compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch),
    "com.github.mpilquist" %%% "simulacrum" % "0.10.0",
    "org.typelevel" %%% "machinist" % "0.6.1")
)

lazy val langFixDepsJVM = Seq(
  libraryDependencies ++= Seq(
    compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch),
    "com.github.mpilquist" %% "simulacrum" % "0.10.0",
    "org.typelevel" %% "machinist" % "0.6.1")
)


lazy val testingDeps = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.3" % "test",
    "org.scalacheck" %%% "scalacheck" % "1.13.5" % "test",
    "org.typelevel" %%% "discipline" % "0.7.3" % "test")
)

lazy val testingDepsJVM = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.3" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
    "org.typelevel" %% "discipline" % "0.7.3" % "test")
)

lazy val altStdLibDeps = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel" %%% "cats" % catsVersion,
    "io.monix" %%% "monix-eval" % monixVersion,
    "io.monix" %%% "monix-cats" % monixVersion)
)

lazy val altStdLibDepsJVM = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats" % catsVersion,
    "io.monix" %% "monix-eval" % monixVersion,
    "io.monix" %% "monix-cats" % monixVersion)
)

//Lazy val defining dependencies common to modules containing spark batch jobs
lazy val sparkBatchDeps = Seq(
  libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,
    "com.github.scopt" %% "scopt" % "3.5.0")
)

//Lazy vals defining dependencies common to modules containing web servers/clients
lazy val circeDeps = Seq(
  libraryDependencies ++= Seq(
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion)
)

lazy val circeDepsJVM = Seq(
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion)
)

lazy val kantanDeps = Seq(
  libraryDependencies ++= Seq(
    "com.nrinaudo" %% "kantan.csv-java8" % kantanVersion,
    "com.nrinaudo" %% "kantan.csv-cats" % kantanVersion,
    "com.nrinaudo" %% "kantan.csv-generic" % kantanVersion
  )
)

lazy val doobieDeps = Seq(
  libraryDependencies ++= Seq(
    "org.tpolecat" %% "doobie-core-cats" % doobieVersion,
    "org.tpolecat" %% "doobie-postgres-cats" % doobieVersion,
    "org.tpolecat" %% "doobie-scalatest-cats" % doobieVersion)
)

lazy val finchDeps = Seq(
  libraryDependencies ++= Seq(
    "com.github.finagle" %% "finch-core" % finchVersion,
    "com.github.finagle" %% "finch-circe" % finchVersion,
    "com.twitter"        %% "twitter-server" % "1.29.0")
)

//Lazy vals defining dependencies common to modules containing Javascript front ends
//TODO: Add jsDependencies commands to the below vals
lazy val scalaJSDeps = Seq(
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % sjsExtVersion,
    "be.doeraene" %%% "scalajs-jquery" % sjsExtVersion)
)

lazy val reactJSDeps = Seq(
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "core"           % sjsReactVersion,
    "com.github.japgolly.scalajs-react" %%% "extra"          % sjsReactVersion,
    "com.github.japgolly.scalajs-react" %%% "ext-cats"       % sjsReactVersion,
    "io.suzaku"                         %%% "diode"          % diodeVersion,
    "io.suzaku"                         %%% "diode-react"    % diodeVersion,
    "com.github.japgolly.scalacss"      %%% "core"           % scalaCSSVersion,
    "com.github.japgolly.scalacss"      %%% "ext-react"      % scalaCSSVersion)
)

lazy val commonDeps = langFixDeps ++ testingDeps ++ altStdLibDeps ++ circeDeps
lazy val commonDepsJVM = langFixDepsJVM ++ testingDepsJVM ++ altStdLibDepsJVM ++ circeDepsJVM
lazy val commonBackendDeps = doobieDeps ++ finchDeps ++ kantanDeps
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

def flywaySettings(dbName: String) = Seq(
  flywayUrl  := s"jdbc:postgresql://glance-eval-db:8003/$dbName",
  flywayUser := "postgres",
  flywayLocations := Seq(
    s"filesystem:${baseDirectory.value}/src/main/resources/db/db.migrations"
  )
)

lazy val sjsCrossVersionPatch = Seq(
  //Work around for https://github.com/scala-js/scala-js/pull/2954
  // Remove the dependency on the scalajs-compiler
  libraryDependencies := libraryDependencies.value.filterNot(_.name == "scalajs-compiler"),
  // And add a custom one
  libraryDependencies += compilerPlugin("org.scala-js" % "scalajs-compiler" % "0.6.18" cross CrossVersion.patch)
)

/**
  * Common helper methods factoring out project definition boilerplate
  */
def soarProject(name: String): Project = {
  Project(name, file(name))
    .settings(soarSettings:_*)
    .settings(commonDepsJVM:_*)
}

def soarCrossProject(name: String, tpe: CrossType): CrossProject = {
  val proj = CrossProject(name, file(name), tpe)
    .jvmSettings(soarSettings:_*)
    .jvmSettings(commonDepsJVM:_*)
    .jsSettings(soarJSSettings:_*)
    .jsSettings(commonDeps:_*)

//  //Below is required because pure projects freak out if you mess with the sjs compiler, no idea why
//  tpe match {
//    case CrossType.Pure => proj.jsSettings(sjsCrossVersionPatch:_*)
//    case CrossType.Full => proj.settings(sjsCrossVersionPatch:_*)
//    case _ => proj
//  }
  proj
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
    case "application.conf" => MergeStrategy.first
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
  .jsSettings(sjsCrossVersionPatch:_*)

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

//Db module of the project - contains all database queries, db.db.migrations, definitions etc...
lazy val db = soarProject("db")
  .dependsOn(coreJVM)
  .settings(
    name := "Soar Db",
    moduleName := "soar-db",
    libraryDependencies ++= Seq(
      "org.flywaydb" % "flyway-core" % "4.0.3",
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats" % pureConfigVersion
    )
  )
  .settings(flywaySettings("soar"):_*)
  .settings(doobieDeps:_*)

//Db-clu module of the project - contains import export scripts for the soar database and depends upon the db module
lazy val dbCli = soarProject("db-cli")
  .dependsOn(coreJVM, db)
  .settings(
    name := "Soar Db Cli",
    moduleName := "soar-db-cli",
    libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0",
    commonAssembly("uk.ac.ncl.la.soar.db.cli.Main", "soar-db.jar"))


//Server module of the project - contains the finch API for serving soar data, used by glance, reports etc...
lazy val server = soarProject("server")
  .dependsOn(coreJVM, db)
  .settings(
    name := "Soar Server",
    moduleName := "soar-server",
    commonAssembly("uk.ac.ncl.la.soar.server.Main", "server.jar"))
  .settings(
    libraryDependencies ++= Seq(
      "org.flywaydb" % "flyway-core" % "4.0.3",
      "com.github.pureconfig" %% "pureconfig" % "0.7.0"))
  .settings(commonBackendDeps:_*)
  .settings(flywaySettings("soar"):_*)

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
  .settings(
    name := "Soar Glance Core",
    moduleName := "soar-glance-core" )
  .jsSettings(sjsCrossVersionPatch:_*)

lazy val glanceCoreJS = glanceCore.js
  .dependsOn(coreJS)

lazy val glanceCoreJVM = glanceCore.jvm
  .enablePlugins(DockerPlugin)
  .dependsOn(coreJVM, db, server)
  .settings(flywaySettings("glance_eval"):_*)


lazy val glanceEval = soarCrossProject("glance-eval", CrossType.Full)
  .enablePlugins(SbtWeb)
  .enablePlugins(WorkbenchPlugin)
  .enablePlugins(DockerPlugin)
  .settings(
    name := "Soar Glance Eval",
    moduleName := "soar-glance-eval",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "shared" / "main" / "scala")
  .settings(sjsCrossVersionPatch:_*)

lazy val glanceEvalJS = glanceEval.js
  .dependsOn(coreJS, glanceCoreJS)
  .settings(commonFrontendDeps:_*)
  .settings(
    libraryDependencies ++= Seq (
      //What is the point of having these dependencies in libraryDependencies?
      "org.webjars" %   "bootstrap"  % bootstrapVersion,
      "org.webjars" %   "datatables" % datatablesVersion
    ),
    jsDependencies ++= Seq(
      "org.webjars.bower" % "react" % reactVersion / "react-with-addons.js" minified "react-with-addons.min.js",
      "org.webjars.bower" % "react" % reactVersion / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js",
      "org.webjars" % "jquery" % "1.11.1" / "jquery.js" minified "jquery.min.js",
      "org.webjars" % "bootstrap" % bootstrapVersion / "bootstrap.js" minified "bootstrap.min.js" dependsOn "jquery.js",
      "org.webjars" % "datatables" % datatablesVersion / "jquery.dataTables.js" minified "jquery.dataTables.min.js" dependsOn "jquery.js",
      "org.webjars" % "datatables" % datatablesVersion / "dataTables.bootstrap.js" minified "dataTables.bootstrap.min.js" dependsOn "jquery.js",
      "org.webjars" % "chartjs" % "2.1.3" / "Chart.js" minified "Chart.min.js",
      ProvidedJS / "react-sortable-hoc.js" minified "react-sortable-hoc.min.js" dependsOn("react-with-addons.js", "react-dom.js")
    ),
    scalaJSUseMainModuleInitializer := true,
    dockerfile in docker := {
      val appStatics = (resources in Compile).value
      val appJs = (fullOptJS in Compile).value.data
      val appTarget = "/nginx/share/nginx/html"
      
      new Dockerfile {
        from("nginx")
        copy(appStatics, appTarget)
        copy(appJs, s"$appTarget/assets")
      }
    },
    imageNames in docker := Seq(
      ImageName(s"${organization.value}/glance-eval-frontend:latest"),
      ImageName(
        namespace = Some(organization.value),
        repoisitory = "glance-eval-frontend",
        tag = Some(s"v${version.value}"))
    ))


lazy val glanceEvalJVM = glanceEval.jvm
  .dependsOn(coreJVM, db, server, glanceCoreJVM)
  .settings(
    (resources in Compile) += (fastOptJS in (glanceEvalJS, Compile)).value.data,
    mainClass in Compile := Some("uk.ac.ncl.la.soar.glance.eval.server.Main"),
    dockerfile in docker := {
      val fatJar: File = assembly.value
      val jarTarget = s"/app/${fatJar.name}"

      new Dockerfile {
        from("java")
        add(fatJar, jarTarget)
        entryPoint("java", "-Xms512M -Xmx2G -jar", jarTarget)
        expose(8080)
      }
    },
    imageNames in docker := Seq(
      ImageName(s"${organization.value}/glance-eval-backend:latest"),
      ImageName(
        namespace = Some(organization.value),
        repoisitory = "glance-eval-backend",
        tag = Some(s"v${version.value}"))
    ))
  .settings(commonAssembly("uk.ac.ncl.la.soar.glance.eval.server.Main", "soar-glance-eval.jar"))

//TODO: Investigate intermittent heap space OOM error on assembly of this module
lazy val glanceEvalCli = soarProject("glance-eval-cli")
  .dependsOn(coreJVM, db, server, glanceCoreJVM, glanceEvalJVM)
  .settings(
    name := "Soar Glance Eval CLI",
    moduleName := "soar-glance-eval-cli",
    libraryDependencies ++= Seq(
      "com.jsuereth" %% "scala-arm" % "2.0",
      "com.github.scopt" %% "scopt" % "3.5.0"),
    commonAssembly("uk.ac.ncl.la.soar.glance.eval.cli.Main", "soar-glance-eval-cli.jar"))

/**
  * Command Aliases to make using this sbt project from the console a little more palatable
  */
addCommandAlias("testAll",
  List(
   "; coreJS/test",
   "; coreJVM/test",
   "; db/test",
   "; db-cli/test",
   "; glance-coreJVM/test",
   "; glance-coreJS/test",
   "; glance-evalJVM/test",
   "; glance-evalJS/test",
   "; glance-eval-cli/test",
   "; model/test",
   "; server/test").mkString)
addCommandAlias("cleanAll",
  List(
   "; coreJS/clean",
   "; coreJVM/clean",
   "; db/clean",
   "; db-cli/clean",
   "; glance-coreJVM/clean",
   "; glance-coreJS/clean",
   "; glance-evalJVM/clean",
   "; glance-evalJS/clean",
   "; glance-eval-cli/clean",
   "; model/clean",
   "; server/clean").mkString)
addCommandAlias("compileAll",
  List(
   "; coreJS/compile",
   "; coreJVM/compile",
   "; db/compile",
   "; db-cli/compile",
   "; glance-coreJVM/compile",
   "; glance-coreJS/compile",
   "; glance-evalJVM/compile",
   "; glance-evalJS/compile",
   "; glance-eval-cli/compile",
   "; model/compile",
   "; server/compile").mkString)
