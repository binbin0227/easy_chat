param([string]$VoiceModel = 'work/models/paraformer-zh-int8/model.int8.onnx')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $jar = Join-Path $projectRoot 'build/libs/easy-chat-forge-1.8.9-0.1.0.jar'
    if (!(Test-Path -LiteralPath $jar)) { throw '请先构建发行模组。' }
    $latestSource = Get-ChildItem (Join-Path $projectRoot 'src/main') -Recurse -File | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
    if ((Get-Item -LiteralPath $jar).LastWriteTimeUtc -lt $latestSource.LastWriteTimeUtc) { throw '发行 JAR 早于源码，请先执行 remapJar，不能打包旧发行文件。' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $sourceArchive = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $catalogEntry = $sourceArchive.GetEntry('easychat/text-model-catalog.json')
        if (!$catalogEntry) { throw '发行 JAR 缺少模型目录。' }
        $catalogReader = [IO.StreamReader]::new($catalogEntry.Open())
        try { $packedCatalog = $catalogReader.ReadToEnd() } finally { $catalogReader.Dispose() }
        $expectedCatalog = [IO.File]::ReadAllText((Join-Path $projectRoot 'src/main/resources/easychat/text-model-catalog.json'))
        if ($packedCatalog -ne $expectedCatalog) { throw '发行 JAR 模型目录与源码不同，请重新构建。' }
    } finally { $sourceArchive.Dispose() }
    if (!(Test-Path -LiteralPath $VoiceModel)) { throw '单文件发行需要已校验的 Paraformer model.int8.onnx；通过 -VoiceModel 指定已有权重。脚本不会下载模型。' }
    if ((Get-FileHash -LiteralPath $VoiceModel -Algorithm SHA256).Hash.ToLowerInvariant() -ne '90bc03034ae1bef9575f8cc798cd1519c8be8aa9e8b458a033e32017ff4d584c') {
        throw 'Paraformer 模型校验失败，不生成发行包。'
    }
    New-Item -ItemType Directory -Force 'outputs', 'work/package' | Out-Null
    $temporary = Join-Path $projectRoot 'work/package/easy-chat-bundled.jar'
    Copy-Item -LiteralPath $jar -Destination $temporary -Force
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::Open($temporary, [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $entryName = 'assets/easychat/models/paraformer-zh-int8/model.int8.onnx'
        $oldEntry = $archive.GetEntry($entryName)
        if ($oldEntry) { $oldEntry.Delete() }
        $entry = $archive.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::Optimal)
        $inputStream = [System.IO.File]::OpenRead((Resolve-Path -LiteralPath $VoiceModel).Path)
        try {
            $outputStream = $entry.Open()
            try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose() }
        } finally { $inputStream.Dispose() }
        $zhHashes = ConvertFrom-StringData (Get-Content "$PSScriptRoot/text-model-sha256.properties" -Raw)
        foreach ($name in @('encoder_model_quantized.onnx','decoder_model_quantized.onnx','tokenizer.json')) {
            $sourceFile = Join-Path $projectRoot ('work/text-translation/' + $name)
            if ((Get-FileHash -LiteralPath $sourceFile).Hash.ToLowerInvariant() -ne $zhHashes[$name]) { throw "英中模型校验失败：$name" }
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $sourceFile, 'easychat/text-model/' + $name, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Join-Path $PSScriptRoot 'text-model-sha256.properties'), 'easychat/text-model.properties') | Out-Null
        $enHashes = ConvertFrom-StringData (Get-Content "$PSScriptRoot/text-model-zh-en-sha256.properties" -Raw)
        foreach ($name in @('encoder_model_quantized.onnx','decoder_model_quantized.onnx','tokenizer.json')) {
            $sourceFile = Join-Path $projectRoot ('work/text-translation-zh-en/' + $name)
            if ((Get-FileHash -LiteralPath $sourceFile).Hash.ToLowerInvariant() -ne $enHashes[$name]) { throw "中英模型校验失败：$name" }
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $sourceFile, 'easychat/text-model-zh-en/' + $name, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }

    } finally { $archive.Dispose() }
    Move-Item -LiteralPath $temporary -Destination 'outputs/easy-chat-forge-1.8.9-0.1.0.jar' -Force
    $legacyZip = Join-Path $projectRoot 'outputs/easy-chat-forge-1.8.9-0.1.0-offline.zip'
    if (Test-Path -LiteralPath $legacyZip) {
        try { Remove-Item -LiteralPath $legacyZip -Force }
        catch { Write-Warning '旧 ZIP 正被占用，暂时保留；请使用新的单文件 JAR。' }
    }
    Copy-Item -LiteralPath 'docs/INSTALL.md' -Destination 'outputs/安装说明.md'
    Copy-Item -LiteralPath 'docs/VERIFICATION.md' -Destination 'outputs/验证记录.md'
    Copy-Item -LiteralPath 'docs/LICENSE_REVIEW.md' -Destination 'outputs/开源许可检查.md'
    # Previous screenshot fixtures showed removed engines; do not copy them into this package.
    Get-FileHash -LiteralPath 'outputs/easy-chat-forge-1.8.9-0.1.0.jar' -Algorithm SHA256 |
        ForEach-Object { $_.Hash.ToLowerInvariant() + '  ' + (Split-Path $_.Path -Leaf) } |
        Set-Content -LiteralPath 'outputs/SHA256SUMS.txt' -Encoding ascii
    Get-ChildItem 'outputs' -File | Select-Object Name, Length
} finally { Pop-Location }
