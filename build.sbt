name := "force"

version := "1.0"

lazy val scalaVersion2_12 = "2.12.17"

Compile / doc / scalacOptions ++= Seq("-Vimplicits", "-deprecation", "-Ywarn-dead-code", "-Ywarn-value-discard", "-Ywarn-unused")

lazy val akkaVersion = "2.6.21"
lazy val scalatestVersion = "3.1.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"          % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster"        % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed"    % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed"  % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit"        % akkaVersion % Test,
  "org.scalatest"      %% "scalatest"           % scalatestVersion % Test,
  "com.typesafe"       % "config"               % "1.4.2",
  "io.circe"          %% "circe-core"           % "0.14.3",
  "io.circe"          %% "circe-generic"        % "0.14.3",
  "io.circe"          %% "circe-parser"         % "0.14.3"
)
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:+CMSClassUnloadingEnabled")