# Commands not modernized in TCPack 2.0

TCPack 2.0 retains only `/ptime` and `/fly`. The commands below are
**removed/not provided**, not deprecated aliases, and have no active
configuration or permission declarations in 2.0.

Original TCPack is by tzahi12345 (Tzahi) and TacoID; this reduced
compatibility adaptation is by Trenly. See [NOTICE](../NOTICE) and
[LICENSE](../LICENSE).

## Present in the legacy source

| Command | Legacy purpose |
| --- | --- |
| `/compress` | Compression help |
| `/compress wood` | Convert planks into logs |
| `/compress bonemeal` | Convert bonemeal into bones |
| `/compress ores` | Convert diamond, gold, and iron into blocks |
| `/fake` | Fake-command help |
| `/fake join` | Broadcast a fake self join |
| `/fake join <username>` | Broadcast a fake join for another name |
| `/fake leave` | Broadcast a fake self departure |
| `/fake leave <username>` | Broadcast a fake departure for another name |
| `/fake op <username>` | Send a fake operator-grant message |
| `/fake deop <username>` | Send a fake operator-removal message |
| `/fake ban <username>` | Kick without actually banning |
| `/fake gban <username> [reason...]` | Kick and broadcast a fake ban |
| `/fake kill <username> <reason...>` | Broadcast a fake death |
| `/fake say <username> <message...>` | Make the target chat or execute a command |
| `/fake fix` | Broken debug-only path referencing an undefined config key |
| `/tcpack` | Aggregate help for command families |

The old `EnableFake`, `EnableCompress`, and fake-ban message settings
no longer control any behavior. Existing files may retain those keys;
2.0 ignores them.

## Advertised, but absent from this source checkout

The [published project feature list](https://dev.bukkit.org/projects/tcpack)
additionally lists these commands. They are not restored or implemented:

| Command | Advertised purpose |
| --- | --- |
| `/fake restart` | Show a fake server-restart message |
| `/fake restart <username>` | Show a fake restart message to a target |
| `/fake give <username> <item...>` | Show a fake item-give message |

The source checkout identified itself as v0.9, while the reported deployed
binary was v1.0. This document distinguishes source evidence from published
descriptions; it does not claim to enumerate unseen binary-only behavior.
