import sbt._

object Dependencies {
  object Versions {
    val fs2v2 = "2.5.10"
    val fs2v3 = "3.2.3"

    val catsEffectsScalatest2 = "0.5.4"
    val catsEffectsScalatest3 = "1.4.0"
  }

  object Library {
    import Versions._

    val Fs2V2 = Seq(
        "co.fs2"        %% "fs2-core"                      % fs2v2,
        "com.codecommit" %% "cats-effect-testing-scalatest" % catsEffectsScalatest2 % Test,
    )

    val Fs2V3 = Seq(
        "co.fs2"        %% "fs2-core"                      % fs2v3,
        "org.typelevel" %% "cats-effect-testing-scalatest" % catsEffectsScalatest3 % Test,
    )
  }
}
