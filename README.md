# TCPack 2.1.0

**Original TCPack by tzahi12345 (Tzahi) and TacoID.**
**TCPack 2.0 compatibility refactor by Trenly.**

A small Bukkit plugin for personal time, flight, and item compression.
This adaptation provides `/ptime`, `/fly`, `/compress`, and `/tcpack` help, and contains no
MCStats, bStats, or other telemetry. It is based on the
[original TCPack](https://github.com/Tzahi12345/TCPack); no endorsement by
the original authors is implied.

## Compatibility and installation

One `TCPack-2.1.0.jar` targets Minecraft **1.8 through 26.3**, with Paper as the
primary implementation and Bukkit/Spigot compatibility where available.
Folia is not supported. The plugin uses Java 8 bytecode; **run each server on
the Java version required by that server**, not necessarily Java 8.
Paper 26.3 requires Java 25.

> [!NOTE]
> The target range is not a claim that every server release has been tested.
> See [compatibility evidence](docs/compatibility.md) for checks actually
> performed, exact builds, and any unresolved runtime coverage.

1. Stop the server normally and back up its plugin configuration.
2. Replace the old TCPack JAR in `plugins` with `TCPack-2.1.0.jar`. Do not keep
   both versions installed.
3. Start the server normally. Do not use plugin hot-reload tools.

Existing `plugins/TCPack/config.yml` files remain usable. Only these flags
are read; missing flags default to `true`:

```yaml
EnablePtime: true
EnableFlight: true
EnableCompress: true
```

Obsolete configuration keys are ignored, not removed from your existing
file. Malformed YAML or non-boolean flags log an explicit configuration
error and disable TCPack rather than silently enabling a feature.

## TCPack help

`/tcpack` lists `/fly`, `/compress`, and `/ptime` in the original blue/gold
help style. It requires `tcpack.list` (operator-only by default), works
for players and console, and does not execute the listed commands.
As in the original, additional arguments still display this help.

## Personal time

All `/ptime` commands require a player sender. They change only that
player's time, never world time or another player's time.
Help retains the original blue PlayerTime header, gold command names,
and short blue descriptions, including entries for dawn and dusk.

| Command | Effect | Permission |
| --- | --- | --- |
| `/ptime` | Show help | `ptime.list` |
| `/ptime day` | Fixed time 6000 | `ptime.day` |
| `/ptime night` | Fixed time 18000 | `ptime.night` |
| `/ptime morning` | Fixed time 0 | `ptime.morning` |
| `/ptime dawn` | Fixed time 23000 (sunrise) | `ptime.dawn` |
| `/ptime dusk` | Fixed time 12000 (sunset) | `ptime.dusk` |
| `/ptime reset` | Resume normal server-relative time | `ptime.reset` |
| `/ptime info` | Show personal time and offset | `ptime.info` |

**Migration:** legacy `dawn` set time to 12000, which is sunset. In 2.0 it
means sunrise; use the new `dusk` command for the previous value.

## Flight

| Command | Effect | Permission |
| --- | --- | --- |
| `/fly` | Show help | `fly.list` |
| `/fly t` or `/fly toggle` | Toggle own flight | `fly.toggle` |
| `/fly on` | Enable own flight | `fly.enable` |
| `/fly off` | Disable own flight | `fly.disable` |
| `/fly t <player>` or `/fly toggle <player>` | Toggle target flight | `fly.toggle.other` |
| `/fly on <player>` | Enable target flight | `fly.enable.other` |
| `/fly off <player>` | Disable target flight | `fly.disable.other` |

Console may view `/fly` help and use actions with an explicit online
player. Self-actions require a player. Targets must match an online
player's exact name (not a partial name).

Flight changes are allowed in survival and adventure; creative and
spectator are rejected without state changes. Enabling flight starts
flying; disabling clears flying before revoking flight allowance.
Redundant on/off requests report the existing state. No flight speed,
game mode, reconnect persistence, or game-mode restoration is added.

Every listed permission defaults to **operator-only**. Grant the exact
nodes with your permissions plugin to allow other players to use them.
There are no new wildcard permission aliases. If another plugin owns a
short command, use `/tcpack:ptime` or `/tcpack:fly`.

## Compression

Compression returns in **2.1.0**, with the original blue/gold help style.
All commands are player-only. Missing `EnableCompress` defaults to `true`;
an existing `EnableCompress: false` remains respected.

| Command | Effect | Permission |
| --- | --- | --- |
| `/compress` | Show compression help | `compress.list` |
| `/compress wood` | Convert each matching 4 planks into 1 base log/stem | `compress.wood` |
| `/compress bonemeal` | Convert 9 bonemeal into a bone block, or 3 into a bone on servers without bone blocks | `compress.bonemeal` |
| `/compress ores` | Convert iron/gold nuggets into ingots, then matching ingots, raw ores, diamonds, redstone, or lapis lazuli into blocks | `compress.ores` |

All four permissions default to operator-only, as with time and flight.
Use `/tcpack:compress` if another plugin claims the short command.

Materials are detected from the running server, not version strings:

- Wood includes every available plank species with a corresponding base
  log, including newer species such as mangrove, cherry, and pale oak.
  Crimson/warped planks produce unstripped stems. Output is never stripped
  logs/stems or wood/hyphae blocks. Bamboo is excluded because it has no
  corresponding log/stem. Legacy data values preserve all six wood species.
- Bonemeal prefers bone blocks whenever that material exists, including
  pre-1.13 servers with legacy dye data. It falls back to bones only when
  bone blocks are unavailable. Other dyes are untouched.
- Ores includes iron/gold/copper/netherite ingots and raw iron/gold/copper
  where available, plus diamonds, redstone dust, and lapis lazuli. Any runtime
  `*_INGOT` or `RAW_*` item must have its matching storage block to qualify.
  Every 9 iron/gold nuggets become 1 ingot where supported; every 9
  matching ingots, raw ores, diamonds, redstone dust, or lapis lazuli become
  1 corresponding block. Legacy lapis uses dye data 4; other dyes are untouched. Nugget-derived
  ingots are used before existing ingots, in the same invocation:
  8 ingots + 9 nuggets produce 1 block, while 8 ingots + 81 nuggets
  produce 1 block and leave the 8 original ingots. Fewer than 9 nuggets
  remain nuggets. Intermediate ingots do not need inventory space.
  World ore blocks, coal, and emeralds are not
  compressed by this command.

Each invocation processes all eligible plain items in the main inventory
and hotbar, combining matching stacks and preserving remainders. Armor,
offhand, and items with custom metadata (names, lore, enchantments, or
other stored custom properties) are untouched. Conversion never drops
items: if the full output cannot fit, the entire invocation is rejected
without modifying any inventory slots. Freed input slots and existing
plain output stacks count as available space.

## Removed commands and telemetry

`/fake` is not registered. The
[not-modernized command inventory](docs/not-modernized-commands.md)
includes every removed source command and the published-only commands.

The old downloaded v1.0 JAR's Metrics task invokes an obsolete
array-returning `Server.getOnlinePlayers()` method. The source checkout
predates that binary; 2.0 is a clean build of the retained features, not a
patch to the downloaded JAR. No legacy compiled classes or telemetry
libraries are packaged.

## Building

Use a JDK supporting Java 8 API-targeted compilation and the Maven Wrapper:

```powershell
.\mvnw.cmd verify
```

On Linux/macOS:

```sh
bash ./mvnw verify
```

The output is `target/TCPack-2.1.0.jar`. The server API is provided by the
server, not bundled. Build/test tools require network access on first use.
Maven is the source of truth for IDE imports; the obsolete Eclipse project,
legacy `TCPack` source/output tree, and checked-in compiled classes have
been removed. Maven `target` directories at every level are ignored.
See [compatibility and environment reproduction](docs/compatibility.md)
for precise JDKs, commands, and runtime-test prerequisites.

## License and attribution

**TCPack by tzahi12345 (Tzahi) and TacoID** and **the 2.0 compatibility
refactor by Trenly** are licensed under
[Creative Commons Attribution-ShareAlike 3.0 Unported](https://creativecommons.org/licenses/by-sa/3.0/).
See [LICENSE](LICENSE) for the full legal terms and [NOTICE](NOTICE) for
original-work provenance, attribution, and adaptation details. Both are
also included under `META-INF` in the plugin JAR.

The license permits sharing and adaptation subject to its terms,
including attribution and ShareAlike, without additional restrictions.
No warranties are given. Third-party tooling retains its own licensing.
The published license statement was supplied by the maintainer because
the [CurseForge license page](https://www.curseforge.com/minecraft/bukkit-plugins/tcpack/license)
returned HTTP 403 during the refactor.
