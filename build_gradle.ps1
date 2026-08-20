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
$oldElenai = Join-Path $libsDir "ElenaiDodge2-1.12.2-1.1.0.jar"
if (Test-Path $oldElenai) { Remove-Item $oldElenai -Force }

$deps = @(
    "ElenaiDodge2Extended-1.12.2-1.1.3.jar",
    "bewitchment-1.12.2-0.0.22.65.jar",
    "RoguelikeDungeons-Arcana-1.12.2-2.5.0.jar",
    "CoFHWorld-1.12.2-1.4.0.1-universal.jar",
    "bettercaves-1.12.2-2.0.4.jar",
    "RecurrentComplexVolts-1.12.2-2.0.0.7.jar",
    "IvToolkit-1.3.3-1.12.jar",
    "RTG-1.12.2-7.3.3.6.jar"
)
foreach ($dep in $deps) {
    $src = "$localModsDir/$dep"
    $dest = "$libsDir/$dep"
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dest -Force
    }
}

# 2. Configure Java 25 JDK path
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-25"
Write-Output "JAVA_HOME set to: $env:JAVA_HOME"

# 3. Run gradlew build
Write-Output "Running Gradle build task..."
# Execute gradlew.bat in the script directory
$oldPwd = pwd
cd $scriptDir
try {
    & .\gradlew.bat build
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }
} finally {
    cd $oldPwd
}

# 4. Copy remapped output jar to mods folders (workspace + CurseForge instance)
# Prefer the primary artifact; skip sources/dev/javadoc classifiers if present.
$buildLibs = Join-Path $scriptDir "build/libs"
if (!(Test-Path $workspaceDir/mods)) { New-Item -ItemType Directory -Path "$workspaceDir/mods" | Out-Null }
$buildJars = Get-ChildItem -Path $buildLibs -Filter "ArcanaQuestTweaks-*.jar" |
    Where-Object { $_.Name -notmatch "(sources|javadoc|dev)" } |
    Sort-Object LastWriteTime -Descending
if ($buildJars.Count -gt 0) {
    $latestJar = $buildJars[0]
    Write-Output "Latest compiled jar: $($latestJar.FullName)"
    
    # Remove stale old versions in mods directories to prevent game from loading outdated jars
    Get-ChildItem -Path "$workspaceDir/mods" -Filter "ArcanaQuestTweaks-*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force
    Get-ChildItem -Path $localModsDir -Filter "ArcanaQuestTweaks-*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force

    Write-Output "Copying remapped mod jar to workspace mods folder..."
    Copy-Item -Path $latestJar.FullName -Destination "$workspaceDir/mods/$($latestJar.Name)" -Force
    Write-Output "Copying remapped mod jar to local game mods folder..."
    Copy-Item -Path $latestJar.FullName -Destination "$localModsDir/$($latestJar.Name)" -Force
    Write-Output "=== Build and Deployment Succeeded! ==="
} else {
    Write-Error "Output jar not found! Build might have failed. Checked: $buildLibs"
}
