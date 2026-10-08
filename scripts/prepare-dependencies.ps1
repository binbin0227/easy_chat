param([switch]$CheckOnly, [switch]$SkipModels, [ValidateSet('Official','Mirror')][string]$Source = 'Official')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$manifest = Get-Content (Join-Path $PSScriptRoot 'dependencies.json') -Raw | ConvertFrom-Json
foreach ($item in $manifest) {
    if ($SkipModels -and !$item.path.StartsWith('work/libs/')) { continue }
    $destination = [IO.Path]::GetFullPath((Join-Path $projectRoot $item.path))
    $allowed = [IO.Path]::GetFullPath((Join-Path $projectRoot 'work')) + [IO.Path]::DirectorySeparatorChar
    if (!$destination.StartsWith($allowed, [StringComparison]::OrdinalIgnoreCase)) { throw '依赖目标路径越界' }
    if ((Test-Path -LiteralPath $destination) -and (Get-FileHash -LiteralPath $destination).Hash.ToLowerInvariant() -eq $item.sha256) {
        Write-Host "已校验：$($item.path)"; continue
    }
    if ($CheckOnly) { throw "缺少依赖或校验失败：$($item.path)" }
    New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
    $url = $item.url
    if ($Source -eq 'Mirror') { $url = $url.Replace('https://huggingface.co/', 'https://hf-mirror.com/') }
    $temporary = $destination + '.part'
    try {
        & curl.exe --fail --location --retry 2 --connect-timeout 20 --max-time 1800 $url -o $temporary
        if ($LASTEXITCODE -ne 0) { throw "下载失败：$($item.path)" }
        if ((Get-FileHash -LiteralPath $temporary).Hash.ToLowerInvariant() -ne $item.sha256) { throw "校验失败：$($item.path)" }
        Move-Item -LiteralPath $temporary -Destination $destination -Force
    } finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force } }
}
Write-Host '依赖准备完成。未下载或安装 Java。'
