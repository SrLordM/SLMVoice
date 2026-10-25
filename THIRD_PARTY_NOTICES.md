# Third-party components and license notices

This file records the components verified in the distributed SLM Voice 0.7.1 JAR. The exact nested JAR names are under `META-INF/jars/`; license texts listed below are included under `META-INF/licenses/` in both the mod JAR and the sources JAR. SLM Voice itself is licensed under GPL-3.0-only; see [`LICENSE`](LICENSE).

## Upstream soundboard code

The main SLM Voice soundboard is based on KikuGie's [`voicechat-soundboard` project, `multiaddon` branch](https://github.com/kikugie/voicechat-soundboard/tree/multiaddon), and has been modified and expanded for SLM Voice. The upstream root project declares GPLv3. This distribution preserves that GPLv3 basis under SLM Voice's GPL-3.0-only license; this credit refers to the original soundboard code, not later SLM Voice changes.

## Embedded components

| Component | Embedded artifact/version | License evidence and attribution |
| --- | --- | --- |
| JLayer | `javazoom:jlayer:1.0.1` | LGPL-2.1-only. Original project attribution: JavaZOOM (1999-2008), as stated by the upstream project; Maven Central's POM identifies the GNU Lesser General Public License. [Source](https://github.com/umjammer/jlayer), [license](https://github.com/umjammer/jlayer/blob/master/LICENSE.txt), [matching source archive](https://repo.maven.apache.org/maven2/javazoom/jlayer/1.0.1/jlayer-1.0.1-sources.jar). |
| MP3SPI | `com.googlecode.soundlibs:mp3spi:1.9.5.4` | LGPL-2.1-only. Original project attribution: JavaZOOM; the upstream project states that it is LGPL-licensed and provides `LICENSE.txt`. [Source](https://github.com/umjammer/mp3spi), [license](https://github.com/umjammer/mp3spi/blob/master/LICENSE.txt), [matching source archive](https://repo.maven.apache.org/maven2/com/googlecode/soundlibs/mp3spi/1.9.5.4/mp3spi-1.9.5.4-sources.jar). |
| Tritonus Share | `com.googlecode.soundlibs:tritonus-share:0.3.7.4` | LGPL-2.1-only according to the version-specific Maven Central record. Tritonus's own site describes the license as the GNU Library/Lesser General Public License but does not state a version; the version-specific artifact record supplies 2.1. Original author: Florian Bomers; the artifact POM credits Patrik Duditš as packager. [Version-specific artifact record](https://central.sonatype.com/artifact/com.googlecode.soundlibs/tritonus-share/0.3.7.4), [upstream project](https://www.tritonus.org/), [matching source archive](https://repo.maven.apache.org/maven2/com/googlecode/soundlibs/tritonus-share/0.3.7.4/tritonus-share-0.3.7.4-sources.jar). |
| OkHttp | `com.squareup.okhttp3:okhttp:4.12.0` | Apache-2.0, as identified by the versioned Maven Central POM; developer metadata credits Square, Inc. [Version-specific artifact record](https://central.sonatype.com/artifact/com.squareup.okhttp3/okhttp/4.12.0), [source and license](https://github.com/square/okhttp). |
| Okio (JVM variant) | `com.squareup.okio:okio-jvm:3.9.1` | Apache-2.0, as identified by the versioned Maven Central POM; developer metadata credits Square, Inc. The `okio` multiplatform coordinate resolves to this JVM artifact in the distributed JAR. [Version-specific artifact record](https://central.sonatype.com/artifact/com.squareup.okio/okio-jvm/3.9.1), [source and license](https://github.com/square/okio). |
| owo-sentinel | `io.wispforest:owo-sentinel:0.13.0-alpha.16+1.21.11` | MIT, as declared by the embedded Fabric metadata. Its author field is `glisco`; the upstream owo-lib repository supplies the MIT license text, reproduced in `LICENSES/MIT.txt`. [Source](https://github.com/wisp-forest/owo-lib), [upstream license](https://github.com/wisp-forest/owo-lib/blob/master/LICENSE). |
| Kotlin OwO UI (`kowoui`) | `kowoui:0.3.0+1.21.11` | The `kowoui` module's Fabric metadata declares MIT and credits KikuGie; this declaration is present in all three available revisions of that metadata in the upstream history. The module comes from [KikuGie's `voicechat-soundboard` repository](https://github.com/kikugie/voicechat-soundboard/tree/multiaddon/kowoui), whose root license and root Fabric metadata declare GPLv3. No separate `kowoui` license file or copyright year was found, so no year is asserted here. This distribution preserves both the module's MIT notice and the project's GPLv3 license; MIT is compatible with GPLv3 for combined distribution. The local module has changes relative to upstream, so this notice does not imply that KikuGie authored those changes. |

## Additional bundled notice

The OkHttp JAR contains `okhttp3/internal/publicsuffix/NOTICE` for its bundled Public Suffix List data. That notice states:

> Note that publicsuffixes.gz is compiled from The Public Suffix List: https://publicsuffix.org/list/public_suffix_list.dat
>
> It is subject to the terms of Mozilla Public License, v. 2.0: https://mozilla.org/MPL/2.0/

The notice does not identify a snapshot/version of the list. The MPL-2.0 text is included in `LICENSES/MPL-2.0.txt`.

## License texts

- `LICENSES/LGPL-2.1.txt`: complete GNU Lesser General Public License 2.1 text; applies to JLayer, MP3SPI, and Tritonus Share.
- `LICENSES/Apache-2.0.txt`: complete Apache License 2.0 text; applies to OkHttp and Okio.
- `LICENSES/MIT.txt`: MIT text and the available upstream attribution data for owo-sentinel and kowoui.
- `LICENSES/MPL-2.0.txt`: complete Mozilla Public License 2.0 text for the Public Suffix List notice bundled in OkHttp.
- `LICENSE`: complete GNU General Public License version 3 text for SLM Voice and the GPLv3-based upstream soundboard code described above. The `kowoui` module separately declares MIT, as noted in the component table.

The three LGPL source archives linked above were checked at the matching Maven Central coordinates and returned HTTP 200. The nested dependency JARs are kept as separate JAR-in-JAR entries; this project does not patch their contents. This notice and the included texts provide license information, but are not legal advice or a determination of compliance for a particular distribution method.
