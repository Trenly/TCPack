[CmdletBinding()]
param(
    [string] $JarPath = (Join-Path (Split-Path $PSScriptRoot -Parent) 'target\TCPack-2.0.1.jar')
)

$ErrorActionPreference = 'Stop'
$repository = Split-Path $PSScriptRoot -Parent
$resolvedJar = (Resolve-Path -LiteralPath $JarPath).Path
$archive = [System.IO.Compression.ZipFile]::OpenRead($resolvedJar)

function Read-Entry {
    param([System.IO.Compression.ZipArchiveEntry] $Entry)
    $stream = $Entry.Open()
    $buffer = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($buffer)
        return ,$buffer.ToArray()
    } finally {
        $stream.Dispose()
        $buffer.Dispose()
    }
}

try {
    $expectedClasses = @(
        'me/tzahi/TCPack/TCPack.class'
        'me/tzahi/TCPack/PluginSettings.class'
        'me/tzahi/TCPack/command/PtimeCommand.class'
        'me/tzahi/TCPack/command/FlyCommand.class'
    )
    $classes = @($archive.Entries | Where-Object FullName -Like '*.class')
    $differences = Compare-Object ($expectedClasses | Sort-Object) `
        ($classes.FullName | Sort-Object)
    if ($differences) {
        throw "Unexpected packaged classes: $($differences | Out-String)"
    }
    foreach ($entry in $classes) {
        [byte[]] $bytes = Read-Entry $entry
        if ($bytes.Length -lt 8 -or [BitConverter]::ToString($bytes, 0, 4) -ne 'CA-FE-BA-BE') {
            throw "Invalid class file: $($entry.FullName)"
        }
        $major = ([int] $bytes[6] -shl 8) -bor [int] $bytes[7]
        if ($major -ne 52) {
            throw "Not Java 8 bytecode: $($entry.FullName) (major $major)"
        }
        $constants = [Text.Encoding]::UTF8.GetString($bytes)
        if ($constants -match 'org/mcstats|org/bstats|net/minecraft|org/bukkit/craftbukkit') {
            throw "Unexpected telemetry/internal API reference: $($entry.FullName)"
        }
    }
    foreach ($name in @('LICENSE', 'NOTICE')) {
        $entry = $archive.GetEntry("META-INF/$name")
        if ($null -eq $entry) {
            throw "Missing packaged $name"
        }
        [byte[]] $packaged = Read-Entry $entry
        $original = [IO.File]::ReadAllBytes((Join-Path $repository $name))
        if ([Convert]::ToBase64String($packaged) -cne [Convert]::ToBase64String($original)) {
            throw "Packaged $name differs from the original"
        }
    }
    $descriptorEntry = $archive.GetEntry('plugin.yml')
    if ($null -eq $descriptorEntry) {
        throw 'Missing plugin.yml'
    }
    $descriptor = [Text.Encoding]::UTF8.GetString((Read-Entry $descriptorEntry))
    if ($descriptor -notmatch "(?m)^version: ['""]?2\.0\.1['""]?\s*$") {
        throw 'Plugin version is not 2.0.1'
    }
    if ($descriptor -match '\$\{') {
        throw 'Unresolved Maven resource placeholder in plugin.yml'
    }
    if ($null -eq $archive.GetEntry('config.yml')) {
        throw 'Missing config.yml'
    }
} finally {
    $archive.Dispose()
}

$hash = (Get-FileHash -LiteralPath $resolvedJar -Algorithm SHA256).Hash
[pscustomobject]@{
    Jar = $resolvedJar
    SHA256 = $hash
    Version = '2.0.1'
    ClassFileMajor = 52
    LicenseAndNotice = 'Identical to repository originals'
    Telemetry = 'None'
}
