# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 6/10 (60.0%)
- **Function parity:** 57/245 matched (target 140) — 23.3%
- **Class/type parity:** 26/33 matched (target 34) — 78.8%
- **Combined symbol parity:** 83/278 matched (target 174) — 29.9%
- **Average inline-code cosine:** 0.49 (function body across 6 matched files)
- **Average documentation cosine:** 0.38 (doc text across 6 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 3 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. cell

- **Target:** `vt100.Cell`
- **Similarity:** 0.73
- **Dependents:** 1
- **Priority Score:** 1012202.7
- **Functions:** 20/21 matched (target 37)
- **Missing functions:** `eq`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 2. screen

- **Target:** `vt100.Screen [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 949610.0
- **Functions:** 0/93 matched (target 0)
- **Missing functions:** `new`, `set_size`, `size`, `set_scrollback`, `scrollback`, `contents`, `write_contents`, `rows`, `contents_between`, `state_formatted`, `state_diff`, `contents_formatted`, `write_contents_formatted`, `rows_formatted`, `contents_diff`, `write_contents_diff`, `rows_diff`, `input_mode_formatted`, `write_input_mode_formatted`, `input_mode_diff`, `write_input_mode_diff`, `attributes_formatted`, `write_attributes_formatted`, `cursor_position`, `cursor_state_formatted`, `write_cursor_state_formatted`, `cell`, `row_wrapped`, `alternate_screen`, `application_keypad`, `application_cursor`, `hide_cursor`, `bracketed_paste`, `mouse_protocol_mode`, `mouse_protocol_encoding`, `fgcolor`, `bgcolor`, `bold`, `dim`, `italic`, `underline`, `inverse`, `grid`, `grid_mut`, `enter_alternate_grid`, `exit_alternate_grid`, `save_cursor`, `restore_cursor`, `set_mode`, `clear_mode`, `mode`, `set_mouse_mode`, `clear_mouse_mode`, `set_mouse_encoding`, `clear_mouse_encoding`, `text`, `bs`, `tab`, `lf`, `vt`, `ff`, `cr`, `decsc`, `decrc`, `deckpam`, `deckpnm`, `ri`, `ris`, `ich`, `cuu`, `cud`, `cuf`, `cub`, `cnl`, `cpl`, `cha`, `cup`, `ed`, `decsed`, `el`, `decsel`, `il`, `dl`, `dch`, `su`, `sd`, `ech`, `vpa`, `decset`, `decrst`, `sgr`, `decstbm`, `u16_to_u8`
- **Types:** 2/3 matched (target 2)
- **Missing types:** `Screen`

### 3. grid

- **Target:** `grid.Grid [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 606210.0
- **Functions:** 0/59 matched (target 0)
- **Missing functions:** `new`, `allocate_rows`, `new_row`, `clear`, `size`, `set_size`, `pos`, `set_pos`, `save_cursor`, `restore_cursor`, `visible_rows`, `drawing_rows`, `drawing_rows_mut`, `visible_row`, `drawing_row`, `drawing_row_mut`, `current_row_mut`, `visible_cell`, `drawing_cell`, `drawing_cell_mut`, `scrollback_len`, `scrollback`, `set_scrollback`, `write_contents`, `write_contents_formatted`, `write_contents_diff`, `write_cursor_position_formatted`, `erase_all`, `erase_all_forward`, `erase_all_backward`, `erase_row`, `erase_row_forward`, `erase_row_backward`, `insert_cells`, `delete_cells`, `erase_cells`, `insert_lines`, `delete_lines`, `scroll_up`, `scroll_down`, `set_scroll_region`, `in_scroll_region`, `scroll_region_active`, `set_origin_mode`, `row_inc_clamp`, `row_inc_scroll`, `row_dec_clamp`, `row_dec_scroll`, `row_set`, `col_inc`, `col_inc_clamp`, `col_dec`, `col_tab`, `col_set`, `col_wrap`, `row_clamp_top`, `row_clamp_bottom`, `row_clamp`, `col_clamp`
- **Types:** 2/3 matched (target 2)
- **Missing types:** `Grid`

### 4. term

- **Target:** `term.Term`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 53004.4
- **Functions:** 7/10 matched (target 63)
- **Missing functions:** `new`, `default`, `extend_itoa`
- **Types:** 18/20 matched (target 21)
- **Missing types:** `MouseProtocolMode`, `MouseProtocolEncoding`

### 5. row

- **Target:** `vt100.Row`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 1801.8
- **Functions:** 17/17 matched (target 22)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 6. attrs

- **Target:** `vt100.Attrs`
- **Similarity:** 0.84
- **Dependents:** 0
- **Priority Score:** 1501.6
- **Functions:** 13/13 matched (target 18)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 6)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

