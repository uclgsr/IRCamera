# --- Kotlin/Java per-level merger (mirrored structure) ---
$MirrorName = "_ktjava_mirror"
$ExcludeDirs = @('build', 'out', 'bin', 'target', '.git', '.idea', '.gradle', 'gradle')
$CleanOutput = $true
$ErrorActionPreference = 'Stop'

$root = (Get-Location).Path
$mirrorRoot = Join-Path $root $MirrorName
if ($CleanOutput -and (Test-Path $mirrorRoot))
{
    Remove-Item -Recurse -Force $mirrorRoot
}

# Exclude mirror and unwanted dirs
$excludeAll = $ExcludeDirs + $MirrorName
$escaped = $excludeAll | ForEach-Object { [regex]::Escape($_) }
$excludedRegex = '(\\|/)(?:' + ($escaped -join '|') + ')(\\|/)'

# Collect all dirs (including root)
$dirs = @([pscustomobject]@{ FullName = $root }) + (
Get-ChildItem -Directory -Recurse |
        Where-Object { $_.FullName -notmatch $excludedRegex }
)

# Preload file lists
$allKt = Get-ChildItem -Recurse -File -Filter *.kt   | Where-Object { $_.FullName -notmatch $excludedRegex }
$allJava = Get-ChildItem -Recurse -File -Filter *.java | Where-Object { $_.FullName -notmatch $excludedRegex }

foreach ($dir in $dirs)
{
    $dirPath = $dir.FullName
    $rel = $dirPath.Substring($root.Length).TrimStart('\', '/')
    $destDir = if ($rel)
    {
        Join-Path $mirrorRoot $rel
    }
    else
    {
        $mirrorRoot
    }
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null

    # Safe filename component
    $relName = if ($rel)
    {
        $rel -replace '[\\/]+', '_'
    }
    else
    {
        'root'
    }

    $ktFiles = $allKt | Where-Object { $_.FullName.StartsWith($dirPath, [StringComparison]::OrdinalIgnoreCase) } | Sort-Object FullName
    $jvFiles = $allJava | Where-Object { $_.FullName.StartsWith($dirPath, [StringComparison]::OrdinalIgnoreCase) } | Sort-Object FullName

    if ($ktFiles.Count -gt 0)
    {
        $outKt = Join-Path $destDir ("${relName}_all.kt")
        "// Merged .kt under '$( $rel -replace '^$', '.' )' subtree" | Set-Content -Path $outKt -Encoding UTF8
        "// Files: $( $ktFiles.Count ); Generated $( Get-Date -Format 'yyyy-MM-dd HH:mm:ss' )" | Add-Content -Path $outKt -Encoding UTF8
        Add-Content -Path $outKt -Value "" -Encoding UTF8
        foreach ($f in $ktFiles)
        {
            $rf = $f.FullName.Substring($root.Length).TrimStart('\', '/')
            Add-Content -Path $outKt -Value ("`n// ===== " + $rf + " =====`n") -Encoding UTF8
            $c = Get-Content -Raw -Path $f.FullName
            $c = $c.TrimStart([char]0xFEFF); $c = $c.TrimEnd("`r", "`n")
            Add-Content -Path $outKt -Value $c -Encoding UTF8
            Add-Content -Path $outKt -Value "`n" -Encoding UTF8
        }
    }
}

Write-Host "Mirror created -> $mirrorRoot"
