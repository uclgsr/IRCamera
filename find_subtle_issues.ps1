$kotlinFiles = Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt"

Write-Host "Scanning $( $kotlinFiles.Count ) Kotlin files for subtle syntax issues..."

foreach ($file in $kotlinFiles)
{
    $content = Get-Content -Path $file.FullName -Raw -ErrorAction SilentlyContinue
    if ($content)
    {
        $lines = $content -split "`r?`n"

        # Check file ending
        $trimmedContent = $content.TrimEnd()
        $lastNonEmptyLine = ""
        for ($i = $lines.Length - 1; $i -ge 0; $i--) {
            if ($lines[$i].Trim() -ne "")
            {
                $lastNonEmptyLine = $lines[$i].Trim()
                break
            }
        }

        # Check for files ending with suspicious patterns
        if ($lastNonEmptyLine -match "(private|public|protected|internal|class|interface|fun|val|var|@|:|\{|\,)$")
        {
            Write-Host "SUSPICIOUS ENDING: '$lastNonEmptyLine' in $( $file.Name )" -ForegroundColor Yellow
            Write-Host "File: $( $file.FullName )" -ForegroundColor Gray
        }

        # Check for annotation without declaration
        for ($i = 0; $i -lt $lines.Length - 1; $i++) {
            $currentLine = $lines[$i].Trim()
            $nextLine = $lines[$i + 1].Trim()

            if ($currentLine -match "^@\w+" -and $nextLine -eq "")
            {
                Write-Host "ANNOTATION WITHOUT DECLARATION: '$currentLine' at line $( $i + 1 ) in $( $file.Name )" -ForegroundColor Red
                Write-Host "File: $( $file.FullName )" -ForegroundColor Yellow
            }
        }

        # Check for lines with just annotations or modifiers followed by empty lines at file end
        for ($i = $lines.Length - 5; $i -lt $lines.Length; $i++) {
            if ($i -ge 0 -and $i -lt $lines.Length)
            {
                $line = $lines[$i].Trim()
                if ($line -match "^(@\w+|private|public|protected|internal)$")
                {
                    $hasDeclarationAfter = $false
                    for ($j = $i + 1; $j -lt $lines.Length; $j++) {
                        if ($lines[$j].Trim() -ne "" -and $lines[$j].Trim() -notmatch "^(@\w+|private|public|protected|internal)$")
                        {
                            $hasDeclarationAfter = $true
                            break
                        }
                    }
                    if (-not $hasDeclarationAfter)
                    {
                        Write-Host "ORPHANED MODIFIER/ANNOTATION: '$line' at line $( $i + 1 ) in $( $file.Name )" -ForegroundColor Red
                        Write-Host "File: $( $file.FullName )" -ForegroundColor Yellow
                        Write-Host "Context: Last 5 lines of file" -ForegroundColor Gray
                    }
                }
            }
        }
    }
}

Write-Host "Scan completed."
