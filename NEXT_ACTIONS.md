# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 10/10 (100.0%)
- **Function parity:** 230/245 matched (target 346) — 93.9%
- **Class/type parity:** 31/33 matched (target 43) — 93.9%
- **Combined symbol parity:** 261/278 matched (target 389) — 93.9%
- **Average inline-code cosine:** 0.70 (function body across 9 matched files)
- **Average documentation cosine:** 0.39 (doc text across 9 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 3 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. parser

- **Target:** `vt100.Parser`
- **Similarity:** 0.21
- **Dependents:** 1
- **Priority Score:** 1071108.0
- **Functions:** 3/10 matched (target 9)
- **Missing functions:** `new`, `new_with_callbacks`, `screen_mut`, `callbacks_mut`, `default`, `write`, `flush`
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_

### 2. cell

- **Target:** `vt100.Cell`
- **Similarity:** 0.73
- **Dependents:** 1
- **Priority Score:** 1012202.7
- **Functions:** 20/21 matched (target 37)
- **Missing functions:** `eq`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 3. callbacks

- **Target:** `vt100.Callbacks`
- **Similarity:** 0.76
- **Dependents:** 1
- **Priority Score:** 1001302.4
- **Functions:** 12/12 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Lint issues:** 28

### 4. term

- **Target:** `term.Term`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 53004.4
- **Functions:** 7/10 matched (target 63)
- **Missing functions:** `new`, `default`, `extend_itoa`
- **Types:** 18/20 matched (target 21)
- **Missing types:** `MouseProtocolMode`, `MouseProtocolEncoding`

### 5. screen

- **Target:** `vt100.Screen`
- **Similarity:** 0.88
- **Dependents:** 0
- **Priority Score:** 29601.2
- **Functions:** 91/93 matched (target 98)
- **Missing functions:** `new`, `u16_to_u8`
- **Types:** 3/3 matched
- **Missing types:** _none_

### 6. perform

- **Target:** `vt100.Perform`
- **Similarity:** 0.65
- **Dependents:** 0
- **Priority Score:** 21103.5
- **Functions:** 8/10 matched
- **Missing functions:** `new`, `new_with_callbacks`
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Lint issues:** 3

### 7. grid

- **Target:** `grid.Grid`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 6201.8
- **Functions:** 59/59 matched (target 77)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_

### 8. row

- **Target:** `vt100.Row`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 1801.8
- **Functions:** 17/17 matched (target 22)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 9. attrs

- **Target:** `vt100.Attrs`
- **Similarity:** 0.84
- **Dependents:** 0
- **Priority Score:** 1501.6
- **Functions:** 13/13 matched (target 18)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 6)
- **Missing types:** _none_

### 10. lib

- **Target:** `vt100.Lib [STUB]`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

