
object Versions {
    val kotlin = "1.2.31"
    val mockitoCore = "2.17.0"
    val mockitoKotlin = "1.5.0"
    val junit = "4.12"
    val assertj = "3.9.1"
}

object Dependencies {
    val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
}

object TestDependencies {
    val junit = "junit:junit:${Versions.junit}"
    val mockitoCore = "org.mockito:mockito-core:${Versions.mockitoCore}"
    val mockitoKotlin = "com.nhaarman:mockito-kotlin:${Versions.mockitoKotlin}"
    val assertj = "org.assertj:assertj-core:${Versions.assertj}"
}