# Arcana Quest Tweaks Gradle compiler script
$ErrorActionPreference = "Stop"

$scriptDir = (Split-Path -Parent $MyInvocation.MyCommand.Path).Replace("\", "/")
$workspaceDir = $scriptDir

Write-Output "=== Arcana Quest Tweaks Gradle Build ==="
Write-Output "Workspace: $workspaceDir"
Write-Output "Script Dir: $scriptDir"

# 1. Create libs folder and copy dependency
$libsDir = Join-Path $scriptDir "libs"
if (!(Test-Path $libsDir)) { New-Item -ItemType Directory $libsDir | Out-Null }
$depJar = "$workspaceDir/mods/ElenaiDodge2-1.12.2-1.1.0.jar"
$targetDep = "$libsDir/ElenaiDodge2-1.12.2-1.1.0.jar"
Write-Output "Copying Elenai Dodge 2 dependency..."
Copy-Item -Path $depJar -Destination $targetDep -Force

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
$buildJar = "$scriptDir/build/libs/ArcanaQuestTweaks-1.1.jar"
$modsJar = "$workspaceDir/mods/ArcanaQuestTweaks-1.1.jar"
$localModsJar = "c:/Users/hughe/curseforge/minecraft/Instances/Arcana Quest DEVBOX/mods/ArcanaQuestTweaks-1.1.jar"
if (Test-Path $buildJar) {
    Write-Output "Copying compiled mod to Google Drive mods folder..."
    Copy-Item -Path $buildJar -Destination $modsJar -Force
    Write-Output "Copying compiled mod to local game mods folder..."
    Copy-Item -Path $buildJar -Destination $localModsJar -Force
    Write-Output "=== Build and Deployment Succeeded! ==="
} else {
    Write-Error "Output jar not found! Build might have failed."
}
