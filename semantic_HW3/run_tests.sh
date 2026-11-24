#!/usr/bin/env bash
set -u

CP="lib/java-cup-11b.jar:src"
FAIL=0

run_pos() {
    local src="$1"
    local ast="$2"
    if java -cp "$CP" Main "$src" "$ast" > /dev/null 2>&1; then
        echo "[PASS] $src"
    else
        echo "[FAIL] $src (ожидался exit 0 и совпадение AST)"
        FAIL=1
    fi
}

run_neg() {
    local src="$1"
    local expected_count="$2"
    shift 2
    local err
    err=$(java -cp "$CP" Main "$src" 2>&1 >/dev/null)
    local code=$?

    if [ "$code" -eq 0 ]; then
        echo "[FAIL] $src (ожидался exit != 0, получен 0)"
        FAIL=1
        return
    fi

    local actual_count
    actual_count=$(echo "$err" | grep -cE "Семантическая ошибка|Синтаксическая ошибка")
    if [ "$actual_count" -ne "$expected_count" ]; then
        echo "[FAIL] $src (ожидалось ошибок: $expected_count, получено: $actual_count)"
        echo "$err"
        FAIL=1
        return
    fi

    for pattern in "$@"; do
        if ! echo "$err" | grep -qF "$pattern"; then
            echo "[FAIL] $src (не найдена ожидаемая диагностика: $pattern)"
            echo "$err"
            FAIL=1
            return
        fi
    done

    echo "[PASS] $src ($actual_count ош.)"
}

run_pos tests/test_positive.txt tests/test_positive.ast
run_pos tests/test_contracts.txt tests/test_contracts.ast
run_pos tests/test_recursion_multi.txt tests/test_recursion_multi.ast
run_pos tests/test_arrays.txt tests/test_arrays.ast
run_pos tests/test_tuples_logic.txt tests/test_tuples_logic.ast

run_neg tests/test_neg_readonly.txt 1 "[2:5]" "неизменяем"
run_neg tests/test_neg_contracts_scope.txt 2 "[1:23]" "[1:53]"
run_neg tests/test_neg_scalar_index.txt 2 "[2:9]" "[4:5]" "не является массивом"
run_neg tests/test_neg_inference.txt 3 "[2:9]" "Невозможно вывести тип"
run_neg tests/test_neg_unknown_call.txt 2 "unknown" "missing"
run_neg tests/test_neg_missing_type.txt 2 "Синтаксическая ошибка" "[1:7]"
run_neg tests/test_neg_array_cmp.txt 1 "[2:9]" "применим только к операндам типа INT"
run_neg tests/test_neg_inference_recovery.txt 2 "[6:11]" "[8:11]"

exit $FAIL