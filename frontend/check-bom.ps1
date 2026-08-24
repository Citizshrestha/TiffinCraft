$ErrorActionPreference = 'SilentlyContinue'
$files = Get-ChildItem -Path 'app\src\main\res' -Recurse -Include '*.xml'
foreach ($file in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Write-Output ('BOM: ' + $file.FullName)
    }
}
Write-Output 'Scan complete.'