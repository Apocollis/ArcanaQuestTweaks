# Stamina Tweaks compiler script for Windows Powershell
$ErrorActionPreference = "Stop"

$scriptDir = (Split-Path -Parent $MyInvocation.MyCommand.Path).Replace("\", "/")
# Resolve paths to absolute paths
$modsFolder = [System.IO.Path]::GetFullPath((Join-Path $scriptDir "../../mods")).Replace("\", "/")
$libFolder = "C:/Users/hughe/curseforge/minecraft/Install/libraries"
$mcJar = "C:/Users/hughe/curseforge/minecraft/Install/versions/1.12.2/1.12.2.jar"
$forgeJar = "C:/Users/hughe/curseforge/minecraft/Install/versions/forge-14.23.5.2864/forge-14.23.5.2864.jar"
$elenaiJar = (Join-Path $modsFolder "ElenaiDodge2-1.12.2-1.1.0.jar").Replace("\", "/")

Write-Output "=== Stamina Tweaks Build ==="
Write-Output "Script directory: $scriptDir"
Write-Output "Output mods folder: $modsFolder"

# 1. Clean previous build directories
$buildDir = (Join-Path $scriptDir "build").Replace("\", "/")
$classesDir = (Join-Path $buildDir "classes").Replace("\", "/")
if (Test-Path $buildDir) {
    Write-Output "Cleaning old build files..."
    Remove-Item -Path $buildDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

# 2. Gather library classpath (ignoring empty jars and using forward slashes)
Write-Output "Gathering libraries..."
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
