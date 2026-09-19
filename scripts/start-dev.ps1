[CmdletBinding()]
param(
    [string]$JavaHome = 'C:\MyProgram\develop\Java\java1.8_8u361',
    [string]$MavenHome = 'C:\MyProgram\develop\Maven\apache-maven-3.5.4',
    [string]$MavenRepository = 'C:\MyProgram\develop\Maven\apache-maven-3.5.4\repo',
    [string]$NodeExe = '',
    [switch]$SkipBuild,
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'
$utf8 = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'eladmin'
$frontendRoot = Join-Path $projectRoot 'eladmin-web'
$logRoot = Join-Path $projectRoot 'runtime-logs'
$mavenCommand = Join-Path $MavenHome 'bin\mvn.cmd'
$mavenSettings = Join-Path $MavenHome 'conf\settings.xml'
$vueCli = Join-Path $frontendRoot 'node_modules\@vue\cli-service\bin\vue-cli-service.js'

function Assert-PathExists {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Name 不存在：$Path"
    }
}

function Test-HttpEndpoint {
    param([Parameter(Mandatory = $true)][string]$Uri)

    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 5
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 300
    } catch {
        return $false
    }
}

function Test-PortListening {
    param([Parameter(Mandatory = $true)][int]$Port)

    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-HttpEndpoint {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [Parameter(Mandatory = $true)][string]$ServiceName,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpEndpoint -Uri $Uri) {
            Write-Host "$ServiceName 已就绪：$Uri"
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "$ServiceName 在 $TimeoutSeconds 秒内未就绪，请检查 runtime-logs。"
}

function Resolve-NodeExecutable {
    if (-not [string]::IsNullOrWhiteSpace($NodeExe)) {
        Assert-PathExists -Path $NodeExe -Name 'Node.js'
        return (Resolve-Path -LiteralPath $NodeExe).Path
    }

    $candidates = New-Object System.Collections.Generic.List[string]
    if (-not [string]::IsNullOrWhiteSpace($env:NVM_SYMLINK)) {
        $candidates.Add((Join-Path $env:NVM_SYMLINK 'node.exe'))
    }

    $nvmRoots = @($env:NVM_HOME, 'C:\MyProgram\develop\nvm') |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path -LiteralPath $_) } |
        Select-Object -Unique
    foreach ($nvmRoot in $nvmRoots) {
        Get-ChildItem -LiteralPath $nvmRoot -Directory -Filter 'v16.*' -ErrorAction SilentlyContinue |
            Sort-Object { [version]($_.Name.TrimStart('v')) } -Descending |
            ForEach-Object { $candidates.Add((Join-Path $_.FullName 'node.exe')) }
    }

    $pathNode = Get-Command node.exe -ErrorAction SilentlyContinue
    if ($null -ne $pathNode) {
        $candidates.Add($pathNode.Source)
    }

    foreach ($candidate in $candidates) {
        if (-not (Test-Path -LiteralPath $candidate)) {
            continue
        }
        $versionText = & $candidate --version
        $version = [version]$versionText.TrimStart('v')
        if ($version.Major -le 16) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw '未找到兼容的 Node.js（建议 Node 16）。可通过 -NodeExe 指定 node.exe。'
}

Assert-PathExists -Path $JavaHome -Name 'Java 8'
Assert-PathExists -Path $MavenHome -Name 'Maven'
Assert-PathExists -Path $MavenRepository -Name 'Maven 本地仓库'
Assert-PathExists -Path $mavenCommand -Name 'Maven 命令'
Assert-PathExists -Path $mavenSettings -Name 'Maven settings.xml'
Assert-PathExists -Path $backendRoot -Name '后端目录'
Assert-PathExists -Path $frontendRoot -Name '前端目录'
Assert-PathExists -Path $vueCli -Name '前端 Vue CLI 依赖'

$jasyptPassword = $env:JASYPT_ENCRYPTOR_PASSWORD
if ([string]::IsNullOrWhiteSpace($jasyptPassword)) {
    $jasyptPassword = [Environment]::GetEnvironmentVariable('JASYPT_ENCRYPTOR_PASSWORD', 'User')
}
if ([string]::IsNullOrWhiteSpace($jasyptPassword)) {
    throw '未找到 JASYPT_ENCRYPTOR_PASSWORD。请先设置当前进程或 Windows 用户级环境变量。'
}

New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

$env:JAVA_HOME = $JavaHome
$env:MAVEN_HOME = $MavenHome
$env:JASYPT_ENCRYPTOR_PASSWORD = $jasyptPassword
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$env:Path = "$JavaHome\bin;$MavenHome\bin;$env:Path"

$backendHealthUri = 'http://127.0.0.1:11280/auth/code'
$frontendUri = 'http://127.0.0.1:8013/'
$proxyHealthUri = 'http://127.0.0.1:8013/auth/code'

if (-not (Test-HttpEndpoint -Uri $backendHealthUri)) {
    if (Test-PortListening -Port 11280) {
        Write-Host '后端端口已监听，等待服务完成初始化...'
        Wait-HttpEndpoint -Uri $backendHealthUri -ServiceName '后端' -TimeoutSeconds 60
    }

    if (Test-PortListening -Port 11280) {
        Write-Host "后端已在运行：$backendHealthUri"
    } else {
        if (-not $SkipBuild) {
            $testArgument = if ($RunTests) { '-DskipTests=false' } else { '-DskipTests' }
            Write-Host '开始构建后端模块...'
            & $mavenCommand `
                -s $mavenSettings `
                "-Dmaven.repo.local=$MavenRepository" `
                $testArgument `
                clean install
            if ($LASTEXITCODE -ne 0) {
                throw "后端构建失败，退出码：$LASTEXITCODE"
            }
        }

        Write-Host '启动后端...'
        $backendProcess = Start-Process `
            -FilePath $mavenCommand `
            -ArgumentList @(
                '-s',
                $mavenSettings,
                "-Dmaven.repo.local=$MavenRepository",
                '-pl',
                'eladmin-system',
                '-DskipTests',
                'spring-boot:run'
            ) `
            -WorkingDirectory $backendRoot `
            -RedirectStandardOutput (Join-Path $logRoot 'backend.out.log') `
            -RedirectStandardError (Join-Path $logRoot 'backend.err.log') `
            -WindowStyle Hidden `
            -PassThru
        Write-Host "后端启动进程 PID：$($backendProcess.Id)"
        Wait-HttpEndpoint -Uri $backendHealthUri -ServiceName '后端'
    }
} else {
    Write-Host "后端已在运行：$backendHealthUri"
}

if (-not (Test-HttpEndpoint -Uri $frontendUri)) {
    if (Test-PortListening -Port 8013) {
        Write-Host '前端端口已监听，等待服务完成初始化...'
        Wait-HttpEndpoint -Uri $frontendUri -ServiceName '前端' -TimeoutSeconds 60
    }

    if (Test-PortListening -Port 8013) {
        Write-Host "前端已在运行：$frontendUri"
    } else {
        $resolvedNodeExe = Resolve-NodeExecutable
        Write-Host "使用 Node.js：$resolvedNodeExe"
        Write-Host '启动前端...'
        $frontendProcess = Start-Process `
            -FilePath $resolvedNodeExe `
            -ArgumentList @(
                'node_modules\@vue\cli-service\bin\vue-cli-service.js',
                'serve',
                '--no-progress',
                '--host',
                '0.0.0.0',
                '--port',
                '8013'
            ) `
            -WorkingDirectory $frontendRoot `
            -RedirectStandardOutput (Join-Path $logRoot 'frontend.out.log') `
            -RedirectStandardError (Join-Path $logRoot 'frontend.err.log') `
            -WindowStyle Hidden `
            -PassThru
        Write-Host "前端启动进程 PID：$($frontendProcess.Id)"
        Wait-HttpEndpoint -Uri $frontendUri -ServiceName '前端'
    }
} else {
    Write-Host "前端已在运行：$frontendUri"
}

Wait-HttpEndpoint -Uri $proxyHealthUri -ServiceName '前端代理'
Write-Host '前后端启动并联通完成。'
