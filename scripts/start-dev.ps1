[CmdletBinding()]
param(
    [string]$JavaHome = 'C:\MyProgram\develop\Java\java1.8_8u361',
    [string]$MavenHome = 'C:\MyProgram\develop\Maven\apache-maven-3.5.4',
    [string]$MavenRepository = 'C:\MyProgram\develop\Maven\apache-maven-3.5.4\repo',
    [string]$NodeExe = '',
    [switch]$SkipBuild,
    [switch]$RunTests,
    [switch]$RestartBackend
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
$localEnvFile = Join-Path $projectRoot '.codex-local\env.ps1'

if (Test-Path -LiteralPath $localEnvFile) {
    . $localEnvFile
}

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
        [int]$TimeoutSeconds = 120,
        [System.Diagnostics.Process]$Process,
        [string[]]$FailureLogPaths = @()
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpEndpoint -Uri $Uri) {
            Write-Host "$ServiceName 已就绪：$Uri"
            return
        }
        if ($null -ne $Process) {
            $Process.Refresh()
            if ($Process.HasExited) {
                foreach ($logPath in $FailureLogPaths) {
                    if (Test-Path -LiteralPath $logPath) {
                        Write-Host "===== $logPath ====="
                        Get-Content -LiteralPath $logPath -Encoding UTF8 -Tail 40
                    }
                }
                throw "$ServiceName 启动进程已提前退出，退出码：$($Process.ExitCode)"
            }
        }
        Start-Sleep -Seconds 2
    }

    throw "$ServiceName 在 $TimeoutSeconds 秒内未就绪，请检查 runtime-logs。"
}

function Wait-PortClosed {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-PortListening -Port $Port)) {
            return
        }
        Start-Sleep -Milliseconds 500
    }

    throw "端口 $Port 在 $TimeoutSeconds 秒内未释放。"
}

function Get-BackendListenerProcess {
    $processIds = @(Get-NetTCPConnection -LocalPort 11280 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)
    if ($processIds.Count -eq 0) {
        return $null
    }
    if ($processIds.Count -ne 1) {
        throw "后端端口 11280 存在多个监听进程，拒绝自动停止：$($processIds -join ', ')"
    }
    return Get-CimInstance Win32_Process -Filter "ProcessId = $($processIds[0])"
}

function Stop-BackendService {
    $backendProcess = Get-BackendListenerProcess
    if ($null -eq $backendProcess) {
        Write-Host '后端未运行，无需停止。'
        return
    }

    $expectedClasses = Join-Path $backendRoot 'eladmin-system\target\classes'
    $commandLine = [string]$backendProcess.CommandLine
    if (-not $commandLine.Contains($expectedClasses) -or -not $commandLine.Contains('me.zhengjie.AppRun')) {
        throw "端口 11280 的进程不属于当前项目，拒绝停止。PID：$($backendProcess.ProcessId)"
    }

    $parentProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $($backendProcess.ParentProcessId)" -ErrorAction SilentlyContinue
    $parentOwned = $null -ne $parentProcess -and
        ([string]$parentProcess.CommandLine).Contains($backendRoot) -and
        ([string]$parentProcess.CommandLine).Contains('spring-boot:run')

    Write-Host "停止后端进程 PID：$($backendProcess.ProcessId)"
    Stop-Process -Id $backendProcess.ProcessId -Force
    if ($parentOwned -and (Get-Process -Id $parentProcess.ProcessId -ErrorAction SilentlyContinue)) {
        Write-Host "停止 Maven 父进程 PID：$($parentProcess.ProcessId)"
        Stop-Process -Id $parentProcess.ProcessId -Force
    }
    Wait-PortClosed -Port 11280
}

function Invoke-BackendBuild {
    $testArgument = if ($RunTests) { '-DskipTests=false' } else { '-DskipTests' }
    Write-Host '开始构建后端模块...'
    Push-Location -LiteralPath $backendRoot
    try {
        & $mavenCommand `
            -s $mavenSettings `
            "-Dmaven.repo.local=$MavenRepository" `
            $testArgument `
            clean install
        if ($LASTEXITCODE -ne 0) {
            throw "后端构建失败，退出码：$LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

function Start-BackendService {
    $stdoutLog = Join-Path $logRoot 'backend.out.log'
    $stderrLog = Join-Path $logRoot 'backend.err.log'
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
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -WindowStyle Hidden `
        -PassThru
    Write-Host "后端启动进程 PID：$($backendProcess.Id)"
    Wait-HttpEndpoint `
        -Uri $backendHealthUri `
        -ServiceName '后端' `
        -Process $backendProcess `
        -FailureLogPaths @($stdoutLog, $stderrLog)
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
$env:LOG_PATH = Join-Path $logRoot 'backend'
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$env:Path = "$JavaHome\bin;$MavenHome\bin;$env:Path"

$backendHealthUri = 'http://127.0.0.1:11280/auth/code'
$frontendUri = 'http://127.0.0.1:8013/'
$proxyHealthUri = 'http://127.0.0.1:8013/auth/code'

if ($RestartBackend) {
    if (-not $SkipBuild) {
        Invoke-BackendBuild
    }
    Stop-BackendService
    Start-BackendService
} elseif (-not (Test-HttpEndpoint -Uri $backendHealthUri)) {
    if (Test-PortListening -Port 11280) {
        Write-Host '后端端口已监听，等待服务完成初始化...'
        Wait-HttpEndpoint -Uri $backendHealthUri -ServiceName '后端' -TimeoutSeconds 60
    }

    if (Test-PortListening -Port 11280) {
        Write-Host "后端已在运行：$backendHealthUri"
    } else {
        if (-not $SkipBuild) {
            Invoke-BackendBuild
        }
        Start-BackendService
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
