# SLM Voice

SLM Voice is a client-side soundboard for Minecraft. Play local MP3 and WAV clips through Simple Voice Chat.

## Compatibility

- Minecraft Java Edition 1.21.11
- Fabric Loader

This release targets Minecraft 1.21.11 on Fabric. Other Minecraft versions and loaders have not been tested.

## Features

- Browse audio by folder or search by name
- Play MP3 and WAV clips through voice chat
- Save favorites and set a shortcut for each clip
- Adjust volume and choose a start and end point
- Refresh the library while the game is open
- Download audio from supported URLs using a Cobalt API endpoint

## Requirements

Install these mods in the same Minecraft instance:

- Fabric API
- Fabric Language Kotlin
- owo-lib
- Simple Voice Chat

Mod Menu is optional. For multiplayer voice transmission, Simple Voice Chat must also be installed and configured on the server.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Add the required mods listed above to your instance.
3. Put `slmvoice-0.7.1+1.21.11.jar` in that instance's `mods` folder. The `-sources.jar` is for browsing source code, not for installing as a mod.
4. Start Minecraft and check that Simple Voice Chat is available.

## Add and play clips

1. Put `.mp3` or `.wav` files in `<instance>/config/slmvoice/`. You can create subfolders to organize them.
2. Open the soundboard with **Open SLM Voice** (default: `J`). You can change this key in Minecraft's Controls settings.
3. Select a clip and press Play, or assign it a per-clip shortcut.
4. Press **Refresh** after adding files while the game is running.
5. Use **Stop SLM Voice audio** (default: `U`) to stop playback.

The optional downloader sends the URL you enter to the configured Cobalt API endpoint. If you configure a Cobalt API token, it is stored in `config/slmvoice.json` and used for downloader requests. Do not share that config file without removing the token.

## Local files

- Audio library: `config/slmvoice/`
- General settings, favorites, shortcuts, and downloader settings: `config/slmvoice.json`
- Per-clip volume and segment settings: `config/slmvoice_tracks.json`

These files belong to your Minecraft instance and are not part of the source repository.

## Notes

SLM Voice runs on the client and does not install audio files on a server. Voice transmission depends on Simple Voice Chat being available and configured for your connection. This is a beta release; behavior can vary with audio files, other mods, and server setup.

## Source and license

The source code is available in this repository. SLM Voice is licensed under **GPL-3.0-only**; see [`LICENSE`](LICENSE) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for third-party license details.

## Credits

SLM Voice is based on KikuGie's [voicechat-soundboard](https://github.com/kikugie/voicechat-soundboard/tree/multiaddon) and has been modified and expanded. The included `kowoui` module has its own MIT license declaration; see [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
