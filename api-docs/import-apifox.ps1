#!/usr/bin/env pwsh
$token = "afxp_383ddex9QBNQQxwpvZkuh19MfhxToX4DJAp2"
$projectId = "8198077"
$jsonPath = "D:\works\Project\green-robot-monitor-v3\api-docs\openapi-green-robot-pro.json"
$json = Get-Content $jsonPath -Raw -Encoding UTF8
$body = @{
    input = @{string = $json}
    options = @{
        schemaOverwriteBehavior = "OVERWRITE_EXISTING"
        endpointOverwriteBehavior = "OVERWRITE_EXISTING"
    }
} | ConvertTo-Json -Depth 10

$response = Invoke-WebRequest -Uri "https://api.apifox.com/v1/projects/$projectId/import-openapi" -Method POST -Headers @{
    "X-Apifox-Api-Version" = "2024-03-28"
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
} -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 120

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10