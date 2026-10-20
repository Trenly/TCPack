# TCPack contributor instructions

## Project and scope

TCPack is a legacy Bukkit plugin modernized to provide personal-time
(`/ptime`), flight (`/fly`), compression (`/compress`), and aggregate
help (`/tcpack`) commands. Read `README.md`,
`docs/compatibility.md`, and `NOTICE` before changing behavior.

- Preserve the plugin name, data-directory identity, and entry point
  `me.tzahi.TCPack.TCPack`.
- Only `/ptime`, `/fly`, `/compress`, and `/tcpack` are registered. Do not restore `/fake`
  or published-only commands without an explicit
  request. Maintain `docs/not-modernized-commands.md` when scope changes.
- Do not add MCStats, bStats, telemetry, or runtime network tasks. The
  original reported failure came from a downloaded binary's obsolete
  `Server.getOnlinePlayers()` array signature; that Metrics source was
  not part of the source checkout.
- Prefer small executors and focused helpers over a general command
  framework. Avoid unrelated refactoring.

## Cross-version compatibility

- Produce **one plugin JAR** targeting Minecraft 1.8 through 26.3.
  Paper is primary; preserve Bukkit/Spigot compatibility where available.
  Folia is not supported.
- Production code and dependencies must support Java 8 bytecode **and
  Java 8 library APIs**. Preserve Maven's `release` setting; `source` and
  `target` alone do not enforce Java library compatibility.
- Compile the distributed artifact against the pinned legacy Spigot API
  with `provided` scope. Never bundle server API classes.
- Use public Bukkit APIs common to the target range. Avoid NMS,
  CraftBukkit internals, Paper-only/Adventure-only APIs, and version-string
  dispatch when the common API suffices.
- Keep the conventional Bukkit `plugin.yml` and its tested
  `api-version: '1.13'` unless deliberately changing compatibility.
  Legacy servers ignore this field; modern servers avoid legacy
  material translation.
- The plugin's bytecode baseline is not the server's JVM requirement.
  Use each server's supported Java version; Paper 26.3 uses Java 25.

## Command and configuration invariants

- Preserve existing permission names and operator-only defaults.
  Do not introduce wildcard permission aliases or broaden access
  incidentally. Keep `plugin.yml`, help, tests, and README consistent.
- Reject invalid arguments, denied permissions, disabled features,
  unsupported senders, and offline targets with explicit feedback and
  no state changes. Validate permission before target lookup.
- Preserve `EnablePtime`, `EnableFlight`, and `EnableCompress`, defaulting to true when
  missing. Preserve existing config files and ignore obsolete keys.
  Malformed YAML/non-boolean flags must log an explicit error and
  disable the plugin, not silently fall back to enabled behavior.
- Configuration changes require a normal restart. Do not add hot-reload
  behavior or persist new player state without a request.

### Personal time

- `/ptime` is player-only, including help.
- Fixed values are day=6000, night=18000, morning=0, dawn=23000
  (sunrise), and dusk=12000 (sunset). Use `setPlayerTime(value, false)`.
- Reset uses `resetPlayerTime`; info reports personal time and offset.
  Never change world time or another player's time.
- Preserve the **original help presentation**: blue
  `}---- PlayerTime help ----{` header, gold command names, blue short
  descriptions, and ASCII ` - ` separators. Include dawn and dusk.
  Do not replace it with verbose tick/permission annotations or a new
  color scheme. Respect the invoked label, including `tcpack:ptime`.
- Protect exact help text, colors, ordering, and namespaced labels
  with regression tests.

### Aggregate help

- `/tcpack` lists implemented command families in the original blue/gold
  TCPack help style. Preserve `tcpack.list` with operator-only defaults.
- Allow player and console senders. Additional arguments display the same
  help, as in the original; never list removed `/fake` commands.

### Flight

- Retain `t`/`toggle`, `on`, and `off`, with optional exact online-player
  targets and the existing self/`.other` permissions.
- Console can view help and act on an explicit target, not itself.
- Permit survival/adventure; reject creative/spectator unchanged.
- Enable allowance before enabling flying. Clear flying before revoking
  allowance. Toggle based on allowance, not current airborne state.
- Report redundant on/off requests without mutations.
- Do not alter speed/game mode, add reconnect/game-mode persistence, or
  revoke flight owned by other plugins during lifecycle cleanup.

### Compression

- Preserve the original blue/gold help style and operator-default
  `compress.list`, `compress.wood`, `compress.bonemeal`, and `compress.ores`.
  Commands are player-only and respect namespaced invocation labels.
- Detect available material names instead of hardcoding server versions.
  Pre-1.13 wood and bonemeal require legacy species/dye data values.
- Convert four matching planks to a base log (unstripped stem for nether
  species), never stripped logs, wood, or hyphae. Exclude bamboo, which
  has no matching base log/stem.
- Prefer nine bonemeal to one bone block if available; otherwise use
  three bonemeal to one bone. Match only bonemeal, not other dyes.
- Convert nine matching ingots/raw ores to their available storage block;
  also convert nine diamonds, redstone dust, or lapis lazuli to their blocks.
  Legacy lapis is INK_SACK data 4; never consume other dyes.
  Compress iron/gold nuggets to ingots
  first (9:1), then include those in block conversion, consuming generated
  ingots before existing ingots. Intermediate output needs no storage slots.
  Preserve nugget remainders and atomic rollback; never consume world ore blocks.
- Only process plain items in hotbar/main storage slots 0-35. Preserve
  custom metadata, armor/offhand, and remainders. Plan on clones and
  reject the whole invocation if outputs cannot fit; never drop leftovers.
- Test actual runtime ItemStacks/inventories on both sides of material
  flattening as well as newer wood/raw-ore capabilities.

## Build and repository layout

- Maven is the build/IDE source of truth. Use the checked-in wrapper:
  `.\mvnw.cmd verify` on Windows or `bash ./mvnw verify` on Unix.
- Production source/resources are in `src/main/java` and
  `src/main/resources`; tests are in `src/test/java`.
- The release filename is `target/TCPack-<version>.jar`.
  Derive the packaged plugin version from `pom.xml` resource filtering.
- For a version bump, update coupled metadata tests, artifact-check
  expectations, runtime-driver release assertions, and current
  installation examples. Do not rewrite historical artifact hashes or
  claim previous runtime results apply to a newly built JAR.
- The separate runtime driver's own version is independent of the
  production release; update its filename references if it is bumped.
- Keep wrapper/tool/dependency versions and integrity checks pinned.
  Preserve reproducible-build settings and wrapper executable permission.
- Do not commit generated `target` directories at any depth, compiled
  classes, `.DS_Store`, downloaded JDK/server distributions, or obsolete
  Eclipse files/legacy `TCPack` source/output trees.

## Verification and environment setup

- Use JUnit Jupiter and the existing focused proxy/stub tests. Cover
  command values, permissions, flags, sender/target errors, game modes,
  mutation ordering, exact help output, and descriptor metadata.
- Run the smallest relevant tests, then `verify` when packaging or
  release metadata changes. CI covers Java 8 and 25.
- `tools/Test-Artifact.ps1` verifies production class contents, Java 8
  class-file version, release version, forbidden references, and exact
  LICENSE/NOTICE packaging.
- `tools/Check-Api.ps1 -ApiVersion <version>` compiles against alternate
  APIs in isolation without replacing the distributed legacy-built JAR.
- Build the separate test driver before invoking the runtime runner:
  `.\mvnw.cmd -f tools\runtime-driver\pom.xml package`.
- `tools/Test-Compatibility.ps1` uses an explicit plugin JAR, server JAR,
  Java executable, fresh isolated directory, port, and `-AcceptEula`.
  Never install its test driver into a live server.
- When tooling is missing, locate or provision the required JDKs,
  wrapper/bootstrap tools, and server JARs from official sources.
  Verify published checksums, pin exact builds, record provenance,
  and keep downloads outside git. Do not hardcode session-specific paths.
- Server tests require authorized EULA acceptance, loopback-only binding,
  separate worlds/directories, unique ports, bounded waits, and graceful
  shutdown. Stop only processes created for the test.
- Legacy Paperclip 1.8.8/1.12.2 uses retired download URLs. Use the
  documented official Mojang vanilla-JAR cache-seeding parameters,
  verifying Mojang SHA-1 and Paperclip's embedded SHA-256; do not patch
  or substitute untrusted server JARs.
- Java proxies cannot implement the legacy Player interface's conflicting
  `getHealth` return descriptors. Preserve the runtime driver's
  descriptor-aware `PlayerStub`; it is test-only code, not production.
- Distinguish unit tests, API compilation, actual plugin loading,
  in-server stub/proxy assertions, and connected-player/client tests.
  Never describe one as another or claim untested versions passed.
- Record the tested artifact SHA-256, exact server/API/JVM versions,
  results, shutdown evidence, and limitations in `docs/compatibility.md`.
  The initial eight-server matrix starts at 1.8.8, not exact 1.8;
  connected-player/visual coverage is not established.

## License, attribution, and documentation

- Preserve **CC BY-SA 3.0 Unported** for TCPack and its adaptation.
  Keep the official legal text in `LICENSE` unmodified.
- Preserve original credits to **tzahi12345 (Tzahi) and TacoID** and
  separately credit **Trenly's compatibility refactor**. Keep original
  attribution at least as prominent; do not imply endorsement.
- Preserve `NOTICE` provenance, adaptation/change identification, and
  warranty/license notices. Keep appropriate plugin author metadata.
- Package LICENSE and NOTICE unchanged as `META-INF/LICENSE` and
  `META-INF/NOTICE`. Retain Maven license metadata and README links.
- Do not impose additional restrictions or relicense third-party tools;
  preserve their separate license/notice obligations.
- Update directly related documentation for behavioral, compatibility,
  build, and version changes. Use GitHub `> [!NOTE]` admonitions.

## Git workflow

- Preserve unrelated local work. Use feature branches for ordinary code
  changes unless the maintainer explicitly requests otherwise.
- When asked to update these instructions, commit and push the update
  directly to `master`.
- Do not force-push, delete branches, amend, or rewrite history without
  explicit permission.
- Use imperative commit subjects and a Copilot co-author trailer for
  Copilot-assisted commits. In attended mode commit normally with
  signing enabled; use `--no-gpg-sign` only in explicitly unattended mode.
- Verify the actual diff before committing and verify clean status and
  matching remote HEAD after pushing.
