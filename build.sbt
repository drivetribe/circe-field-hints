licenses += "MIT" -> url("http://opensource.org/licenses/MIT")
version := "0.3"
wartremoverErrors ++= Seq(Wart.TraversableOps, Wart.Return)
organization := "com.drivetribe"
homepage := Option(url("https://github.com/drivetribe/orchestra"))
scmInfo := Option(
  ScmInfo(url("https://github.com/drivetribe/orchestra"), "https://github.com/drivetribe/orchestra.git")
)
developers := List(
  Developer(id = "Astrac",
            name = "Aldo Stracquadanio",
            email = "aldo.stracquadanio@drivetribe.com",
            url = url("https://github.com/Astrac")),
  Developer(id = "joan38", name = "Joan Goyeau", email = "joan@goyeau.com", url = url("http://goyeau.com"))
)
publishTo := Option(
  if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
  else Opts.resolver.sonatypeStaging
)
scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.12", "2.12.4")
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding",
  "utf8",
  "-target:jvm-1.8",
  "-Xlog-reflective-calls",
  "-feature",
  "-Ywarn-dead-code",
  "-Ypartial-unification",
  "-Ywarn-unused-import"
) ++ Seq(
  "by-name-right-associative",
  "delayedinit-select",
  "doc-detached",
  "inaccessible",
  "missing-interpolator",
  "nullary-override",
  "option-implicit",
  "package-object-classes",
  "poly-implicit-overload",
  "private-shadow",
  "unsound-match"
).map(x => s"-Xlint:$x")
libraryDependencies ++= circe ++ scalaCheck.map(_ % Test) ++ Seq(
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
  scalaTest % Test,
  "com.chuusai" %% "shapeless" % "2.3.3"
)

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"
lazy val scalaCheck = Seq(
  "org.scalacheck" %% "scalacheck" % "1.13.4",
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6",
  "com.fortysevendeg" %% "scalacheck-datetime" % "0.2.0"
)

lazy val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % "0.9.1")
