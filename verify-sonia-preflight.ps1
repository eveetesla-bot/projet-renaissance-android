param(
    [string]$ApkPath = "app\build\outputs\apk\soniaTest\app-soniaTest.apk"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$root = $PSScriptRoot
$apk = if ([System.IO.Path]::IsPathRooted($ApkPath)) { $ApkPath } else { Join-Path $root $ApkPath }
$jar = Join-Path $root "work\toolchain\jdk\jdk-17.0.19+10\bin\jar.exe"

if (-not (Test-Path -LiteralPath $apk)) {
    throw "APK absent. Lancez d'abord .\build-local.ps1."
}
if (-not (Test-Path -LiteralPath $jar)) {
    throw "Outil jar du JDK 17 introuvable."
}

$entries = & $jar tf $apk
if ($LASTEXITCODE -ne 0) {
    throw "Impossible d'inspecter le contenu de l'APK."
}

$forbidden = $entries | Where-Object {
    $_ -match '(^|/)(databases|datastore|shared_prefs|backups?)/' -or
    $_ -match '\.(db|db-wal|db-shm|csv|json)$'
}
if ($forbidden) {
    $forbidden | ForEach-Object { Write-Host "Contenu interdit : $_" -ForegroundColor Red }
    throw "L'APK contient un fichier pouvant embarquer des donnees personnelles."
}

$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
$size = (Get-Item -LiteralPath $apk).Length

Write-Host "Precontrole Sonia reussi." -ForegroundColor Green
Write-Host "Aucune base, preference, sauvegarde, donnee JSON ou CSV n'est embarquee."
Write-Host "APK : $apk"
Write-Host "Taille : $size octets"
Write-Host "SHA-256 : $hash"
Write-Host ""
Write-Host "Aucune installation n'a ete lancee."
