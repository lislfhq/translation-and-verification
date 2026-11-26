package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IRInstruction {
    public enum OpCode {
        ASSIGN,     // dst = src1
        BINOP,      // dst = src1 op src2
        UNOP,       // dst = op src1
        LOAD_ARR,   // dst = src1[src2]
        STORE_ARR,  // dst[src1] = src2
        NEW_ARR,    // dst = new_array(src1)
        LENGTH,     // dst = length(src1)
        CALL,       // dsts = call funcName(args...)
        LABEL,      // label:
        GOTO,       // goto label
        IF_FALSE,   // if_false src1 goto label
        RETURN      // return args...
    }

    public OpCode op;
    public Operand dst;
    public List<Operand> dsts; // для кортежного CALL
    public Operand src1;
    public Operand src2;
    public String operator;    // "+", "-", "*", "/", "%", "==", "!=", "<", "<=", ">", ">=", "and", "or", "not"
    public String funcName;
    public List<Operand> args;
    public Operand targetLabel;

    public IRInstruction(OpCode op) {
        this.op = op;
    }

    public static IRInstruction assign(Operand dst, Operand src) {
        IRInstruction ins = new IRInstruction(OpCode.ASSIGN);
        ins.dst = dst;
        ins.src1 = src;
        return ins;
    }

    public static IRInstruction binop(Operand dst, String operator, Operand left, Operand right) {
        IRInstruction ins = new IRInstruction(OpCode.BINOP);
        ins.dst = dst;
        ins.operator = operator;
        ins.src1 = left;
        ins.src2 = right;
        return ins;
    }

    public static IRInstruction unop(Operand dst, String operator, Operand src) {
        IRInstruction ins = new IRInstruction(OpCode.UNOP);
        ins.dst = dst;
        ins.operator = operator;
        ins.src1 = src;
        return ins;
    }

    public static IRInstruction loadArr(Operand dst, Operand array, Operand index) {
        IRInstruction ins = new IRInstruction(OpCode.LOAD_ARR);
        ins.dst = dst;
        ins.src1 = array;
        ins.src2 = index;
        return ins;
    }

    public static IRInstruction storeArr(Operand array, Operand index, Operand value) {
        IRInstruction ins = new IRInstruction(OpCode.STORE_ARR);
        ins.dst = array;
        ins.src1 = index;
        ins.src2 = value;
        return ins;
    }

    public static IRInstruction newArr(Operand dst, Operand size) {
        IRInstruction ins = new IRInstruction(OpCode.NEW_ARR);
        ins.dst = dst;
        ins.src1 = size;
        return ins;
    }

    public static IRInstruction length(Operand dst, Operand array) {
        IRInstruction ins = new IRInstruction(OpCode.LENGTH);
        ins.dst = dst;
        ins.src1 = array;
        return ins;
    }

    public static IRInstruction call(List<Operand> dsts, String funcName, List<Operand> args) {
        IRInstruction ins = new IRInstruction(OpCode.CALL);
        ins.dsts = new ArrayList<>(dsts);
        if (dsts.size() == 1) {
            ins.dst = dsts.get(0);
        }
        ins.funcName = funcName;
        ins.args = new ArrayList<>(args);
        return ins;
    }

    public static IRInstruction label(Operand lbl) {
        IRInstruction ins = new IRInstruction(OpCode.LABEL);
        ins.targetLabel = lbl;
        return ins;
    }

    public static IRInstruction jump(Operand lbl) {
        IRInstruction ins = new IRInstruction(OpCode.GOTO);
        ins.targetLabel = lbl;
        return ins;
    }

    public static IRInstruction ifFalse(Operand cond, Operand lbl) {
        IRInstruction ins = new IRInstruction(OpCode.IF_FALSE);
        ins.src1 = cond;
        ins.targetLabel = lbl;
        return ins;
    }

    public static IRInstruction ret(List<Operand> values) {
        IRInstruction ins = new IRInstruction(OpCode.RETURN);
        ins.args = new ArrayList<>(values);
        return ins;
    }

    /** Возвращает все переменные, которые читаются в этой инструкции */
    public List<String> getReadVars() {
        List<String> reads = new ArrayList<>();
        if (op == OpCode.STORE_ARR && dst != null && dst.isVar()) {
            // Запись в массив читает ссылку на сам массив и индекс
            reads.add(dst.name);
        }
        if (src1 != null && src1.isVar()) reads.add(src1.name);
        if (src2 != null && src2.isVar()) reads.add(src2.name);
        if (args != null) {
            for (Operand a : args) {
                if (a != null && a.isVar()) reads.add(a.name);
            }
        }
        return reads;
    }

    /** Возвращает все переменные, в которые пишет эта инструкция */
    public List<String> getWrittenVars() {
        if (op == OpCode.STORE_ARR) {
            return Collections.emptyList();
        }
        if (op == OpCode.CALL && dsts != null && !dsts.isEmpty()) {
            List<String> w = new ArrayList<>();
            for (Operand d : dsts) {
                if (d != null && d.isVar()) w.add(d.name);
            }
            return w;
        }
        if (dst != null && dst.isVar()) {
            return Collections.singletonList(dst.name);
        }
        return Collections.emptyList();
    }

    public IRInstruction copy() {
        IRInstruction c = new IRInstruction(this.op);
        c.dst = this.dst;
        c.dsts = this.dsts != null ? new ArrayList<>(this.dsts) : null;
        c.src1 = this.src1;
        c.src2 = this.src2;
        c.operator = this.operator;
        c.funcName = this.funcName;
        c.args = this.args != null ? new ArrayList<>(this.args) : null;
        c.targetLabel = this.targetLabel;
        return c;
    }

    @Override
    public String toString() {
        switch (op) {
            case ASSIGN:
                return "  " + dst + " = " + src1;
            case BINOP:
                return "  " + dst + " = " + src1 + " " + operator + " " + src2;
            case UNOP:
                return "  " + dst + " = " + operator + " " + src1;
            case LOAD_ARR:
                return "  " + dst + " = " + src1 + "[" + src2 + "]";
            case STORE_ARR:
                return "  " + dst + "[" + src1 + "] = " + src2;
            case NEW_ARR:
                return "  " + dst + " = new_array(" + src1 + ")";
            case LENGTH:
                return "  " + dst + " = length(" + src1 + ")";
            case CALL: {
                String argStr = args.stream().map(Operand::toString).collect(Collectors.joining(", "));
                if (dsts != null && !dsts.isEmpty()) {
                    String dstStr = dsts.stream().map(Operand::toString).collect(Collectors.joining(", "));
                    return "  " + dstStr + " = call " + funcName + "(" + argStr + ")";
                }
                return "  call " + funcName + "(" + argStr + ")";
            }
            case LABEL:
                return targetLabel + ":";
            case GOTO:
                return "  goto " + targetLabel;
            case IF_FALSE:
                return "  if_false " + src1 + " goto " + targetLabel;
            case RETURN: {
                String retStr = args.stream().map(Operand::toString).collect(Collectors.joining(", "));
                return "  return " + retStr;
            }
            default:
                return "";
        }
    }
}