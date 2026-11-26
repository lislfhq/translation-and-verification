package opt;

import ir.BasicBlock;
import ir.IRFunction;
import ir.IRInstruction;
import ir.Operand;
import regalloc.LivenessAnalyzer;
import java.util.*;

public class LocalOptimizer {
    private final OptimizationConfig config;

    public LocalOptimizer(OptimizationConfig config) {
        this.config = config;
    }

    public void optimize(IRFunction fn) {
        if (!config.isAnyEnabled()) return;

        fn.rebuildCFG();
        boolean changed;
        int guard = 0;
        do {
            changed = false;
            // Для безопасного локального DCE обновляем liveOut на уровне CFG
            LivenessAnalyzer.computeLiveness(fn.cfg);

            for (BasicBlock block : fn.cfg.blocks) {
                if (config.enableConstProp) {
                    changed |= runConstantPropAndFold(block);
                }
                if (config.enableAlgebraic) {
                    changed |= runAlgebraicSimplification(block);
                }
                if (config.enableCSE) {
                    changed |= runLocalCSE(block);
                }
                if (config.enableDCE) {
                    changed |= runLocalDCE(block);
                }
            }
        } while (changed && ++guard < 20);

        fn.syncInstructionsFromCFG();
        fn.rebuildCFG();
    }

    /** 1. Распространение и свёртка констант в базовом блоке */
    private boolean runConstantPropAndFold(BasicBlock block) {
        boolean changed = false;
        Map<String, Operand> constEnv = new HashMap<>();

        for (IRInstruction ins : block.instructions) {
            // Подставляем известные константы в читаемые операнды
            if (ins.src1 != null && ins.src1.isVar() && constEnv.containsKey(ins.src1.name)) {
                ins.src1 = constEnv.get(ins.src1.name);
                changed = true;
            }
            if (ins.src2 != null && ins.src2.isVar() && constEnv.containsKey(ins.src2.name)) {
                ins.src2 = constEnv.get(ins.src2.name);
                changed = true;
            }
            if (ins.args != null) {
                for (int i = 0; i < ins.args.size(); i++) {
                    Operand a = ins.args.get(i);
                    if (a != null && a.isVar() && constEnv.containsKey(a.name)) {
                        ins.args.set(i, constEnv.get(a.name));
                        changed = true;
                    }
                }
            }

            // Пытаемся свернуть инструкцию в константу
            if (ins.op == IRInstruction.OpCode.BINOP && ins.src1.isConst() && ins.src2.isConst()) {
                Integer folded = foldBinop(ins.operator, ins.src1.intValue, ins.src2.intValue);
                if (folded != null) {
                    Operand c = isBoolOp(ins.operator) ? Operand.ofBool(folded != 0) : Operand.ofInt(folded);
                    ins.op = IRInstruction.OpCode.ASSIGN;
                    ins.src1 = c;
                    ins.src2 = null;
                    ins.operator = null;
                    changed = true;
                }
            } else if (ins.op == IRInstruction.OpCode.UNOP && ins.src1.isConst()) {
                Operand c = "-".equals(ins.operator)
                        ? Operand.ofInt(-ins.src1.intValue)
                        : Operand.ofBool(ins.src1.intValue == 0);
                ins.op = IRInstruction.OpCode.ASSIGN;
                ins.src1 = c;
                ins.operator = null;
                changed = true;
            }

            // Обновляем таблицу известных констант
            for (String w : ins.getWrittenVars()) {
                constEnv.remove(w);
            }
            if (ins.op == IRInstruction.OpCode.ASSIGN && ins.dst != null && ins.dst.isVar()) {
                if (ins.src1.isConst()) {
                    constEnv.put(ins.dst.name, ins.src1);
                } else if (ins.src1.isVar() && constEnv.containsKey(ins.src1.name)) {
                    constEnv.put(ins.dst.name, constEnv.get(ins.src1.name));
                }
            }
        }
        return changed;
    }

    /** 2. Алгебраические упрощения */
    private boolean runAlgebraicSimplification(BasicBlock block) {
        boolean changed = false;
        for (IRInstruction ins : block.instructions) {
            if (ins.op != IRInstruction.OpCode.BINOP) continue;

            Operand a = ins.src1;
            Operand b = ins.src2;
            String op = ins.operator;

            if ("+".equals(op)) {
                if (isZero(b)) { makeAssign(ins, a); changed = true; }
                else if (isZero(a)) { makeAssign(ins, b); changed = true; }
            } else if ("-".equals(op)) {
                if (isZero(b)) { makeAssign(ins, a); changed = true; }
                else if (a.equals(b)) { makeAssign(ins, Operand.ofInt(0)); changed = true; }
            } else if ("*".equals(op)) {
                if (isZero(a) || isZero(b)) { makeAssign(ins, Operand.ofInt(0)); changed = true; }
                else if (isOne(b)) { makeAssign(ins, a); changed = true; }
                else if (isOne(a)) { makeAssign(ins, b); changed = true; }
            } else if ("/".equals(op)) {
                if (isOne(b)) { makeAssign(ins, a); changed = true; }
                else if (isZero(a)) { makeAssign(ins, Operand.ofInt(0)); changed = true; }
            } else if ("and".equals(op)) {
                if (isZero(a) || isZero(b)) { makeAssign(ins, Operand.ofBool(false)); changed = true; }
                else if (isOne(b)) { makeAssign(ins, a); changed = true; }
                else if (isOne(a)) { makeAssign(ins, b); changed = true; }
            } else if ("or".equals(op)) {
                if (isOne(a) || isOne(b)) { makeAssign(ins, Operand.ofBool(true)); changed = true; }
                else if (isZero(b)) { makeAssign(ins, a); changed = true; }
                else if (isZero(a)) { makeAssign(ins, b); changed = true; }
            }
        }
        return changed;
    }

    /** 3. Локальный CSE (Common Subexpression Elimination) */
    private boolean runLocalCSE(BasicBlock block) {
        boolean changed = false;
        Map<String, String> exprToVar = new HashMap<>();

        for (IRInstruction ins : block.instructions) {
            if (ins.op == IRInstruction.OpCode.BINOP) {
                String key = buildExprKey(ins.operator, ins.src1, ins.src2);
                String existingVar = exprToVar.get(key);
                if (existingVar != null && !existingVar.equals(ins.dst.name)) {
                    makeAssign(ins, Operand.ofVar(existingVar, ins.dst.type));
                    changed = true;
                } else {
                    invalidateVar(exprToVar, ins.dst.name);
                    exprToVar.put(key, ins.dst.name);
                }
            } else {
                for (String w : ins.getWrittenVars()) {
                    invalidateVar(exprToVar, w);
                }
            }
        }
        return changed;
    }

    /** 4. Локальный DCE (удаление мёртвых вычислений и копирований в пределах блока) */
    private boolean runLocalDCE(BasicBlock block) {
        boolean changed = false;
        Set<String> live = new HashSet<>(block.liveOut);

        ListIterator<IRInstruction> it = block.instructions.listIterator(block.instructions.size());
        while (it.hasPrevious()) {
            IRInstruction ins = it.previous();
            boolean hasSideEffects = ins.op == IRInstruction.OpCode.CALL
                    || ins.op == IRInstruction.OpCode.STORE_ARR
                    || ins.op == IRInstruction.OpCode.LABEL
                    || ins.op == IRInstruction.OpCode.GOTO
                    || ins.op == IRInstruction.OpCode.IF_FALSE
                    || ins.op == IRInstruction.OpCode.RETURN;

            // Удаляем тривиальные присваивания вида x = x
            if (ins.op == IRInstruction.OpCode.ASSIGN && ins.dst != null && ins.dst.equals(ins.src1)) {
                it.remove();
                changed = true;
                continue;
            }

            List<String> written = ins.getWrittenVars();
            if (!hasSideEffects && written.size() == 1) {
                String dstVar = written.get(0);
                if (!live.contains(dstVar)) {
                    it.remove();
                    changed = true;
                    continue;
                }
            }

            live.removeAll(written);
            live.addAll(ins.getReadVars());
        }
        return changed;
    }

    private String buildExprKey(String op, Operand a, Operand b) {
        String sa = a.toString();
        String sb = b.toString();
        if (("+".equals(op) || "*".equals(op) || "==".equals(op) || "!=".equals(op)
                || "and".equals(op) || "or".equals(op)) && sa.compareTo(sb) > 0) {
            return op + "|" + sb + "|" + sa;
        }
        return op + "|" + sa + "|" + sb;
    }

    private void invalidateVar(Map<String, String> exprToVar, String varName) {
        exprToVar.entrySet().removeIf(e -> e.getValue().equals(varName)
                || e.getKey().contains("|" + varName + "|")
                || e.getKey().endsWith("|" + varName));
    }

    private void makeAssign(IRInstruction ins, Operand src) {
        ins.op = IRInstruction.OpCode.ASSIGN;
        ins.src1 = src;
        ins.src2 = null;
        ins.operator = null;
    }

    private boolean isZero(Operand op) {
        return op != null && op.isConst() && op.intValue == 0;
    }

    private boolean isOne(Operand op) {
        return op != null && op.isConst() && op.intValue == 1;
    }

    private boolean isBoolOp(String op) {
        return "==".equals(op) || "!=".equals(op) || "<".equals(op) || "<=".equals(op)
                || ">".equals(op) || ">=".equals(op) || "and".equals(op) || "or".equals(op);
    }

    private Integer foldBinop(String op, int a, int b) {
        switch (op) {
            case "+":  return a + b;
            case "-":  return a - b;
            case "*":  return a * b;
            case "/":  return b != 0 ? a / b : null;
            case "%":  return b != 0 ? a % b : null;
            case "==": return (a == b) ? 1 : 0;
            case "!=": return (a != b) ? 1 : 0;
            case "<":  return (a < b) ? 1 : 0;
            case "<=": return (a <= b) ? 1 : 0;
            case ">":  return (a > b) ? 1 : 0;
            case ">=": return (a >= b) ? 1 : 0;
            case "and": return (a != 0 && b != 0) ? 1 : 0;
            case "or":  return (a != 0 || b != 0) ? 1 : 0;
            default:   return null;
        }
    }
}