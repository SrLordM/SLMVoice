# SLM Voice

SLM Voice is a client-side soundboard for Minecraft that plays local MP3 and WAV files through Simple Voice Chat.

## Supported setup

- Minecraft Java Edition 1.21.11
- Fabric Loader
- Client-side installation

Only this Minecraft version and Fabric setup are targeted by this release. Compatibility with other Minecraft versions, loaders, and mod combinations is not promised.

## Features

- Play local MP3 and WAV files through Simple Voice Chat
- Organize audio into folders and browse with search
- Mark favorites and assign per-audio shortcuts
- Adjust volume and audio start/end segments
- Refresh the library after adding files
- Optional URL download panel using the configured Cobalt API endpoint

## Requirements

Install these mods in addition to SLM Voice:

- Fabric API
- Fabric Language Kotlin
- owo-lib
- Simple Voice Chat

Simple Voice Chat must also be installed and configured on the multiplayer server for voice transmission there. Follow its own setup instructions. Mod Menu is optional.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install the required mods listed above.
3. Put `slmvoice-0.7.1+1.21.11.jar` in the instance's `mods` folder. Do not install the `-sources.jar` as a mod.
4. Start the game and confirm that Simple Voice Chat is available.

## Add and play audio

1. Put `.mp3` or `.wav` files in `.minecraft/config/slmvoice` (or the equivalent `config/slmvoice` folder in your launcher instance).
2. Open the soundboard with the `Open SLM Voice` keybind. Its default key is `J`; keybinds can be changed in Minecraft's Controls settings.
3. Select an audio and use Play, or configure a per-audio shortcut in the soundboard.
4. Press `Refresh` after adding files while the game is open.
5. Use the `Stop SLM Voice audio` keybind to stop playback. Its default key is `U`.

The optional downloader sends the URL entered by the user to the configured Cobalt service. A user-provided Cobalt API token, if configured, is stored in the local config file; do not share that file publicly.

## Local files

- Audio library: `config/slmvoice/`
- General settings, favorites, and shortcuts: `config/slmvoice.json`
- Per-audio volume and segment settings: `config/slmvoice_tracks.json`

These are local instance files and should not be included in bug reports or public source uploads unless intentionally sanitized.

## Limitations

- SLM Voice is client-side; it does not install audio files on a server.
- Voice transmission depends on Simple Voice Chat being installed, connected, and configured correctly.
- MP3 and WAV are the supported audio formats.
- This is a beta release. Performance and behavior can vary with audio files, other mods, and server configuration.

## Source and license

The project source is provided in this repository. The release also includes a `-sources.jar` for source browsing; it is not a complete project archive or an installable mod. SLM Voice is licensed under **GPL-3.0-only**; see [`LICENSE`](LICENSE) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
