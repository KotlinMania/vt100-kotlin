# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 9/10 (90.0%)
- **Function parity:** 245/245 matched (target 402) — 100.0%
- **Class/type parity:** 33/33 matched (target 47) — 100.0%
- **Combined symbol parity:** 278/278 matched (target 449) — 100.0%
- **Average inline-code cosine:** 0.79 (function body across 9 matched files)
- **Average documentation cosine:** 0.36 (doc text across 9 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 0 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. cell

- **Target:** `vt100.Cell`
- **Similarity:** 0.76
- **Dependents:** 1
- **Priority Score:** 1002202.4
- **Functions:** 21/21 matched (target 38)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 2. callbacks

- **Target:** `vt100.Callbacks`
- **Similarity:** 0.69
- **Dependents:** 1
- **Priority Score:** 1001303.1
- **Functions:** 12/12 matched (target 23)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 4)
- **Missing types:** _none_

### 3. parser

- **Target:** `vt100.Parser`
- **Similarity:** 0.80
- **Dependents:** 1
- **Priority Score:** 1001102.1
- **Functions:** 10/10 matched (target 20)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 4. screen

- **Target:** `vt100.Screen`
- **Similarity:** 0.89
- **Dependents:** 0
- **Priority Score:** 9601.1
- **Functions:** 93/93 matched (target 108)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_

### 5. grid

- **Target:** `grid.Grid`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 6201.8
- **Functions:** 59/59 matched (target 77)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_

### 6. term

- **Target:** `term.Term`
- **Similarity:** 0.64
- **Dependents:** 0
- **Priority Score:** 3003.6
- **Functions:** 10/10 matched (target 76)
- **Missing functions:** _none_
- **Types:** 20/20 matched (target 21)
- **Missing types:** _none_

### 7. row

- **Target:** `vt100.Row`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 1801.8
- **Functions:** 17/17 matched (target 26)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 8. attrs

- **Target:** `vt100.Attrs`
- **Similarity:** 0.84
- **Dependents:** 0
- **Priority Score:** 1501.6
- **Functions:** 13/13 matched (target 18)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 6)
- **Missing types:** _none_

### 9. perform

- **Target:** `vt100.Perform`
- **Similarity:** 0.83
- **Dependents:** 0
- **Priority Score:** 1101.7
- **Functions:** 10/10 matched (target 16)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
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

