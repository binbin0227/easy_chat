param([string[]]$Tasks = @('build','remapJar'), [string]$JavaHome)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $minecraftJava = Join-Path $env:APPDATA '.minecraft/runtime/java-runtime-delta'
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    elseif (Test-Path (Join-Path $minecraftJava 'bin/javac.exe')) { $env:JAVA_HOME = $minecraftJava }
    if (!$env:JAVA_HOME -or !(Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        throw '构建需要 JDK 21。请安装 JDK 21 并设置 JAVA_HOME；玩家使用模组不需要这一步。'
    }
    $env:GRADLE_USER_HOME = Join-Path $projectRoot 'work/gradle-home'
    New-Item -ItemType Directory -Force 'work/tmp' | Out-Null
    # Short relative AF_UNIX path avoids Windows/Unicode temp-path length issues.
    $env:JAVA_TOOL_OPTIONS = (($env:JAVA_TOOL_OPTIONS + ' -Djdk.net.unixdomain.tmpdir=work/tmp').Trim())
    if ($env:HTTPS_PROXY) {
        $proxyUri = [Uri]$env:HTTPS_PROXY
        $env:GRADLE_OPTS = (($env:GRADLE_OPTS + " -Dhttps.proxyHost=$($proxyUri.Host) -Dhttps.proxyPort=$($proxyUri.Port) -Dhttp.proxyHost=$($proxyUri.Host) -Dhttp.proxyPort=$($proxyUri.Port)").Trim())
    }
    $env:GRADLE_OPTS = (($env:GRADLE_OPTS + ' -Dorg.gradle.internal.http.connectionTimeout=20000 -Dorg.gradle.internal.http.socketTimeout=30000').Trim())
    $portableGradle = Join-Path $projectRoot 'work/tools/gradle-8.14/bin/gradle.bat'
    $gradle = if (Test-Path $portableGradle) { $portableGradle } else { Join-Path $projectRoot 'gradlew.bat' }
    $jdk8 = Get-ChildItem 'work/tools' -Directory -Filter 'jdk8*' -ErrorAction SilentlyContinue | Select-Object -First 1
    $toolchainArgs = @("-Porg.gradle.java.installations.paths=$env:JAVA_HOME")
    if ($jdk8) { $toolchainArgs = @("-Porg.gradle.java.installations.paths=$env:JAVA_HOME,$($jdk8.FullName)") }
    & $gradle @Tasks @toolchainArgs --console=plain
    if ($LASTEXITCODE -ne 0) { throw "构建失败（退出码 $LASTEXITCODE）" }
} finally { Pop-Location }
