function Get-CompatibleNodeVersion {
    param([Parameter(Mandatory = $true)][string]$Executable)

    if (-not (Test-Path -LiteralPath $Executable -PathType Leaf)) {
        return $null
    }

    try {
        $versionText = & $Executable --version 2>$null
        $version = [version]([string]$versionText).Trim().TrimStart('v')
        if ($version.Major -in @(14, 16)) {
            return $version
        }
    } catch {
        return $null
    }

    return $null
}

function Get-NvmRoots {
    $roots = New-Object System.Collections.Generic.List[string]
    foreach ($root in @($env:NVM_HOME, (Join-Path $env:APPDATA 'nvm'), (Join-Path $env:LOCALAPPDATA 'nvm'), 'C:\MyProgram\develop\nvm')) {
        if (-not [string]::IsNullOrWhiteSpace($root)) {
            $roots.Add($root)
        }
    }

    $nvmCommand = Get-Command nvm.exe -ErrorAction SilentlyContinue
    if ($null -ne $nvmCommand) {
        $nvmDirectory = Split-Path -Parent $nvmCommand.Source
        $roots.Add($nvmDirectory)
        $settingsPath = Join-Path $nvmDirectory 'settings.txt'
        if (Test-Path -LiteralPath $settingsPath) {
            $rootSetting = Get-Content -LiteralPath $settingsPath -Encoding UTF8 |
                Where-Object { $_ -match '^\s*root\s*:' } |
                Select-Object -First 1
            if ($rootSetting -match '^\s*root\s*:\s*(.+?)\s*$') {
                $roots.Add($Matches[1])
            }
        }
    }

    return @($roots |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path -LiteralPath $_ -PathType Container) } |
        ForEach-Object { (Resolve-Path -LiteralPath $_).Path } |
        Select-Object -Unique)
}

function Resolve-CompatibleNodeExecutable {
    param([string]$NodeExe = '')

    if (-not [string]::IsNullOrWhiteSpace($NodeExe)) {
        $resolved = (Resolve-Path -LiteralPath $NodeExe -ErrorAction Stop).Path
        if ($null -eq (Get-CompatibleNodeVersion -Executable $resolved)) {
            throw "指定的 Node.js 版本不兼容，仅支持 Node 16 或 Node 14：$resolved"
        }
        return $resolved
    }

    $candidates = New-Object System.Collections.Generic.List[string]
    $nvmRoots = Get-NvmRoots
    foreach ($major in @(16, 14)) {
        foreach ($nvmRoot in $nvmRoots) {
            Get-ChildItem -LiteralPath $nvmRoot -Directory -Filter "v$major.*" -ErrorAction SilentlyContinue |
                Sort-Object { [version]($_.Name.TrimStart('v')) } -Descending |
                ForEach-Object { $candidates.Add((Join-Path $_.FullName 'node.exe')) }
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($env:NVM_SYMLINK)) {
        $candidates.Add((Join-Path $env:NVM_SYMLINK 'node.exe'))
    }
    $pathNode = Get-Command node.exe -ErrorAction SilentlyContinue
    if ($null -ne $pathNode) {
        $candidates.Add($pathNode.Source)
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ($null -ne (Get-CompatibleNodeVersion -Executable $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw '未找到兼容的 Node.js。请安装 Node 16（首选）或 Node 14，或通过 -NodeExe 指定 node.exe。'
}

function Set-CompatibleNodePath {
    param([Parameter(Mandatory = $true)][string]$Executable)

    $nodeDirectory = Split-Path -Parent $Executable
    $pathParts = @($env:Path -split ';' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and $_ -ne $nodeDirectory })
    $env:Path = (@($nodeDirectory) + $pathParts) -join ';'
}
