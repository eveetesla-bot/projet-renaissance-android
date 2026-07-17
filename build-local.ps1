$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$projectRoot = $PSScriptRoot
$bundledJdk = Join-Path $projectRoot "work\toolchain\jdk\jdk-17.0.19+10"
$bundledSdk = Join-Path $projectRoot "work\toolchain\sdk"

$jdkCandidates = @($bundledJdk)
if ($env:JAVA_HOME) {
    $jdkCandidates += $env:JAVA_HOME
}
$javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
if ($javaCommand) {
    $jdkCandidates += Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
}
$jdk = $jdkCandidates |
    Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ "bin\java.exe")) } |
    Select-Object -First 1

$sdkCandidates = @($bundledSdk)
if ($env:ANDROID_SDK_ROOT) {
    $sdkCandidates += $env:ANDROID_SDK_ROOT
}
if ($env:ANDROID_HOME) {
    $sdkCandidates += $env:ANDROID_HOME
}
if ($env:LOCALAPPDATA) {
    $sdkCandidates += Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
$sdk = $sdkCandidates |
    Where-Object { $_ -and (Test-Path -LiteralPath $_) } |
    Select-Object -First 1

if (-not $jdk) {
    throw "JDK 17 introuvable. Installez JDK 17 dans Android Studio, ou définissez JAVA_HOME avant de relancer."
}
if (-not $sdk) {
    throw "Android SDK introuvable. Installez-le depuis Android Studio, ou définissez ANDROID_SDK_ROOT avant de relancer."
}

$sdkManager = Join-Path $sdk "cmdline-tools\latest\bin\sdkmanager.bat"
$platform36 = Join-Path $sdk "platforms\android-36\android.jar"
$buildTools36 = Join-Path $sdk "build-tools\36.0.0\aapt2.exe"
$wrapper = Join-Path $projectRoot "gradlew.bat"

$jdkReleaseFile = Join-Path $jdk "release"
$javaVersion = if (Test-Path -LiteralPath $jdkReleaseFile) { Get-Content -Raw -LiteralPath $jdkReleaseFile } else { "" }
if ($javaVersion -notmatch 'JAVA_VERSION="17\.') {
    throw "JDK 17 requis. Le JDK détecté dans $jdk n'annonce pas une version 17."
}

$missingPlatform = -not (Test-Path -LiteralPath $platform36)
$missingBuildTools = -not (Test-Path -LiteralPath $buildTools36)
if ($missingPlatform -or $missingBuildTools) {
    Write-Host "La chaîne Android 36 locale est incomplète." -ForegroundColor Yellow
    Write-Host "Platform Android 36 : $(if ($missingPlatform) { 'ABSENTE' } else { 'présente' })"
    Write-Host "Build Tools 36.0.0 : $(if ($missingBuildTools) { 'ABSENTS' } else { 'présents' })"
    if (Test-Path -LiteralPath $sdkManager) {
        Write-Host "Installez les éléments manquants avec cette commande PowerShell :" -ForegroundColor Yellow
        Write-Host "& '$sdkManager' 'platforms;android-36' 'build-tools;36.0.0'" -ForegroundColor Cyan
    } else {
        Write-Host "Installez Android SDK Platform 36 et Build Tools 36.0.0 depuis Android Studio > SDK Manager." -ForegroundColor Yellow
    }
    throw "Compilation arrêtée : Platform 36 et Build Tools 36.0.0 doivent être installés."
}

if (-not (Test-Path -LiteralPath $wrapper)) {
    throw "Gradle Wrapper introuvable : $wrapper"
}

$env:JAVA_HOME = $jdk
$env:ANDROID_SDK_ROOT = $sdk
$env:GRADLE_USER_HOME = Join-Path $projectRoot "work\gradle-user-home"

Write-Host "Projet Renaissance - vérification de la chaîne Android" -ForegroundColor Cyan
Write-Host "JDK 17 : $jdk"
Write-Host "Android SDK 36 : $sdk"
Write-Host "Android Build Tools : 36.0.0"
Write-Host "Android Gradle Plugin : 8.11.1"
Write-Host "Gradle Wrapper : 8.13"
Write-Host "Les dépendances seront conservées dans work\gradle-user-home."

& $wrapper --version
if ($LASTEXITCODE -ne 0) {
    throw "Impossible d'initialiser Gradle Wrapper 8.13."
}

& $wrapper testDebugUnitTest assembleSoniaTestAndroidTest assembleDebug assembleSoniaTest --no-daemon --max-workers=1 --console=plain
if ($LASTEXITCODE -ne 0) {
    throw "La compilation Gradle a échoué avec le code $LASTEXITCODE."
}

$apk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path -LiteralPath $apk)) {
    throw "Gradle a terminé sans produire l'APK attendu."
}

$soniaApk = Join-Path $projectRoot "app\build\outputs\apk\soniaTest\app-soniaTest.apk"
if (-not (Test-Path -LiteralPath $soniaApk)) {
    throw "Gradle a terminé sans produire l'APK Sonia Test attendu."
}

Write-Host "Tests et compilation terminés avec succès." -ForegroundColor Green
Write-Host "APK : $apk"
Write-Host "APK Sonia Test : $soniaApk"


