# Compatibility and verification

TCPack 2.x targets Minecraft 1.8 through 26.3 on Paper, with Bukkit/Spigot
compatibility where available. Folia is excluded. One Java-8-compatible
plugin JAR is used for all targets; server implementations and JVMs are
not bundled.

## Evidence levels

Unit tests verify command decisions and state changes using the common
Bukkit API. Compiling/linking against server APIs verifies available API
signatures, not actual server behavior. Starting a server verifies plugin
loading; exercising commands as players is additionally necessary to
verify personal time and flight. These checks are not interchangeable.

Representative versions for runtime verification are 1.8 (if available),
1.8.8, 1.12.2, 1.13.2, 1.16.5, 1.17.1, 1.20.6, 1.21.11, and 26.3.
The reported modern endpoint is Paper 26.3 build 135, API
`26.3.build.135-beta`. A substituted server build must be identified as
such, never reported as that exact endpoint.

## Environment

Builds must enforce both Java 8 bytecode and Java 8 library APIs. A newer
JDK running Maven does not change the plugin's Java 8 runtime target.
Use the wrapper from the repository root:

```powershell
.\mvnw.cmd verify
```

The production server API has provided scope; it must not appear inside
`target/TCPack-2.1.0.jar`. The JAR must contain only the maintained code,
plugin/configuration resources, and attribution/license metadata, never
the old Eclipse `TCPack/bin` classes.

Server test environments must use their own directories/worlds, unique
ports bound to loopback, exact pinned server builds, and JVMs supported by
those builds. See Paper's [current JVM guidance](https://docs.papermc.io/paper/getting-started/);
Paper 26.3 uses Java 25. Legacy servers generally need older JVMs.
Download tools/JDKs/server files from official sources, verify publisher
checksums where available, and keep them outside git.

## Runtime prerequisites and safety

The server owner must authorize acceptance of the
[Minecraft EULA](https://aka.ms/MinecraftEULA) before test servers run.
The maintainer explicitly authorized acceptance for the isolated test
instances used for this refactor. This does not authorize changes to any
live server. Runtime acceptance must not be represented as complete
merely because unit tests or artifact checks pass.

Do not run these checks against a live server, expose offline-mode test
servers publicly, use plugin hot-reload, or share world data between
versions. Stop only the test processes started for these checks.

For every runtime target, record the JAR SHA-256, exact server build,
exact JVM, fresh/existing configuration results, startup/shutdown logs,
player/operator/console outcomes, permissions, disabled flags, offline
targets, and retained command results. Confirm there are no Metrics
scheduler errors. Exercise `/tcpack:ptime` and `/tcpack:fly` to isolate
command conflicts with other plugins.

The legacy v1.0 downloaded plugin and its Metrics source are not present
in this checkout, so a faithful execution of its reported failure cannot
be claimed. The replacement's absence of Metrics code/dependencies can
be checked directly in the source, dependency graph, and built JAR.

## Recorded results

The initial 2.0 refactor artifact tested in the eight-server matrix below
had SHA-256:

```text
193c75e6c9707b655fe9bc8776d6096b78cb34a4e93f3c0a711948cefe7998e1
```

Two clean Maven builds produced byte-identical JARs. All 30 unit tests
passed on both Temurin Java 8u504 and Java 25.0.4.1. The build uses Maven
Wrapper 3.9.11, JUnit Jupiter 5.11.4, and the provided Spigot API
`1.8.8-R0.1-20160221.082514-43`. Java 8 library APIs are enforced during
compilation; packaged class-file major versions are 52.

Artifact checks confirm that only the four maintained production classes
are packaged, no telemetry/server/test classes are bundled, version is
2.0, and packaged LICENSE/NOTICE bytes match the repository originals.
Run these checks with:

```powershell
.\tools\Test-Artifact.ps1
```

Public player-time/flight API probes compiled against both the exact
legacy API above and official Paper API `26.3.build.135-beta` (SHA-256
`51c9101da1b6dce83d5a7203f19213d8cec1f98f860c747ebcd6d4f24fbc5c24`).
All four production source files also compiled directly against this
exact modern API and its official resolved libraries. That supplemental
compile used Java 8 source/target settings to read the Java-25 API;
the distributed JAR remains the original `--release 8` legacy-API build.

The isolated server harness loads TCPack plus a **separate test-only
plugin** and exercises registered commands with proxy player/sender
objects inside the actual server. This establishes real plugin
loading/command dispatch and API linkage, **not connected-player or
visual client behavior**.

| Official Paper version | Build | JVM | Same-JAR in-server probes |
| --- | --- | --- | --- |
| 1.8.8 | 443 | Temurin 8u504 | Passed |
| 1.12.2 | 1205 | Temurin 8u504 | Passed |
| 1.13.2 | 387 | Temurin 8u504 | Passed |
| 1.16.5 | 427 | Temurin 8u504 | Passed |
| 1.17.1 | 81 | Temurin 16.0.2+7 | Passed |
| 1.20.6 | 115 | Temurin 21.0.12.1 | Passed |
| 1.21.11 | 67 | Temurin 21.0.12.1 | Passed |
| 26.3 | 135 | Temurin 25.0.4.1 | Passed |

Every row passed **167 in-server assertions** using the same production
JAR and final test driver, followed by graceful server shutdown. The
checks include namespaced/plain registry ownership, command permission
decisions, and time/flight API interactions. Metadata defaults and
existing/fresh configuration variations are covered by unit tests, not
claimed as connected-player runtime evidence. The test driver uses a
descriptor-preserving player stub to handle the legacy 1.8.8 API's
incompatible `getHealth()` return descriptors; this is test-only code.

The original-style `/ptime` help restoration before the 2.0.1 version bump was verified
with 31 unit tests on Java 8 and 25 and 167 in-server assertions on
Paper 1.8.8 build 443 and 26.3 build 135. Its rebuilt production JAR has
SHA-256 `9f9f67e0b5618548c23bbdf88df466e64510b7d671e0731c6eab1b1e572531fa`.
The intermediate rows above describe the initial artifact, not a rerun
of this help-only change.

Paper 1.8 itself was not available from the official archive; 1.8.8 is
the acquired legacy endpoint. No result for exact 1.8 is claimed. Full
connected-player testing and every historical patch release remain
unverified.

## Compression release 2.1.0

The 2.1.0 release restores compression and `/tcpack` aggregate help while retaining one legacy-API
Java-8-compatible JAR. Its production artifact is
`target/TCPack-2.1.0.jar`, SHA-256:

```text
cce9e76148bde36e129249f0e7e6b4ff3f66edc588af040f2cdde1dc9c3b6a9e
```

All 55 unit tests passed on Java 8 and 25. The same release JAR passed
the following isolated runtime matrix, including all retained command
checks and compression using the server's real ItemStacks and inventory
storage behind the test player/inventory interface stubs:

| Paper version | Build | JVM | Assertions | Bonemeal output |
| --- | --- | --- | --- | --- |
| 1.8.8 | 443 | Temurin 8u504 | 350 | Bones |
| 1.12.2 | 1205 | Temurin 8u504 | 400 | Bone blocks |
| 1.13.2 | 387 | Temurin 8u504 | 400 | Bone blocks |
| 1.16.5 | 427 | Temurin 8u504 | 421 | Bone blocks |
| 1.17.1 | 81 | Temurin 16.0.2+7 | 449 | Bone blocks |
| 1.20.6 | 115 | Temurin 21.0.12.1 | 466 | Bone blocks |
| 1.21.11 | 67 | Temurin 21.0.12.1 | 473 | Bone blocks |
| 26.3 | 135 | Temurin 25.0.4.1 | 480 | Bone blocks |

Each server stopped gracefully with exit code zero. Probes exercised all
available plank-to-base-log/stem mappings, rejected bamboo where present,
checked legacy species/dye data and modern raw-ore/ingot blocks, preserved
remainders and named/lore items, ignored armor/offhand, and verified
all-or-nothing rejection of a full inventory. These are not connected
player/client or visual tests; the exact 1.8 limitation still applies.
Nugget probes cover iron/gold where available, chaining into blocks in
one invocation, nugget-derived ingot priority, mixed inputs, remainders,
custom metadata, final-output-only space checks, and atomic rollback.
Redstone and lapis probes verify 9:1 block conversion and remainders,
including legacy lapis dye data; unit tests protect other dye variants.
Before aggregate help and nugget compression were restored, all eight
production source files also compiled against Spigot 1.13.2,
1.20.6, and 1.21.11 in isolated API checks and against the exact Paper
26.3 build 135 API/libraries. The release remains the `--release 8`
legacy-API build, not one of those alternate compilation outputs.

## Reproducing server probes

Provision an official Paper JAR and the appropriate Java executable,
then create a **new** isolated work-directory path for each run:

```powershell
.\tools\Test-Compatibility.ps1 `
    -PluginJar .\target\TCPack-2.1.0.jar `
    -ServerJar 'C:\test-tools\paper-26.3-135.jar' `
    -JavaPath 'C:\test-tools\jdk25\bin\java.exe' `
    -WorkingDirectory 'C:\test-runs\tcpack-26.3-new' `
    -Port 25587 `
    -AcceptEula
```

First build the separate runtime driver using
`.\mvnw.cmd -f tools\runtime-driver\pom.xml package`.
`-AcceptEula` is an explicit authorization flag, not a default. The
runner records evidence/logs under its isolated directory, waits for successful
probes, and stops its own process. See script parameters for overrides
and timeouts. Do not install the test driver in a real server's plugins
directory.

Paperclip for 1.8.8 and 1.12.2 uses retired Mojang S3 URLs. Obtain the
vanilla server from the
[official Mojang version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
and supply `-VanillaServerJar <path>` and `-ExpectedVanillaSha1 <hash>`
to seed the cache without modifying the official Paper JAR. The runner
also checks Paperclip's embedded SHA-256. The manifest SHA-1 values are
`5fafba3f58c40dc51b5c3ca72a98f62dfdae1db7` (1.8.8) and
`886945bfb2b978778c3a0288fd7fab09d315b25f` (1.12.2).

Official acquisitions and checksum evidence for the development run
are recorded in session artifacts, outside git, along with per-server
evidence JSON and console logs. Large JDK/server downloads are not part
of this repository.
