# CooperativeEndAccess

A very, very opinionated plugin for people who don't trust their friends to not beat the Ender Dragon all on their own and instead forces them to all be present to open the End Portal.

Targets Paper servers on Minecraft 1.21.11. Expected to be used for small (4 to 20), friend-group survival multiplayer (SMP) servers.

## Features

- Limit the amount of Eyes of Ender that players can place in an End Portal frame.
- Allow players to remove Eyes of Ender they placed from an End Portal frame.
- Gate access to the End behind a soft gameplay check instead of a fixed `allow-end: false` configuration.
- Prevent the End Portal from forming unless all players are online and in the vicinity of the stronghold End Portal frame.
- That's it.

### Social Affordances

Thanks to this plugin, your SMP experience can be improved by facilitating the following situations:

- Allow four (configurable based on number of eyes per player) of your friends beat the Ender Dragon behind your back instead of just one.
- Allow yourself to pretend that there is a gameplay-based reason for restricting access to the End instead of an arbitrary rule set by the server administrator.
- Allow yourself to pretend that you're still "just another player" despite having operator permissions.
- Allow your friends to continue pretending that reaching the End is the reason that your group's SMPs keep dying.
- Allow you to naturally open access to the End without the embarrassing moment where you have to communicate to them that you noticed that the SMP was dying and decided to finally open the End to get one last hurrah out of everyone.

## Questions

These aren't frequent because I just recently made this plugin and no one has asked me any questions, but here's some pre-emptive answers, written like this because people receive information asked from an "I" perspective better.

### Who is plugin for?

Server administrators running a small, Paper-based Minecraft 1.21.11 survival multiplayer server (SMP) who are shy / lack enough social currency to convince / do not trust their friends not to beat the dragon solo.

If you:

- Have social anxiety.
- Think quietly to yourself, *"I don't trust my friends not to speedrun the End and kill the dragon without everyone"*.
- Are conflict-avoidant in general

**This plugin is built for you and you specifically!**

### How do I use this plugin?

1. Download the `.jar` for this plugin.
2. Place it in your server's `plugins/` directory.
3. Run the server.

If you have already started your server, **this plugin will not handle existing End Portals or filled frames**. You will need to handle these yourself.

If you have already defeated the dragon, **this plugin will act as if it hasn't until the dragon is defeated again** OR you edit `data.yml` (created in this plugin's data directory) to change `dragonDefeated` to `true`. But if you're doing that, the plugin is functionally dead-weight.

### How does opening the End work in this plugin?

**TL;DR:** Each person has a limited number of Eyes of Ender that they can contribute. Each player who contributes to a portal must be present at the portal for it to open.

**Longer explanation:**

The plugin tracks who placed what Eye into what stronghold End Portal frame. Each stronghold portal has a configurable (`max_eyes_per_player`) amount of eyes that a particular player can contribute to the frame. The limit is per frame, not global, so players may visit a different stronghold and place more eyes. Players may also remove an eye that they placed by right-clicking on the frame with an empty main hand.

Once a player has placed an eye into a frame, they become **committed**. To open a portal, all twelve frames in the End Portal frame must be filled with eyes and all committed players must be nearby (configurable: `activation_radius`).

Once all players are nearby, that specific End Portal will open in an **unstable** state. If a committed player leaves the area, the Portal will collapse (fancy word for... return to being air blocks).

Once a single person enters the End, that specific End Portal will be **stabilized**. It will remain open as long as at least one player committed to that portal remains alive in the End dimension.

If all committed players die without killing the dragon, then that End Portal will return to an unstable state, or collapse if not all committed players are present.

If the dragon is defeated, all End Portals become stabilized and vanilla End Portal generation logic returns.

### What should I not expect from this plugin?

- This plugin does not handle weirdly-shaped End Portal frame structures. It expects you to be playing with a standard End Portal structure (5x5, all frames inward).
- This plugin does not handle existing Eyes of Ender placed in the frames (player-placed before the plugin was added or naturally-spawned ones). If you want to handle naturally-spawned Eyes of Ender, mess with your world configuration.
- This plugin does not handle existing, open End Portals. Break them yourself.
- This plugin does not handle having killed the dragon before adding the plugin.
  - If you would like to replicate the functionality of this plugin after killing the dragon, I recommend using this handy little command:
    `find ./plugins -maxdepth 1 -type f -name 'CooperativeEndAccess-*.jar' -delete`
- This plugin does not guarantee that your SMPs will survive more than one week.
- This plugin does not guarantee that your friends won't be upset if you suddenly spring this plugin on them without telling them.
- This plugin does not guarantee that opening the End cooperatively will actually be any fun.

### Help! I deleted my `config.yml`!

That's not a question, but here is the default:

```yaml
# Maximum eyes one player can contribute to a single portal structure
# Automatically clamps to range between 0 and 12 (though why would you pick 0??)
max_eyes_per_player: 3

# Distance semantics:
# - Distances below 0 (e.g. -1) are treated as unlimited range, ignoring dimension.
#   In other words, presence in server is enough to trigger the effects.
# - All distances are 3D Euclidean distance from the portal center.

# Maximum distance (in blocks) committed players may be from the portal for it to remain open
activation_radius: 16.0
# Maximum distance (in blocks) a committed player may be from the portal to receive action bar updates
action_bar_radius: 60.0

# For sound keys, see [Paper's documentation](https://docs.papermc.io/paper/dev/command-api/arguments/registry/#sound)
# or [visit the Minecraft Wiki](https://minecraft.wiki/w/Sounds.json#Sound_events).
# For types of sources, use the "Technical name" on the [Minecraft Wiki's Sound page](https://minecraft.wiki/w/Sound#Categories).
# Volume is clamped to 0.0f at minimum and pitch is clamped to [0.5f, 2.0f] (which are limitations imposed by vanilla Java Edition).
# See the [Minecraft Wiki's /playsound page](https://minecraft.wiki/w/Commands/playsound#Arguments) for more details.
sounds:
  # Sound played when the End Portal is created
  portal_open:
    key: "minecraft:block.end_portal.spawn"
    source: "block"
    volume: 1.0
    pitch: 1.0
  # Sound played when the End Portal collapses
  portal_close:
    key: "minecraft:block.beacon.deactivate"
    source: "block"
    volume: 1.0
    pitch: 0.5
  # Sound played when a player enters the activation_radius
  player_enter:
    key: "minecraft:block.amethyst_block.chime"
    source: "player"
    volume: 1.0
    pitch: 1.0
  # Sound played when a player leaves the activation_radius
  player_leave:
    key: "minecraft:block.amethyst_block.break"
    source: "player"
    volume: 1.0
    pitch: 0.5

# Uses MiniMessage format. See [Paper's documentation](https://docs.papermc.io/adventure/minimessage/format/)
messages:
  # Personal chat message error when player tries to place more eyes than max_eyes_per_player
  max_eyes_error: "<red>You have already contributed your share to this portal!"
  # Personal chat message warning when player removes an eye
  rescind_warning: "<yellow>You took your eye back. The portal state may collapse."
  # Personal chat message warning when player tries to remove another player's eye
  # Placeholder: <name> - The player who owns the eye (e.g. "Megumin").
  non_owner_rescind_warning: "<red>This eye was placed by <name>. You cannot remove it."
  # Personal chat message notice when player places an eye
  committed_notice: "<green>You are now committed to this portal."
  # Server-broadcasted chat notice once the dragon has been defeated for the first time.
  dragon_defeat_notice: "<gold>The Dragon has fallen. End Portals are now stabilized permanently!"
  # Action bar text shown while a committed player is in the End
  # Placeholder: <name> - A committed player present in the End (e.g. "Sumi"), chosen implicitly by eye insertion order.
  stabilized_action_bar: "<bold><light_purple>✔ Stabilized by <name>"
  # Action bar shown when a portal is open
  active_action_bar: "<bold><green>✔ Portal Active"
  # Action bar shown when a portal is awaiting all committed players to be present before opening
  # Placeholder: <names> - comma-delimited list of committed player names missing (e.g. "Notch, Elaina, Mizuhara")
  waiting_action_bar: "<gold>Waiting for: <white><names>"
```

You can also find this file in [`./src/main/resources/config.yml`](https://raw.githubusercontent.com/ganyuke/CooperativeEndAccess/refs/heads/main/src/main/resources/config.yml).

### How do I tell you that your plugin is broken?

Report issues with my spaghetti code through the [issue tracker](https://github.com/ganyuke/CooperativeEndAccess/issues) on the [plugin's GitHub repository](https://github.com/ganyuke/CooperativeEndAccess/).

### How do I build this plugin myself?

1. Clone this repository: `git clone https://github.com/ganyuke/CooperativeEndAccess.git`
2. Change directory: `cd CooperativeEndAccess`
3. Build the `.jar`: `./gradlew build`
4. Find the built `.jar` in `build/libs/`

### How do I submit features for this plugin?

If you wrote the code for it, I'll happily accept a pull request for it if I feel like it doesn't bloat the scope of this plugin.

By submitting a contribution to this repository, you agree that your contribution is licensed under the same license as this repository, as published in the [`LICENSE`](https://raw.githubusercontent.com/ganyuke/CooperativeEndAccess/refs/heads/main/LICENSE) file.

## License

Unless otherwise noted, all source code in this repository is licensed under the **Mozilla Public License 2.0** (SPDX: **MPL-2.0**). Please view the [`LICENSE`](https://raw.githubusercontent.com/ganyuke/CooperativeEndAccess/refs/heads/main/LICENSE) file for the terms you are afforded under the MPL-2.0.

Below is the WTFPL. This repository is NOT licensed under the WTFPL because I believe that people should actually contribute back to software they use, but I find the WTFPL amusing and wanted to show it to you. Don't actually use this license everyone. Use [0BSD](https://en.wikipedia.org/wiki/0BSD) or something.

```
DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
Version 2, December 2004

Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>

Everyone is permitted to copy and distribute verbatim or modified
copies of this license document, and changing it is allowed as long
as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

0. You just DO WHAT THE FUCK YOU WANT TO.
```
