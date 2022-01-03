import sbt._

object Dependencies {
  object Versions {
    val fs2Rabbit3 = "3.0.1"
    val fs2Rabbit4 = "4.1.0"

    val catsEffectsScalatest2 = "0.5.4"
    val catsEffectsScalatest3 = "1.4.0"
  }

  object Library {
    import Versions._

    val Fs2V2 = Seq(
        "dev.profunktor" %% "fs2-rabbit"                    % fs2Rabbit3,
        "dev.profunktor" %% "fs2-rabbit-circe"              % fs2Rabbit3,
        "com.codecommit" %% "cats-effect-testing-scalatest" % catsEffectsScalatest2 % Test,
    )

    val Fs2V3 = Seq(
        "dev.profunktor" %% "fs2-rabbit"                    % fs2Rabbit4,
        "dev.profunktor" %% "fs2-rabbit-circe"              % fs2Rabbit4,
        "org.typelevel"  %% "cats-effect-testing-scalatest" % catsEffectsScalatest3 % Test,
    )
  }
}
