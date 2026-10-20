#requires -Version 7.0
<#
.SYNOPSIS
Runs one supplied release JAR in a fresh loopback-only server with proxy probes.
.DESCRIPTION
Requires explicit server, JVM and isolated directory paths. Never reuses a directory,
downloads software, rebuilds the release, or accepts an EULA without -AcceptEula.
Evidence is retained even on failure. These are in-server proxy probes, NOT real
connected-player, protocol, visual, or client end-to-end tests. The driver never
stops the server; this script owns and stops only the process it creates.
Legacy Paperclip's obsolete download URL can be bypassed with -VanillaServerJar
and -ExpectedVanillaSha1 from Mojang's official version manifest. The original
JAR must also match Paperclip's embedded SHA256. Only the inspected 1.8.8/1.12.2
cache layouts are supported; neither the server JAR nor its metadata is changed.
.EXAMPLE
.\tools\Test-Compatibility.ps1 -PluginJar .\target\TCPack-2.1.0.jar `
  -ServerJar E:\servers\paper.jar -JavaPath E:\jdk8\bin\java.exe `
  -WorkingDirectory E:\TCPack\target\runtime-1.8.8-run1 -Port 25581 -AcceptEula
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string] $PluginJar,
    [Parameter(Mandatory)][string] $ServerJar,
    [Parameter(Mandatory)][string] $JavaPath,
    [Parameter(Mandatory)][string] $WorkingDirectory,
    [Parameter(Mandatory)][ValidateRange(1024, 65535)][int] $Port,
    [string] $DriverJar = "$PSScriptRoot\runtime-driver\target\tcpack-runtime-driver-2.0.jar",
    [string] $VanillaServerJar,
    [ValidatePattern('^[a-fA-F0-9]{40}$')][string] $ExpectedVanillaSha1,
    [ValidateRange(30, 1800)][int] $StartupTimeoutSeconds = 600,
    [ValidateRange(10, 300)][int] $ProbeTimeoutSeconds = 90,
    [ValidateRange(10, 300)][int] $ShutdownTimeoutSeconds = 60,
    [ValidateRange(512, 8192)][int] $MaxMemoryMb = 1024,
    [switch] $AcceptEula
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (-not $AcceptEula) {
    throw 'EULA acceptance is not implicit. Review https://aka.ms/MinecraftEULA and explicitly supply -AcceptEula.'
}
function Resolve-File([string] $Path) {
    $item = Get-Item -LiteralPath $Path
    if ($item.PSIsContainer) { throw "Expected a file: $Path" }
    return $item.FullName
}
$PluginJar = Resolve-File $PluginJar
$ServerJar = Resolve-File $ServerJar
$DriverJar = Resolve-File $DriverJar
$JavaPath = Resolve-File $JavaPath
$vanillaEvidence = $null
$vanillaCacheName = $null
if ($VanillaServerJar -or $ExpectedVanillaSha1) {
    if (-not $VanillaServerJar -or -not $ExpectedVanillaSha1) {
        throw 'Supply both -VanillaServerJar and -ExpectedVanillaSha1 from the official Mojang version manifest.'
    }
    $VanillaServerJar = Resolve-File $VanillaServerJar
    $sha1 = (Get-FileHash -LiteralPath $VanillaServerJar -Algorithm SHA1).Hash
    if ($sha1 -ne $ExpectedVanillaSha1) { throw 'Vanilla server does not match expected official Mojang SHA1.' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($ServerJar)
    try {
        $entry = $archive.GetEntry('patch.json')
        if ($null -eq $entry) { throw 'Vanilla preseed requires legacy Paperclip patch.json.' }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $patch = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    } finally { $archive.Dispose() }
    # These layouts were verified from each official Paperclip class's bytecode.
    $layouts = @{ '1.8.8' = 'original.jar'; '1.12.2' = 'mojang_1.12.2.jar' }
    if (-not $layouts.ContainsKey($patch.version)) { throw "Unverified Paperclip cache layout: $($patch.version)" }
    $sha256 = (Get-FileHash -LiteralPath $VanillaServerJar -Algorithm SHA256).Hash
    if ($sha256 -ne $patch.originalHash) { throw 'Vanilla server does not match embedded Paperclip original SHA256.' }
    $vanillaCacheName = $layouts[$patch.version]
    $vanillaEvidence = [ordered]@{
        jar = $VanillaServerJar
        version = $patch.version
        sha1 = $sha1
        sha256 = $sha256
        expectedMojangSha1 = $ExpectedVanillaSha1
        embeddedPaperclipSha256 = $patch.originalHash
        cacheFile = "cache\$vanillaCacheName"
    }
}
$work = [IO.Path]::GetFullPath($WorkingDirectory)
if (Test-Path -LiteralPath $work) { throw "Refusing existing working directory: $work" }

# Detect occupied ports without connecting to or changing another server.
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
try { $listener.Start() } finally { $listener.Stop() }
[void][IO.Directory]::CreateDirectory($work)
$token = [Guid]::NewGuid().ToString('N')
[IO.File]::WriteAllText((Join-Path $work '.tcpack-harness'), $token)
[void][IO.Directory]::CreateDirectory((Join-Path $work 'plugins'))
Copy-Item -LiteralPath $PluginJar -Destination (Join-Path $work 'plugins\TCPack.jar')
Copy-Item -LiteralPath $DriverJar -Destination (Join-Path $work 'plugins\TCPackRuntimeDriver.jar')
Copy-Item -LiteralPath $ServerJar -Destination (Join-Path $work 'server.jar')
if ($vanillaCacheName) {
    $cache = Join-Path $work 'cache'
    [void][IO.Directory]::CreateDirectory($cache)
    Copy-Item -LiteralPath $VanillaServerJar -Destination (Join-Path $cache $vanillaCacheName)
}
[IO.File]::WriteAllText((Join-Path $work 'eula.txt'), "eula=true`n")
[IO.File]::WriteAllText((Join-Path $work 'server.properties'), @"
server-ip=127.0.0.1
server-port=$Port
enable-query=false
enable-rcon=false
online-mode=true
max-players=1
view-distance=2
simulation-distance=2
spawn-protection=0
allow-nether=false
motd=TCPack isolated compatibility harness - not a live server
"@)
$javaVersion = (& $JavaPath -version 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0) { throw "Cannot query JVM: $javaVersion" }
$evidence = [ordered]@{
    scope = 'In-server proxy probes; not connected-player/client/visual E2E'
    startedUtc = [DateTime]::UtcNow.ToString('o')
    status = 'Running'
    pluginJar = $PluginJar
    pluginSha256 = (Get-FileHash -LiteralPath $PluginJar -Algorithm SHA256).Hash
    serverJar = $ServerJar
    serverSha256 = (Get-FileHash -LiteralPath $ServerJar -Algorithm SHA256).Hash
    vanillaPreseed = $vanillaEvidence
    driverSha256 = (Get-FileHash -LiteralPath $DriverJar -Algorithm SHA256).Hash
    javaPath = $JavaPath
    javaVersion = $javaVersion
    workingDirectory = $work
    address = "127.0.0.1:$Port"
    eulaExplicitlyAccepted = [bool]$AcceptEula
    serverEvidence = @()
    processId = $null
    exitCode = $null
    gracefulShutdown = $false
    failure = $null
}
$evidencePath = Join-Path $work 'compatibility-evidence.json'
function Save-Evidence {
    $evidence | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $evidencePath -Encoding utf8
}
Save-Evidence
$start = [Diagnostics.ProcessStartInfo]::new()
$start.FileName = $JavaPath
$start.WorkingDirectory = $work
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardInput = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
foreach ($argument in @("-Xms128M", "-Xmx${MaxMemoryMb}M",
    "-Dtcpack.harness.directory=$work", "-Dtcpack.harness.token=$token",
    '-jar', 'server.jar', 'nogui')) {
    $start.ArgumentList.Add($argument)
}
$process = [Diagnostics.Process]::new()
$process.StartInfo = $start
$log = [IO.StreamWriter]::new((Join-Path $work 'harness-console.log'), $false, [Text.UTF8Encoding]::new($false))
$log.AutoFlush = $true
$state = @{ ready = $false; passed = $false; fatal = $false }
$streams = @()
function Read-AvailableLines {
    foreach ($stream in $streams) {
        # Drain completed line tasks without blocking either pipe.
        while ($null -ne $stream.task -and $stream.task.IsCompleted) {
            $line = $stream.task.GetAwaiter().GetResult()
            if ($null -eq $line) { $stream.task = $null; break }
            $log.WriteLine($line)
            if ($line -match 'Done \(') { $state.ready = $true }
            if ($line -match 'TCPACK_RUNTIME_PASS assertions=\d+') { $state.passed = $true }
            if ($line -match 'TCPACK_RUNTIME_(PASS|FAIL|EVIDENCE)') {
                $evidence.serverEvidence += $line
            }
            if ($line -match 'TCPACK_RUNTIME_FAIL|UnsupportedClassVersionError|NoSuchMethodError|NoClassDefFoundError|Could not load.*TCPack|Error occurred while enabling TCPack') {
                $state.fatal = $true
            }
            $stream.task = $stream.reader.ReadLineAsync()
        }
    }
}
try {
    if (-not $process.Start()) { throw 'JVM process did not start.' }
    $evidence.processId = $process.Id
    Save-Evidence
    $streams = @(
        @{ reader = $process.StandardOutput; task = $process.StandardOutput.ReadLineAsync() },
        @{ reader = $process.StandardError; task = $process.StandardError.ReadLineAsync() }
    )
    $startup = [Diagnostics.Stopwatch]::StartNew()
    $probe = $null
    while ($true) {
        Read-AvailableLines
        if ($state.fatal) { throw 'Server/driver failure; inspect harness-console.log.' }
        if ($process.HasExited) { throw "Server exited before successful probes (exit $($process.ExitCode))." }
        if ($state.ready -and $state.passed) { break }
        if (-not $state.ready -and $startup.Elapsed.TotalSeconds -gt $StartupTimeoutSeconds) {
            throw 'Server readiness timeout.'
        }
        if ($state.ready -and $null -eq $probe) { $probe = [Diagnostics.Stopwatch]::StartNew() }
        if ($null -ne $probe -and $probe.Elapsed.TotalSeconds -gt $ProbeTimeoutSeconds) {
            throw 'Runtime driver timeout; startup alone does not count as a pass.'
        }
        Start-Sleep -Milliseconds 100
    }
    $evidence.status = 'Passed'
} catch {
    $evidence.status = 'Failed'
    $evidence.failure = $_.Exception.Message
} finally {
    if ($null -ne $evidence.processId) {
        if (-not $process.HasExited) {
            try {
                $process.StandardInput.WriteLine('stop')
                $process.StandardInput.Flush()
            } catch {
                $evidence.failure = "Cannot request graceful stop: $($_.Exception.Message)"
                $evidence.status = 'Failed'
            }
            $shutdown = [Diagnostics.Stopwatch]::StartNew()
            while (-not $process.HasExited -and $shutdown.Elapsed.TotalSeconds -lt $ShutdownTimeoutSeconds) {
                Read-AvailableLines
                Start-Sleep -Milliseconds 100
            }
            if (-not $process.HasExited) {
                # Only the specific process tree launched by this invocation.
                $process.Kill($true)
                $process.WaitForExit()
                $evidence.status = 'Failed'
                $evidence.failure = 'Graceful shutdown timed out; owned process tree terminated.'
            } else {
                $evidence.gracefulShutdown = $true
            }
        }
        $process.WaitForExit()
        Read-AvailableLines
        $evidence.exitCode = $process.ExitCode
        if ($process.ExitCode -ne 0 -or $state.fatal) {
            $evidence.status = 'Failed'
            if (-not $evidence.failure) { $evidence.failure = 'Nonzero exit or fatal server/driver log.' }
        }
    }
    $log.Dispose()
    $process.Dispose()
    $evidence['completedUtc'] = [DateTime]::UtcNow.ToString('o')
    Save-Evidence
}
if ($evidence.status -ne 'Passed') {
    throw "Compatibility run failed: $($evidence.failure) Evidence: $evidencePath"
}
Write-Output "TCPack in-server proxy probes passed; evidence: $evidencePath"
