$ErrorActionPreference = 'Continue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }

$url = "https://github.com/ultralytics/ultralytics/releases/download/v8.2.0/yolov8n.onnx"
$output = "D:\works\Project\green-robot-monitor-v3\backend\models\yolov8n.onnx"

Write-Host "Downloading YOLOv8n model from $url"
Write-Host "Target: $output"

try {
    # Method 1: Invoke-WebRequest with SSL bypass
    Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing -TimeoutSec 120
    Write-Host "Download complete!"
} catch {
    Write-Host "Method 1 failed: $($_.Exception.Message)"
    
    # Method 2: Using WebClient with more options
    try {
        $wc = New-Object System.Net.WebClient
        $wc.Headers.Add("User-Agent", "Mozilla/5.0")
        $wc.DownloadFile($url, $output)
        Write-Host "Method 2 download complete!"
    } catch {
        Write-Host "Method 2 also failed: $($_.Exception.Message)"
    }
}

if (Test-Path $output) {
    $fileSize = (Get-Item $output).Length
    Write-Host "File size: $fileSize bytes"
    
    if ($fileSize -lt 1000) {
        Write-Host "File too small, download may have failed. Content:"
        Get-Content $output
    }
} else {
    Write-Host "File not found after download attempt"
}