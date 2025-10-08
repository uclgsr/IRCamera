# --- Kotlin/Java Text Merger (Produces NON-COMPILING .kt file) ---
$OutputFileSuffix = "_merged"
$ExcludeDirs = @('build', 'out', 'bin', 'target', '.git', '.idea', '.gradle', 'gradle')
$CleanPreviousOutput = $true
$ErrorActionPreference = 'Stop'

# --- Setup ---
$rootPath = (Get-Location).Path
if ($CleanPreviousOutput) {
    Write-Host "Cleaning up previously merged files..." -ForegroundColor Yellow
    $oldFiles = Get-ChildItem -Path $rootPath -Recurse -File -Include "*$($OutputFileSuffix).kt"
    if ($oldFiles) { $oldFiles | Remove-Item -Verbose } else { Write-Host "No old merged files found to clean." }
}

# --- Main Logic ---
Write-Host "Starting in-place text merge..."
$excludedRegex = '(\\|/)(?:' + ($ExcludeDirs -join '|') + ')(?=\\|/|$)'
$dirsToProcess = @(Get-Item -Path $rootPath) + (Get-ChildItem -Path $rootPath -Directory -Recurse | Where-Object { $_.FullName -notmatch $excludedRegex })

foreach ($dir in $dirsToProcess) {
    # Find both .kt and .java files
    $filesToMerge = Get-ChildItem -Path $dir.FullName -Recurse -File -Include *.kt, *.java -Exclude "*$($OutputFileSuffix).kt" | Where-Object { $_.FullName -notmatch $excludedRegex }
    if (-not $filesToMerge) { continue }

    $fileList = $filesToMerge | Sort-Object FullName
    $contentBuilder = [System.Text.StringBuilder]::new()
    $relativePath = $dir.FullName.Substring($rootPath.Length).TrimStart('\')

    [void]$contentBuilder.AppendLine("// Merged ALL .kt and .java files from the '$($relativePath -replace '^$', 'root')' directory and its subdirectories.")
    [void]$contentBuilder.AppendLine("// Total files: $($fileList.Count) | Generated on: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    [void]$contentBuilder.AppendLine()

    foreach ($file in $fileList) {
        $fileRelativePath = $file.FullName.Substring($rootPath.Length).TrimStart('\')
        [void]$contentBuilder.AppendLine("`n// ===== FROM: $fileRelativePath =====`n")
        $fileContent = Get-Content -Raw -Path $file.FullName
        $fileContent = $fileContent.TrimStart([char]0xFEFF)
        [void]$contentBuilder.AppendLine($fileContent.TrimEnd("`r", "`n"))
        [void]$contentBuilder.AppendLine()
    }

    # Force the output file to always have a .kt extension
    $outputFileName = if ($relativePath) { ($relativePath -replace '[\\/]+', '_') + $OutputFileSuffix + ".kt" } else { "root" + $OutputFileSuffix + ".kt" }
    $outputFilePath = Join-Path $dir.FullName $outputFileName

    [System.IO.File]::WriteAllText($outputFilePath, $contentBuilder.ToString().TrimEnd(), (New-Object System.Text.UTF8Encoding $false))
    Write-Host "Created merged (but invalid) file: $outputFilePath" -ForegroundColor Green
}

Write-Host "✅ In-place text merge complete."