# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 10/10 (100.0%)
- **Function parity:** 244/245 matched (target 400) — 99.6%
- **Class/type parity:** 33/33 matched (target 48) — 100.0%
- **Combined symbol parity:** 277/278 matched (target 448) — 99.6%
- **Average inline-code cosine:** 0.81 (function body across 10 matched files)
- **Average documentation cosine:** 0.39 (doc text across 10 matched files)
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
- **Priority Score:** 19601.1
- **Functions:** 92/93 matched (target 106)
- **Missing functions:** `u16_to_u8`
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

### 10. lib

- **Target:** `vt100.Lib`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

