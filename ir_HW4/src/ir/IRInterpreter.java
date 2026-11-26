package ir;

import ast.Type;
import java.util.*;

public class IRInterpreter {
    private final Map<String, IRFunction> funcMap = new LinkedHashMap<>();

    public IRInterpreter(List<IRFunction> functions) {
        for (IRFunction fn : functions) {
            funcMap.put(fn.name, fn);
        }
    }

    public List<Object> execute(String funcName, List<Object> args) {
        IRFunction fn = funcMap.get(funcName);
        if (fn == null) {
            throw new RuntimeException("Функция не найдена в IR: " + funcName);
        }

        Map<String, Object> env = new HashMap<>();
        for (int i = 0; i < fn.params.size(); i++) {
            Operand param = fn.params.get(i);
            Object val = (i < args.size()) ? args.get(i) : defaultVal(param.type);
            env.put(param.name, val);
        }

        Map<String, Integer> labelIndex = new HashMap<>();
        List<IRInstruction> code = fn.instructions;
        for (int i = 0; i < code.size(); i++) {
            if (code.get(i).op == IRInstruction.OpCode.LABEL) {
                labelIndex.put(code.get(i).targetLabel.name, i);
            }
        }

        int pc = 0;
        int steps = 0;
        while (pc < code.size()) {
            if (++steps > 100_000) {
                throw new RuntimeException("Превышен лимит шагов интерпретатора (возможен бесконечный цикл)");
            }
            IRInstruction ins = code.get(pc);
            switch (ins.op) {
                case LABEL:
                    pc++;
                    break;
                case ASSIGN:
                    env.put(ins.dst.name, eval(ins.src1, env));
                    pc++;
                    break;
                case BINOP: {
                    int a = toInt(eval(ins.src1, env));
                    int b = toInt(eval(ins.src2, env));
                    env.put(ins.dst.name, evalBinop(ins.operator, a, b));
                    pc++;
                    break;
                }
                case UNOP: {
                    int a = toInt(eval(ins.src1, env));
                    int res = "-".equals(ins.operator) ? -a : (a == 0 ? 1 : 0);
                    env.put(ins.dst.name, res);
                    pc++;
                    break;
                }
                case NEW_ARR: {
                    int size = toInt(eval(ins.src1, env));
                    env.put(ins.dst.name, new int[Math.max(size, 1)]);
                    pc++;
                    break;
                }
                case LOAD_ARR: {
                    int[] arr = (int[]) eval(ins.src1, env);
                    int idx = toInt(eval(ins.src2, env));
                    env.put(ins.dst.name, arr[idx]);
                    pc++;
                    break;
                }
                case STORE_ARR: {
                    int[] arr = (int[]) eval(ins.dst, env);
                    int idx = toInt(eval(ins.src1, env));
                    int val = toInt(eval(ins.src2, env));
                    arr[idx] = val;
                    pc++;
                    break;
                }
                case LENGTH: {
                    int[] arr = (int[]) eval(ins.src1, env);
                    env.put(ins.dst.name, arr.length);
                    pc++;
                    break;
                }
                case CALL: {
                    List<Object> callArgs = new ArrayList<>();
                    for (Operand a : ins.args) {
                        callArgs.add(eval(a, env));
                    }
                    List<Object> results = execute(ins.funcName, callArgs);
                    if (ins.dsts != null) {
                        for (int i = 0; i < ins.dsts.size(); i++) {
                            env.put(ins.dsts.get(i).name, results.get(i));
                        }
                    }
                    pc++;
                    break;
                }
                case GOTO:
                    pc = labelIndex.get(ins.targetLabel.name);
                    break;
                case IF_FALSE: {
                    int cond = toInt(eval(ins.src1, env));
                    if (cond == 0) {
                        pc = labelIndex.get(ins.targetLabel.name);
                    } else {
                        pc++;
                    }
                    break;
                }
                case RETURN: {
                    List<Object> res = new ArrayList<>();
                    for (Operand r : ins.args) {
                        res.add(eval(r, env));
                    }
                    return res;
                }
            }
        }
        return Collections.emptyList();
    }

    private Object eval(Operand op, Map<String, Object> env) {
        if (op.isConst()) {
            return op.intValue;
        }
        return env.getOrDefault(op.name, defaultVal(op.type));
    }

    private Object defaultVal(Type t) {
        if (t == Type.INT_ARRAY || t == Type.BOOL_ARRAY) {
            return new int[64];
        }
        return 0;
    }

    private int toInt(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Boolean) return ((Boolean) obj) ? 1 : 0;
        return 0;
    }

    private int evalBinop(String op, int a, int b) {
        switch (op) {
            case "+":  return a + b;
            case "-":  return a - b;
            case "*":  return a * b;
            case "/":  return b != 0 ? a / b : 0;
            case "%":  return b != 0 ? a % b : 0;
            case "==": return (a == b) ? 1 : 0;
            case "!=": return (a != b) ? 1 : 0;
            case "<":  return (a < b) ? 1 : 0;
            case "<=": return (a <= b) ? 1 : 0;
            case ">":  return (a > b) ? 1 : 0;
            case ">=": return (a >= b) ? 1 : 0;
            case "and": return (a != 0 && b != 0) ? 1 : 0;
            case "or":  return (a != 0 || b != 0) ? 1 : 0;
            default:   throw new RuntimeException("Неизвестный оператор в IR: " + op);
        }
    }
}