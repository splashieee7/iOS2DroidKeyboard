# iPhone keyboard: measured metrics

Source: `iphone-letters-dark.png`, 1125×2436 px, so a 375 pt wide screen at 3x.
Measured from pixels; values are in pt (px ÷ 3). **% of W** = share of screen width, which is how to scale it to Android screens of other widths.

## Letter keys
| Thing | px | pt | % of W |
|---|---|---|---|
| Letter key width | 92.5 | 30.8 | 8.2% |
| Horizontal gap between keys | 18–19 | ~6.2 | 1.65% |
| Key pitch (key + gap) | 110.3 | 36.8 | 9.8% |
| Left/right edge margin | 20 | 6.7 | 1.8% |
| Key height | 129 | 43 | |
| Vertical gap between rows | 33 | 11 | |
| Row pitch | 162 | 54 | |
| Corner radius | ~21–24 | ~7–8 | |

## Row layout
- Row 1 (QWERTYUIOP): 10 keys, edge to edge with 6.7 pt margins.
- Row 2 (ASDFGHJKL): 9 keys, indented exactly **half a key pitch** (18.4 pt) on each side.
- Row 3: Shift and Delete are **41.7 pt** wide, flush to the edge margins. Z–M line up with row 2's S–K columns. Gap between Shift and Z (and M and Delete) is ~13.7 pt, wider than normal.
- Row 4: `123` 40 pt · emoji 40 pt · space 178 pt · return 86 pt, with normal ~6.3 pt gaps.
- Below row 4: a separate strip with the globe (left) and mic (right), outside the key grid.
- Suggestion bar above row 1: 3 equal slots with thin dividers.

## Colors (dark mode, as captured)
- Keys: `#3F3F3F` (letter and function keys are the same color in this iOS version)
- Keyboard background: ~`#1A1A1A`
- Key labels: white. Letters are lowercase on the keycaps when shift is off.
- No visible drop shadow under keys in dark mode.

## Touch targets
Hit areas should split the gaps down the middle. That makes each letter key's touch target the full 36.8 pt pitch wide and 54 pt row pitch tall, so there are no dead zones.
