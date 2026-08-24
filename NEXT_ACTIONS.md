# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 10/10 (100.0%)
- **Function parity:** 245/245 matched (target 382) — 100.0%
- **Class/type parity:** 33/33 matched (target 44) — 100.0%
- **Combined symbol parity:** 278/278 matched (target 426) — 100.0%
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

- **Target:** `vt100.Cell [PROVENANCE-FALLBACK]`
- **Similarity:** 0.76
- **Dependents:** 1
- **Priority Score:** 1002202.4
- **Functions:** 21/21 matched (target 38)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `cell.rs` vs expected `cell.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:cell.rs` vs expected `cell.rs`
- **Proposed provenance header:** `// port-lint: source cell.rs` (current: `// port-lint: source cell.rs`)
- **Proposed provenance header:** `// port-lint: tests cell.rs` (current: `// port-lint: tests cell.rs`)
- **Lint issues:** 2

### 2. callbacks

- **Target:** `vt100.Callbacks [PROVENANCE-FALLBACK]`
- **Similarity:** 0.65
- **Dependents:** 1
- **Priority Score:** 1001303.4
- **Functions:** 12/12 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `callbacks.rs` vs expected `callbacks.rs`
- **Proposed provenance header:** `// port-lint: source callbacks.rs` (current: `// port-lint: source callbacks.rs`)
- **Lint issues:** 1

### 3. parser

- **Target:** `vt100.Parser [PROVENANCE-FALLBACK]`
- **Similarity:** 0.81
- **Dependents:** 1
- **Priority Score:** 1001101.9
- **Functions:** 10/10 matched (target 20)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `parser.rs` vs expected `parser.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:parser.rs` vs expected `parser.rs`
- **Proposed provenance header:** `// port-lint: source parser.rs` (current: `// port-lint: source parser.rs`)
- **Proposed provenance header:** `// port-lint: tests parser.rs` (current: `// port-lint: tests parser.rs`)
- **Lint issues:** 2

### 4. screen

- **Target:** `vt100.Screen [PROVENANCE-FALLBACK]`
- **Similarity:** 0.89
- **Dependents:** 0
- **Priority Score:** 9601.1
- **Functions:** 93/93 matched (target 107)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `screen.rs` vs expected `screen.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:screen.rs` vs expected `screen.rs`
- **Proposed provenance header:** `// port-lint: source screen.rs` (current: `// port-lint: source screen.rs`)
- **Proposed provenance header:** `// port-lint: tests screen.rs` (current: `// port-lint: tests screen.rs`)
- **Lint issues:** 2

### 5. grid

- **Target:** `grid.Grid [PROVENANCE-FALLBACK]`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 6201.8
- **Functions:** 59/59 matched (target 77)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `grid.rs` vs expected `grid.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:grid.rs` vs expected `grid.rs`
- **Proposed provenance header:** `// port-lint: source grid.rs` (current: `// port-lint: source grid.rs`)
- **Proposed provenance header:** `// port-lint: tests grid.rs` (current: `// port-lint: tests grid.rs`)
- **Lint issues:** 2

### 6. term

- **Target:** `term.Term [PROVENANCE-FALLBACK]`
- **Similarity:** 0.64
- **Dependents:** 0
- **Priority Score:** 3003.6
- **Functions:** 10/10 matched (target 76)
- **Missing functions:** _none_
- **Types:** 20/20 matched (target 21)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `term.rs` vs expected `term.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:term.rs` vs expected `term.rs`
- **Proposed provenance header:** `// port-lint: source term.rs` (current: `// port-lint: source term.rs`)
- **Proposed provenance header:** `// port-lint: tests term.rs` (current: `// port-lint: tests term.rs`)
- **Lint issues:** 2

### 7. row

- **Target:** `vt100.Row [PROVENANCE-FALLBACK]`
- **Similarity:** 0.82
- **Dependents:** 0
- **Priority Score:** 1801.8
- **Functions:** 17/17 matched (target 22)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `row.rs` vs expected `row.rs`
- **Proposed provenance header:** `// port-lint: source row.rs` (current: `// port-lint: source row.rs`)
- **Lint issues:** 1

### 8. attrs

- **Target:** `vt100.Attrs [PROVENANCE-FALLBACK]`
- **Similarity:** 0.84
- **Dependents:** 0
- **Priority Score:** 1501.6
- **Functions:** 13/13 matched (target 18)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 6)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `attrs.rs` vs expected `attrs.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:attrs.rs` vs expected `attrs.rs`
- **Proposed provenance header:** `// port-lint: source attrs.rs` (current: `// port-lint: source attrs.rs`)
- **Proposed provenance header:** `// port-lint: tests attrs.rs` (current: `// port-lint: tests attrs.rs`)
- **Lint issues:** 2

### 9. perform

- **Target:** `vt100.Perform [PROVENANCE-FALLBACK]`
- **Similarity:** 0.83
- **Dependents:** 0
- **Priority Score:** 1101.7
- **Functions:** 10/10 matched (target 12)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `perform.rs` vs expected `perform.rs`
- **Proposed provenance header:** `// port-lint: source perform.rs` (current: `// port-lint: source perform.rs`)
- **Lint issues:** 1

### 10. lib

- **Target:** `vt100.Lib [PROVENANCE-FALLBACK]`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `lib.rs` vs expected `lib.rs`
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source lib.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

