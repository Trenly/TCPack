#requires -Version 7.0
<#
.SYNOPSIS
Compiles production sources against an alternate provided Spigot API in isolation.
.DESCRIPTION
Does not package or overwrite target\TCPack-2.0.1.jar. This is a source compilation
check, not runtime linkage or server support evidence. Requires an installed JDK
on JAVA_HOME; uses the repository's pinned Maven 3.9.11 wrapper.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^[0-9][A-Za-z0-9.+-]*$')]
    [string] $ApiVersion
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$work = Join-Path $root ("target\api-checks\" + $ApiVersion + '-' + [Guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory((Join-Path $work 'src'))
Copy-Item -LiteralPath (Join-Path $root 'pom.xml') -Destination $work
Copy-Item -LiteralPath (Join-Path $root 'src\main') -Destination (Join-Path $work 'src') -Recurse
& (Join-Path $root 'mvnw.cmd') --batch-mode --no-transfer-progress -f (Join-Path $work 'pom.xml') `
    "-Dspigot.api.version=$ApiVersion" '-Dmaven.test.skip=true' compile
if ($LASTEXITCODE -ne 0) { throw "API compile failed for $ApiVersion. Isolated sources/output: $work" }
Write-Output "API source compilation passed: $ApiVersion. Output: $work (not a runtime check)"
