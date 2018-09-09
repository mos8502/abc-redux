
object Versions {
    val kotlin = "1.2.51"
    val mockitoCore = "2.17.0"
    val mockitoKotlin = "1.5.0"
    val junit = "4.12"
    val assertj = "3.9.1"
    val liveData = "1.1.1"
    val espressoCore = "3.0.2"
    val androidTestRunner = "1.0.2"
    val androidGradlePlugin = "3.2.0-rc02"
    val androidMavenPublish = "3.6.2"
    @JvmStatic val androidCompileSdk = 28
    @JvmStatic val androidMinSdk = 14
    @JvmStatic val androidTargetSdk = 28
}

object Dependencies {
    val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    val liveData = "android.arch.lifecycle:livedata:${Versions.liveData}"
}

object TestDependencies {
    val junit = "junit:junit:${Versions.junit}"
    val mockitoCore = "org.mockito:mockito-core:${Versions.mockitoCore}"
    val mockitoKotlin = "com.nhaarman:mockito-kotlin:${Versions.mockitoKotlin}"
    val assertj = "org.assertj:assertj-core:${Versions.assertj}"
    val espressoCore = "com.android.support.test.espresso:espresso-core:${Versions.espressoCore}"
    val androidTestRunner = "com.android.support.test:runner:${Versions.androidTestRunner}"
}