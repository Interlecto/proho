# Visual identity

Source assets for ProHo's brand, shared by the landing page, the web app,
and (once built) the Android and iOS apps. Each client copies or derives
what it needs from here rather than redefining its own colors and marks.

## Assets

| File                      | What it is                                                              |
| -------------------------- | ------------------------------------------------------------------------ |
| `proho-wordmark.svg`       | The "Proho" wordmark, white fill, transparent background. For dark/photo backgrounds. |
| `proho-wordmark-ink.svg`   | Same wordmark, ink-navy fill. For light backgrounds.                    |
| `proho-monogram.svg`       | Square navy tile with a white "P", used as the source for favicons and app icons. |
| `hero-skyline.jpg`         | A dense residential/commercial high-rise skyline at night — the mood image behind the wordmark on marketing surfaces. |

All three SVGs are plain vector paths (no filters or embedded raster data),
so they scale cleanly to any size and can be recolored by editing the
`fill` attribute.

## Provenance

`proho-wordmark.svg` and `proho-monogram.svg` are extracted from
`proho.svg` (an Inkscape file provided by the project owner): the wordmark
is the vector `<path>` Inkscape had drawn on top of an embedded photo, lifted
out on its own. `hero-skyline.jpg` is that embedded photo, re-exported from
the same file (re-compressed for the web; the original embeds AI-generation
provenance metadata (C2PA) from Google's media services, which was stripped
on export since it isn't relevant once the image is just a brand asset).

## Palette

Sampled from the skyline photo's night sky and warm window/traffic light
tones, then adjusted for contrast:

| Token      | Hex       | Use                                                              |
| ---------- | --------- | ------------------------------------------------------------------ |
| Ink (navy) | `#0E1B2E` | Primary dark color — backgrounds, header bars, text on light surfaces |
| Gold (light-mode accent) | `#9C6418` | Links/buttons on light backgrounds (4.9:1 contrast on white) |
| Gold (dark-mode accent)  | `#F0B36B` | Links/buttons on navy or dark backgrounds (9.4:1 contrast on `#0E1B2E`) |
| White      | `#FFFFFF` | Wordmark and text on navy/photo backgrounds                     |

This supersedes the ad hoc green used earlier in `landing/` and the
`#428BCA` blue noted as "from the original mockups" in the Android shell —
both predate this asset and should be treated as provisional placeholders.

## Regenerating favicons and app icons

`proho-monogram.svg` is the source. Each site/app keeps its own rasterized
copies (favicon.ico, apple-touch icon, etc.) alongside its other static
assets rather than referencing this directory at runtime, matching the
pattern used in [ilecto](https://github.com/Interlecto/ilecto). Example,
using ImageMagick:

```bash
magick proho-monogram.svg -resize 32x32 icon-32.png
magick proho-monogram.svg -resize 180x180 favicon-180.png
magick icon-16.png icon-32.png favicon.ico
```
