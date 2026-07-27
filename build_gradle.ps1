# Arcana Quest Tweaks Gradle compiler script
$ErrorActionPreference = "Stop"

$scriptDir = (Split-Path -Parent $MyInvocation.MyCommand.Path).Replace("\", "/")
$workspaceDir = $scriptDir

Write-Output "=== Arcana Quest Tweaks Gradle Build ==="
Write-Output "Workspace: $workspaceDir"
Write-Output "Script Dir: $scriptDir"

# 1. Create libs folder and copy dependencies
$libsDir = Join-Path $scriptDir "libs"
if (!(Test-Path $libsDir)) { New-Item -ItemType Directory $libsDir | Out-Null }
$localModsDir = "c:/Users/hughe/curseforge/minecraft/Instances/Arcana Quest DEVBOX/mods"

Write-Output "Checking dependencies in libs..."
$deps = @(
    "ElenaiDodge2-1.12.2-1.1.0.jar",
    "bewitchment-1.12.2-0.0.22.65.jar",
    "RoguelikeDungeons-Arcana-1.12.2-2.5.0.jar",
    "CoFHWorld-1.12.2-1.4.0.1-universal.jar",
    "bettercaves-1.12.2-2.0.4.jar",
    "RecurrentComplexVolts-1.12.2-2.0.0.7.jar",
    "IvToolkit-1.3.3-1.12.jar"
)
foreach ($dep in $deps) {
    $src = "$localModsDir/$dep"
    $dest = "$libsDir/$dep"
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dest -Force
    }
}

# 2. Configure Java 8 JDK path
$env:JAVA_HOME = "$workspaceDir/temp/jdk8_extracted/jdk8u412-b08"
Write-Output "JAVA_HOME set to: $env:JAVA_HOME"

# 3. Run gradlew build
Write-Output "Running Gradle build task..."
# Execute gradlew.bat in the script directory
$oldPwd = pwd
cd $scriptDir
try {
    & .\gradlew.bat build
} finally {
    cd $oldPwd
}

# 4. Copy output jar to mods folders (both Google Drive and local game instance)
$buildJar = "$scriptDir/build/libs/ArcanaQuestTweaks-1.2.jar"
$modsJar = "$workspaceDir/mods/ArcanaQuestTweaks-1.2.jar"
$localModsJar = "c:/Users/hughe/curseforge/minecraft/Instances/Arcana Quest DEVBOX/mods/ArcanaQuestTweaks-1.2.jar"
if (Test-Path $buildJar) {
    Write-Output "Copying compiled mod to Google Drive mods folder..."
    Copy-Item -Path $buildJar -Destination $modsJar -Force
    Write-Output "Copying compiled mod to local game mods folder..."
    Copy-Item -Path $buildJar -Destination $localModsJar -Force
    Write-Output "=== Build and Deployment Succeeded! ==="
} else {
    Write-Error "Output jar not found! Build might have failed."
}
