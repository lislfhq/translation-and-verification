#!/usr/bin/env bash
set -e

CP="lib/java-cup-11b.jar:src"

run_ast_check() {
    local src="$1"
    local ast="$2"
    java -cp "$CP" Main "$src" "$ast" > /dev/null
    echo "[PASS] AST snapshot: $src"
}

run_ir_check() {
    local src="$1"
    local args="$2"
    local out
    out=$(java -cp "$CP" Main "$src" --run "$args")
    echo "$out" | grep -q "=== IR ДО ОПТИМИЗАЦИЙ ==="
    echo "$out" | grep -q "=== IR ПОСЛЕ ОПТИМИЗАЦИЙ"
    echo "$out" | grep -q "=== CFG for function"
    echo "$out" | grep -q "=== Interference Graph for"
    echo "$out" | grep -q "Colors used (JVM local slots):"
    echo "$out" | grep -q "=== МЕТРИКИ"
    echo "$out" | grep -q "=== ПРОВЕРКА ВЫПОЛНЕНИЯ"
    echo "[PASS] IR + Opt + RegAlloc + Exec($args): $src"
}

# 1. Проверка совместимости с аннотированным AST (HW3)
run_ast_check tests/test_positive.txt tests/test_positive.ast
run_ast_check tests/test_contracts.txt tests/test_contracts.ast
run_ast_check tests/test_recursion_multi.txt tests/test_recursion_multi.ast
run_ast_check tests/test_arrays.txt tests/test_arrays.ast
run_ast_check tests/test_tuples_logic.txt tests/test_tuples_logic.ast

# 2. Проверка генерации IR, оптимизаций, раскраски и эквивалентности выполнения (HW4)
run_ir_check tests/test_positive.txt ""
run_ir_check tests/test_ir_expr.txt "7"
run_ir_check tests/test_ir_branch.txt "4,5"
run_ir_check tests/test_ir_branch.txt "-3,0"
run_ir_check tests/test_ir_loop.txt "5"
run_ir_check tests/test_tuples_logic.txt "4"

# 3. Проверка CLI-флагов отключения оптимизаций и вывода Graphviz DOT
java -cp "$CP" Main tests/test_ir_expr.txt --no-opt --run 7 | grep -q "const=false, alg=false, cse=false, dce=false"
echo "[PASS] CLI flag --no-opt"

java -cp "$CP" Main tests/test_ir_branch.txt --dot-cfg | grep -q "digraph CFG_"
echo "[PASS] CLI flag --dot-cfg"

java -cp "$CP" Main tests/test_ir_branch.txt --dot-ig | grep -q "graph Interference_"
echo "[PASS] CLI flag --dot-ig"

echo "Все тесты HW4 успешно пройдены!"