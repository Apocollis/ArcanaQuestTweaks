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
# Override with AQ_DEVBOX_MODS / AQ_JAVA_HOME when this machine differs.
$localModsDir = if ($env:AQ_DEVBOX_MODS) { $env:AQ_DEVBOX_MODS } else { "c:/Users/hughe/curseforge/minecraft/Instances/Arcana Quest DEVBOX/mods" }

Write-Output "Checking dependencies in libs..."
# Stale/unlisted jars. build.gradle puts EVERY jar in libs/ on the compile classpath,
# so anything not in the compatibility matrix must not sit here.
$stale = @(
    "ElenaiDodge2-1.12.2-1.1.0.jar",
    "RecurrentComplexVolts-1.12.2-2.0.0.7.jar",
    "BaublesEX-1.12.2-2.3.5.jar",
    "WearableBackpacks-RLCraft-1.12.2-3.2.7.jar",
    "RoguelikeDungeons-Arcana-1.12.2-2.5.0.jar"
)
foreach ($old in $stale) {
    $oldPath = Join-Path $libsDir $old
    if (Test-Path $oldPath) { Remove-Item $oldPath -Force }
}

$deps = @(
    "ElenaiDodge2Extended-1.12.2-1.1.3.jar",
    "bewitchment-1.12.2-0.0.22.65.jar",
    "CraftTweaker2-1.12-4.1.20.715.jar",
    "CoFHWorld-1.12.2-1.4.0.1-universal.jar",
    "bettercaves-1.12.2-2.0.4.jar",
    "RecurrentComplexVolts-1.12.2-2.0.0.9.jar",
    "IvToolkit-1.3.3-1.12.jar",
    "RTG-1.12.2-7.3.3.6.jar",
    "astralsorcery-1.12.2-1.10.27.jar",
    "mysticalworld-1.12.2-1.11.0.jar",
    "GrimoireOfGaia3-1.12.2-1.7.2.jar",
    "Reskillable-1.12.2-1.13.1.jar",
    "effortlessbuilding-1.12.2-2.16.jar",
    "Thaumcraft-1.12.2-6.1.BETA26.jar",
    "thaumictweaker-1.3.4.jar",
    "incontrol-1.12-3.10.4.jar",
    "animania-1.12.2-base-2.0.3.28.jar",
    "animania-1.12.2-farm-1.0.2.28.jar",
    "Somnia-1.0.1.jar",
    "BetterMineshaftsForge-1.12.2-2.2.1.jar",
    "StatsKeeper-1.12.2-3.1.13.jar",
    "randomportals-cleanroom0.1.0.jar",
    "1.12.2-DynamicSwordSkills-6.0.1.jar",
    "1.12.2-SwordSkillsApi-1.1.0.jar",
    "SimpleDifficulty-1.12.2-0.3.9.jar",
    "rustic-1.2.0.jar",
    "twilightforest-1.12.2-3.15.1.jar",
    "aether-1.12.2-v1.5.4.1.jar",
    "Quark-r1.6-179.jar",
    "AutoRegLib-1.3-32.jar",
    "Charm-1.12.2-1.4.1.jar",
    "Chisel-MC1.12.2-1.0.2.45.jar",
    "GameStages-1.12.2-2.0.123.jar",
    "recipestages-2.0.1.jar",
    "QualityTools-1.0.7_for_1.12.2.jar",
    "embers-1.26.1.jar",
    "Botania r1.10-364.4.jar",
    "BaublesEX-1.12.2-2.3.7.jar",
    "PotionCore-1.9_for_1.12.2.jar",
    "lycanitesmobs-1.12.2-2.0.8.10.jar",
    "DynamicStealth-1.12.2.120.jar",
    "PlayerRevive_v1.2.40_mc1.12.2.jar",
    "FarmersDelightLegacy-1.1.7.jar",
    "extra-delight-legacy-1.1.6.jar",
    "prospectus-1.8.jar",
    "simpletomb-1.12.2-1.0.0.jar",
    "grappling_hook_mod-1.12.2-v13.jar",
    "OpenGlider-1.12.1-1.1.0.jar",
    "Waystones_1.12.2-4.1.0.jar",
    "TreeChopper-1.12.2-1.2.4.jar",
    "depthsupdate-1.12.2-1.0.0-a12.jar",
    "SereneSeasons-1.12.2-1.2.18-universal.jar"
)
foreach ($dep in $deps) {
    $src = "$localModsDir/$dep"
    $dest = "$libsDir/$dep"
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dest -Force
    }
}

$incontrolJar = Join-Path $libsDir "incontrol-1.12-3.10.4.jar"
$mcjtyToolsJar = Join-Path $libsDir "mcjtytools-1.12-0.0.21.jar"
if (Test-Path $incontrolJar) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($incontrolJar)
    try {
        $entry = $zip.GetEntry("META-INF/libraries/mcjtytools-1.12-0.0.21.jar")
        if ($entry -ne $null) {
            $out = [System.IO.File]::Create($mcjtyToolsJar)
            try {
                $entry.Open().CopyTo($out)
            } finally {
                $out.Dispose()
            }
        }
    } finally {
        $zip.Dispose()
    }
}

# 2. Configure Java 25 JDK path
$env:JAVA_HOME = if ($env:AQ_JAVA_HOME) { $env:AQ_JAVA_HOME } else { "C:\Program Files\Zulu\zulu-25" }
Write-Output "JAVA_HOME set to: $env:JAVA_HOME"

# 3. Run gradlew build
Write-Output "Running Gradle build task..."
# Execute gradlew.bat in the script directory
$oldPwd = pwd
cd $scriptDir
# Gradle's launcher JVM (JDK 25) prints a "restricted method" warning to stderr. Silence it at the
# source, and judge success by the exit code only: under $ErrorActionPreference = "Stop", callers that
# merge stderr (2>&1) would otherwise see a NativeCommandError although Gradle succeeded.
$priorGradleOpts = $env:GRADLE_OPTS
$env:GRADLE_OPTS = (($priorGradleOpts, "--enable-native-access=ALL-UNNAMED") | Where-Object { $_ }) -join " "
$priorErrorPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    & .\gradlew.bat build
    $gradleExit = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $priorErrorPreference
    $env:GRADLE_OPTS = $priorGradleOpts
    cd $oldPwd
}
if ($gradleExit -ne 0) {
    throw "Gradle build failed with exit code $gradleExit"
}

# 4. Copy remapped output jar to mods folders (workspace + CurseForge instance)
# Prefer the primary artifact; skip sources/dev/javadoc classifiers if present.
$buildLibs = Join-Path $scriptDir "build/libs"
if (!(Test-Path $workspaceDir/mods)) { New-Item -ItemType Directory -Path "$workspaceDir/mods" | Out-Null }
# Pick the jar for the version in build.gradle, not merely the newest file (build/libs also holds old
# versions). Falls back to the newest primary jar if the version cannot be read.
$buildVersion = $null
$versionLine = Select-String -Path (Join-Path $scriptDir "build.gradle") -Pattern "^version\s*=\s*'([^']+)'" | Select-Object -First 1
if ($versionLine) { $buildVersion = $versionLine.Matches[0].Groups[1].Value }
$buildJars = @()
if ($buildVersion) {
    $buildJars = @(Get-ChildItem -Path $buildLibs -Filter "ArcanaQuestTweaks-$buildVersion.jar")
}
if ($buildJars.Count -eq 0) {
    $buildJars = @(Get-ChildItem -Path $buildLibs -Filter "ArcanaQuestTweaks-*.jar" |
        Where-Object { $_.Name -notmatch "(sources|javadoc|dev)" } |
        Sort-Object LastWriteTime -Descending)
}
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
