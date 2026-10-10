[CmdletBinding(PositionalBinding = $false)]
param(
    [string]$NodeExe = '',
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$NpmArguments = @('run', 'dev')
)

$ErrorActionPreference = 'Stop'
$utf8 = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8

. (Join-Path $PSScriptRoot 'node-runtime.ps1')

$projectRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $projectRoot 'eladmin-web'
$resolvedNodeExe = Resolve-CompatibleNodeExecutable -NodeExe $NodeExe
Set-CompatibleNodePath -Executable $resolvedNodeExe

$npmCli = Join-Path (Split-Path -Parent $resolvedNodeExe) 'node_modules\npm\bin\npm-cli.js'
if (-not (Test-Path -LiteralPath $npmCli -PathType Leaf)) {
    throw "当前 Node.js 安装中未找到 npm-cli.js：$npmCli"
}

Write-Host "使用 Node.js：$resolvedNodeExe"
Push-Location -LiteralPath $frontendRoot
try {
    & $resolvedNodeExe $npmCli @NpmArguments
    if ($LASTEXITCODE -ne 0) {
        throw "前端命令执行失败，退出码：$LASTEXITCODE"
    }
} finally {
    Pop-Location
}
