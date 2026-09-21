# Fonts for printable student cards

The student-card PDF uses these bundled fonts when a student's name contains
Sinhala or Tamil characters. The complex-script name is shaped with Java2D and
embedded as a high-resolution transparent image; all other card content and
the QR code remain PDF content. Bundling fonts keeps output independent of the
fonts installed on a server.

| File | Official source | License |
|---|---|---|
| `NotoSansSinhala-Regular.ttf` | https://notofonts.github.io/sinhala/fonts/NotoSansSinhala/hinted/ttf/NotoSansSinhala-Regular.ttf | `OFL-Sinhala.txt` |
| `NotoSansTamil-Regular.ttf` | https://notofonts.github.io/tamil/fonts/NotoSansTamil/hinted/ttf/NotoSansTamil-Regular.ttf | `OFL-Tamil.txt` |
| `NotoSans-Regular.ttf` | https://github.com/notofonts/noto-fonts/blob/main/hinted/ttf/NotoSans/NotoSans-Regular.ttf | `OFL-NotoSans.txt` |

Fonts are redistributed under the SIL Open Font License 1.1. The corresponding
license notices are included here. Do not remove them when packaging the app.

SHA-256 (for the bundled copies):

```text
NotoSansSinhala-Regular.ttf d1cf1a95103aba673f88c3b447aa519b06e86a85783d67a2efb39f0dd29b728d
NotoSansTamil-Regular.ttf   90e35c9625fd292edbd3722b133cab84b8b947da410ea1324aaad623cc6e7568
NotoSans-Regular.ttf        b85c38ecea8a7cfb39c24e395a4007474fa5a4fc864f6ee33309eb4948d232d5
```
