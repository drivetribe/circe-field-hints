lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"
lazy val scalaCheck = Seq(
  "org.scalacheck" %% "scalacheck" % "1.13.4",
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.3",
  "com.fortysevendeg" %% "scalacheck-datetime" % "0.2.0"
)

lazy val circeV = "0.8.0"
lazy val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeV)

lazy val `circe-field-hints` = (project in file("."))
  .settings(
    bintrayOrganization := Some("drivetribe"),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    version in ThisBuild := "0.2",
    wartremoverErrors ++= Seq(Wart.TraversableOps, Wart.Return),
    organization in ThisBuild := "io.chumps",
    scalaVersion in ThisBuild := "2.11.11",
    scalacOptions in ThisBuild ++= Seq(
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
    ).map(x => s"-Xlint:$x"),
    libraryDependencies ++= circe ++ scalaCheck.map(_ % Test) ++ Seq(
      compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
      scalaTest % Test,
      "com.chuusai" %% "shapeless" % "2.3.2"
    )
  )
