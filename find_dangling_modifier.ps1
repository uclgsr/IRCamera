$kotlinFiles = Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt"

Write-Host "Scanning $($kotlinFiles.Count) Kotlin files for dangling modifiers..."

foreach ($file in $kotlinFiles) {
    $content = Get-Content -Path $file.FullName -ErrorAction SilentlyContinue
    if ($content) {
        for ($i = 0; $i -lt $content.Length; $i++) {
            $line = $content[$i].Trim()
            
            # Check for lines that are just modifiers
            if ($line -match "^(private|public|protected|internal)$") {
                Write-Host "FOUND DANGLING MODIFIER: $line at line $($i+1) in $($file.Name)" -ForegroundColor Red
                Write-Host "File: $($file.FullName)" -ForegroundColor Yellow
            }
            
            # Check for lines ending with just a modifier and whitespace
            if ($line -match "(private|public|protected|internal)\s*$" -and $line.Length -lt 20) {
                Write-Host "POTENTIAL ISSUE: '$line' at line $($i+1) in $($file.Name)" -ForegroundColor Yellow
                Write-Host "File: $($file.FullName)" -ForegroundColor Gray
            }
            
            # Check for incomplete declarations
            if ($line -match "^(private|public|protected|internal)\s+(class|interface|fun|val|var)$") {
                Write-Host "INCOMPLETE DECLARATION: '$line' at line $($i+1) in $($file.Name)" -ForegroundColor Red
                Write-Host "File: $($file.FullName)" -ForegroundColor Yellow
            }
        }
    }
}

Write-Host "Scan completed."