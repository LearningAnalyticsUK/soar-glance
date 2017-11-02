logLevel := Level.Warn

resolvers += "Flyway" at "https://flywaydb.org/repo"

addSbtPlugin("org.scalastyle"    %% "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("org.scoverage"     %% "sbt-scoverage"         % "1.5.0")
addSbtPlugin("org.scala-js"      %  "sbt-scalajs"           % "0.6.18")
addSbtPlugin("com.lihaoyi"       %  "workbench"             % "0.3.0")
addSbtPlugin("io.spray"          %  "sbt-revolver"          % "0.8.0")
addSbtPlugin("com.typesafe.sbt"  %  "sbt-web"               % "1.4.0")
addSbtPlugin("org.flywaydb"      %  "flyway-sbt"            % "4.2.0")
addSbtPlugin("se.marcuslonnberg" %  "sbt-docker"            % "1.5.0")
