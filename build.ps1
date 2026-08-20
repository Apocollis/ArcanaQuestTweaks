# Legacy javac build script (Antigravity / Stamina Tweaks era).
# Primary Gradle workflow: use build_gradle.ps1 instead.
# This file is PRESERVED for Antigravity compatibility - do not delete.
$ErrorActionPreference = "Stop"

$scriptDir = (Split-Path -Parent $MyInvocation.MyCommand.Path).Replace("\", "/")

# --- Path resolution ---------------------------------------------------------
# Historical Antigravity layout expected mods two levels above the project:
#   <something>/mods  <-  ../../mods from the project root
# That relative path is still tried first so Antigravity keeps working.
# From C:\dev\ArcanaQuestTweaks, ../../mods resolves to C:\mods (usually absent),
# so we fall back to the CurseForge DEVBOX instance mods, then workspace mods/.
$legacyModsFolder = [System.IO.Path]::GetFullPath((Join-Path $scriptDir "../../mods")).Replace("\", "/")
$curseForgeModsFolder = "C:/Users/hughe/curseforge/minecraft/Instances/Arcana Quest DEVBOX/mods"
$workspaceModsFolder = (Join-Path $scriptDir "mods").Replace("\", "/")
$elenaiName = "ElenaiDodge2Extended-1.12.2-1.1.3.jar"

function Test-ModsFolder([string]$folder) {
    return (Test-Path $folder) -and (Test-Path (Join-Path $folder $elenaiName))
}

if (Test-ModsFolder $legacyModsFolder) {
    $modsFolder = $legacyModsFolder
} elseif (Test-ModsFolder $curseForgeModsFolder) {
    $modsFolder = $curseForgeModsFolder
} else {
    $modsFolder = $workspaceModsFolder
    if (!(Test-Path $modsFolder)) {
        New-Item -ItemType Directory -Force -Path $modsFolder | Out-Null
    }
    Write-Warning "ElenaiDodge jar not found under legacy ../../mods or CurseForge DEVBOX mods. Using workspace mods: $modsFolder"
}

# Absolute CurseForge Install paths (required classpath - do not remove)
$libFolder = "C:/Users/hughe/curseforge/minecraft/Install/libraries"
$mcJar = "C:/Users/hughe/curseforge/minecraft/Install/versions/1.12.2/1.12.2.jar"
$forgeJar = "C:/Users/hughe/curseforge/minecraft/Install/versions/forge-14.23.5.2864/forge-14.23.5.2864.jar"
$elenaiJar = (Join-Path $modsFolder $elenaiName).Replace("\", "/")

Write-Output "=== Stamina Tweaks Build (legacy javac) ==="
Write-Output "Script directory: $scriptDir"
Write-Output "Output mods folder: $modsFolder"
Write-Output "Legacy ../../mods candidate: $legacyModsFolder"

# 1. Clean previous LEGACY javac output only (do not wipe Gradle build/)
$buildDir = (Join-Path $scriptDir "build/javac-legacy").Replace("\", "/")
$classesDir = (Join-Path $buildDir "classes").Replace("\", "/")
if (Test-Path $buildDir) {
    Write-Output "Cleaning old legacy javac build files..."
    Remove-Item -Path $buildDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

# 2. Gather library classpath (ignoring empty jars and using forward slashes)
Write-Output "Gathering libraries..."
foreach ($required in @($libFolder, $mcJar, $forgeJar)) {
    if (!(Test-Path $required)) {
        Write-Error "Required path missing (preserved for this script): $required"
    }
}
if (!(Test-Path $elenaiJar)) {
    Write-Error "ElenaiDodge jar missing: $elenaiJar (place it in mods folder used above)"
}

$libJars = Get-ChildItem -Path $libFolder -Filter "*.jar" -Recurse | Where-Object { $_.Length -gt 0 } | ForEach-Object { $_.FullName.Replace("\", "/") }
$classpathList = $libJars + $mcJar + $forgeJar + $elenaiJar
$classpath = $classpathList -join ";"

# 3. Find source files (using forward slashes)
$sources = Get-ChildItem -Path (Join-Path $scriptDir "src/main/java") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName.Replace("\", "/") }
Write-Output "Found $($sources.Count) source files to compile."

# 4. Write options file for javac (quoting paths to handle spaces)
$optionsFile = Join-Path $buildDir "javac_options.txt"
$optionsContent = @(
    "--release", "8",
    "-cp", "`"$classpath`"",
    "-d", "`"$classesDir`""
)
foreach ($src in $sources) {
    $optionsContent += "`"$src`""
}

[System.IO.File]::WriteAllLines($optionsFile, $optionsContent)

# 5. Compile Java files targeting Java 8
Write-Output "Compiling Java files..."
& javac "@$optionsFile"

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed!"
}

# 6. Copy resources
$resourcesDir = Join-Path $scriptDir "src/main/resources"
if (Test-Path $resourcesDir) {
    Write-Output "Copying resources..."
    Copy-Item -Path (Join-Path $resourcesDir "*") -Destination $classesDir -Recurse -Force
}

# 7. Package JAR
$targetJar = Join-Path $modsFolder "StaminaTweaks-1.0.jar"
Write-Output "Packaging JAR to $targetJar..."
& jar cf $targetJar -C $classesDir .

if ($LASTEXITCODE -ne 0) {
    Write-Error "JAR packaging failed!"
}

Write-Output "=== Build Succeeded ==="