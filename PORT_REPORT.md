=== Deep Analysis: tmp/vt100/src (rust) -> src/commonMain/kotlin (kotlin) ===

Scanning source codebase (rust)...
Codebase: tmp/vt100/src (rust)
  Files: 10
  Total imports: 5
  Most depended: callbacks (1 dependents)

Scanning target codebase (kotlin)...
Codebase: src/commonMain/kotlin (kotlin)
  Files: 17
  Total imports: 61
  Most depended: vt100.Attrs (2 dependents)

Comparing codebases...
Computing AST similarities...

=== Codebase Comparison Report ===

Source: tmp/vt100/src (10 files)
Target: src/commonMain/kotlin (17 files)
Scoring invariant: FnSim is required function body/parameter similarity. Class/type and symbol parity are reported beside it; whole-file shape is diagnostic only.

Matched:   10 files
Unmatched: 0 source, 1 target

=== Matched Files (by porting priority) ===

Source                        Target                        FnSim     Dependents FunctionParityTypeParity  Priority
 --------------------------------------------------------------------------------------------------------------------------
cell                          vt100.Cell                    0.76      1          21/21         1/1         1002202.4 
callbacks                     vt100.Callbacks               0.65      1          12/12         1/1         1001303.4 
parser                        vt100.Parser                  0.81      1          10/10         1/1         1001101.9 
screen                        vt100.Screen                  0.89      0          93/93         3/3         9601.1    
grid                          grid.Grid                     0.82      0          59/59         3/3         6201.8    
term                          term.Term                     0.64      0          10/10         20/20       3003.6    
row                           vt100.Row                     0.82      0          17/17         1/1         1801.8    
attrs                         vt100.Attrs                   0.84      0          13/13         2/2         1501.6    
perform                       vt100.Perform                 0.83      0          10/10         1/1         1101.7    
lib                           vt100.Lib [STUB]              1.00      0          0/0           0/0         0.0       

=== Function and Symbol Details ===

cell -> vt100.Cell
  similarity: 0.76, priority: 1002202.4, dependents: 1
  functions: 21/21 matched (target total: 38, required body score: 0.76)
  missing functions: none
  types: 1/1 matched (target total: 2)
  missing types: none

callbacks -> vt100.Callbacks
  similarity: 0.65, priority: 1001303.4, dependents: 1
  functions: 12/12 matched (target total: 12, required body score: 0.65)
  missing functions: none
  types: 1/1 matched (target total: 2)
  missing types: none

parser -> vt100.Parser
  similarity: 0.81, priority: 1001101.9, dependents: 1
  functions: 10/10 matched (target total: 20, required body score: 0.81)
  missing functions: none
  types: 1/1 matched (target total: 2)
  missing types: none

screen -> vt100.Screen
  similarity: 0.89, priority: 9601.1, dependents: 0
  functions: 93/93 matched (target total: 107, required body score: 0.89)
  missing functions: none
  types: 3/3 matched (target total: 4)
  missing types: none

grid -> grid.Grid
  similarity: 0.82, priority: 6201.8, dependents: 0
  functions: 59/59 matched (target total: 77, required body score: 0.82)
  missing functions: none
  types: 3/3 matched (target total: 4)
  missing types: none

term -> term.Term
  similarity: 0.64, priority: 3003.6, dependents: 0
  functions: 10/10 matched (target total: 76, required body score: 0.64)
  missing functions: none
  types: 20/20 matched (target total: 21)
  missing types: none

row -> vt100.Row
  similarity: 0.82, priority: 1801.8, dependents: 0
  functions: 17/17 matched (target total: 22, required body score: 0.82)
  missing functions: none
  types: 1/1 matched (target total: 1)
  missing types: none

attrs -> vt100.Attrs
  similarity: 0.84, priority: 1501.6, dependents: 0
  functions: 13/13 matched (target total: 18, required body score: 0.84)
  missing functions: none
  types: 2/2 matched (target total: 6)
  missing types: none

perform -> vt100.Perform
  similarity: 0.83, priority: 1101.7, dependents: 0
  functions: 10/10 matched (target total: 12, required body score: 0.83)
  missing functions: none
  types: 1/1 matched (target total: 1)
  missing types: none

lib -> vt100.Lib [STUB]
  similarity: 1.00, priority: 0.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 1.00)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none


=== Porting Quality Summary ===

Matched by exact header:          10 / 10
Matched by provenance fallback:   0 / 10
Matched by name:                  0 / 10
Total TODOs in target: 0
Total lint errors:    0
Stub files:           1

=== Big Picture ===

- Missing files: 0
- Incomplete ports (similarity < 60%): 0
- Stub files: 1
- Files missing functions: 0 (total deficit: 0 functions)
- Type definitions missing: 0
- Files missing tests: 0 (total deficit: 0 unported `#[test]` functions)
- Documentation coverage: 471 / 544 lines (87%)

Primary focus: replace stub files with real implementations

=== Files with Issues ===

File                          Similarity LineRatio  FunctionParityTests     TODOs Lint  Status
----------------------------------------------------------------------------------------------------
vt100.Lib [STUB]              1.00       0.00       -             -         0     0     STUB

=== Porting Recommendations ===

Incomplete ports (similarity < 60%): 0
Missing files: 0


=== Documentation Gaps ===

Documentation coverage: 471 / 544 lines (87%)
Files with >20% doc gap: 2

File                          Src Docs    Tgt Docs    Gap %     DocSim    DocAmt    DocEq     
----------------------------------------------------------------------------------------------
screen                        328         230         29%       0.96      0.70      0.83      
lib                           68          38          44%       0.61      0.56      0.58      

=== Generating Reports ===

✅ Generated: port_status_report.md
✅ Generated: high_priority_ports.md
✅ Generated: NEXT_ACTIONS.md
✅ Generated: port_lint_proposed_changes.md

📁 All reports generated successfully!
