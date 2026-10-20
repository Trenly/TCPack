# Commands not modernized in TCPack

TCPack 2.1.0 provides `/ptime`, `/fly`, `/compress`, and `/tcpack` help. The commands below are
**removed/not provided**, not deprecated aliases, and have no active
configuration or permission declarations.

Original TCPack is by tzahi12345 (Tzahi) and TacoID; this reduced
compatibility adaptation is by Trenly. See [NOTICE](../NOTICE) and
[LICENSE](../LICENSE).

## Present in the legacy source

| Command | Legacy purpose |
| --- | --- |
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

The old `EnableFake` and fake-ban message settings no longer control any
behavior. Existing files may retain those keys; the plugin ignores them.

Compression was removed in 2.0 and restored in 2.1.0 with runtime-aware
wood, bonemeal, and ore conversions. `EnableCompress` and the original
`compress.*` permission nodes are active again. See [README](../README.md)
for current conversion ratios, inventory safeguards, and supported items.
The base `/tcpack` aggregate-help command is also restored in 2.1.0,
listing the implemented command families instead of the removed `/fake`.

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
