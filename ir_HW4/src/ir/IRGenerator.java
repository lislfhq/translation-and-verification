package ir;

import ast.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRGenerator implements ASTVisitor {
    public final List<IRFunction> functions = new ArrayList<>();
    private IRFunction currentFunc;
    private int tempCounter = 0;
    private int labelCounter = 0;
    private Operand lastExprOperand;

    // Кэш типов переменных текущей функции
    private final Map<String, Type> varTypes = new HashMap<>();

    private Operand newTemp(Type type) {
        String name = "t" + (tempCounter++);
        Operand t = Operand.ofVar(name, type);
        if (currentFunc != null) {
            currentFunc.localTypes.put(name, type);
        }
        return t;
    }

    private Operand newLabel(String prefix) {
        return Operand.ofLabel(prefix + "_" + (labelCounter++));
    }

    private void emit(IRInstruction ins) {
        currentFunc.instructions.add(ins);
    }

    public List<IRFunction> generate(ASTNode root) {
        functions.clear();
        if (root != null) {
            root.accept(this);
        }
        return functions;
    }

    @Override
    public void visit(ProgramNode node) {
        if (node.functions != null) {
            for (FunctionDeclNode fn : node.functions) {
                fn.accept(this);
            }
        }
    }

    @Override
    public void visit(FunctionDeclNode node) {
        currentFunc = new IRFunction(node.name);
        tempCounter = 0;
        varTypes.clear();

        if (node.parameters != null) {
            for (VarDeclNode p : node.parameters) {
                Operand op = Operand.ofVar(p.name, p.declaredType);
                currentFunc.params.add(op);
                currentFunc.localTypes.put(p.name, p.declaredType);
                varTypes.put(p.name, p.declaredType);
            }
        }

        if (node.returnParams != null) {
            for (VarDeclNode r : node.returnParams) {
                Operand op = Operand.ofVar(r.name, r.declaredType);
                currentFunc.returns.add(op);
                currentFunc.localTypes.put(r.name, r.declaredType);
                varTypes.put(r.name, r.declaredType);
                // Инициализируем выходные скалярные переменные нулём (как локальные слоты в JVM)
                if (r.declaredType == Type.INT || r.declaredType == Type.BOOL) {
                    emit(IRInstruction.assign(op, Operand.ofInt(0)));
                }
            }
        }

        if (node.usesParams != null) {
            for (VarDeclNode u : node.usesParams) {
                Type t = u.declaredType != null ? u.declaredType : Type.INT;
                Operand op = Operand.ofVar(u.name, t);
                currentFunc.localTypes.put(u.name, t);
                varTypes.put(u.name, t);
                if (t == Type.INT_ARRAY || t == Type.BOOL_ARRAY) {
                    // В Funny локальный массив без явного размера выделяется с рабочим буфером
                    emit(IRInstruction.newArr(op, Operand.ofInt(64)));
                } else {
                    emit(IRInstruction.assign(op, Operand.ofInt(0)));
                }
            }
        }

        if (node.body != null) {
            for (ASTNode stmt : node.body) {
                stmt.accept(this);
            }
        }

        emit(IRInstruction.ret(currentFunc.returns));
        currentFunc.rebuildCFG();
        functions.add(currentFunc);
    }

    @Override
    public void visit(VarDeclNode node) {
        Type t = node.declaredType != null ? node.declaredType : Type.INT;
        varTypes.put(node.name, t);
        currentFunc.localTypes.put(node.name, t);
        if (node.expr != null) {
            node.expr.accept(this);
            emit(IRInstruction.assign(Operand.ofVar(node.name, t), lastExprOperand));
        }
    }

    @Override
    public void visit(AssignNode node) {
        node.expr.accept(this);
        Type t = varTypes.getOrDefault(node.name, node.expr.exprType);
        emit(IRInstruction.assign(Operand.ofVar(node.name, t), lastExprOperand));
    }

    @Override
    public void visit(TupleAssignNode node) {
        List<Operand> argOps = new ArrayList<>();
        if (node.call.args != null) {
            for (ASTNode arg : node.call.args) {
                arg.accept(this);
                argOps.add(lastExprOperand);
            }
        }

        List<Operand> dstOps = new ArrayList<>();
        for (int i = 0; i < node.targets.size(); i++) {
            String targetName = node.targets.get(i);
            Type t = varTypes.getOrDefault(targetName,
                    (node.call.resultTypes != null && i < node.call.resultTypes.size())
                            ? node.call.resultTypes.get(i) : Type.INT);
            dstOps.add(Operand.ofVar(targetName, t));
        }

        emit(IRInstruction.call(dstOps, node.call.name, argOps));
    }

    @Override
    public void visit(IfNode node) {
        Operand elseLbl = newLabel("L_else");
        Operand endLbl = newLabel("L_endif");

        node.cond.accept(this);
        if (node.elseBody != null && !node.elseBody.isEmpty()) {
            emit(IRInstruction.ifFalse(lastExprOperand, elseLbl));
            for (ASTNode stmt : node.thenBody) {
                stmt.accept(this);
            }
            emit(IRInstruction.jump(endLbl));
            emit(IRInstruction.label(elseLbl));
            for (ASTNode stmt : node.elseBody) {
                stmt.accept(this);
            }
            emit(IRInstruction.label(endLbl));
        } else {
            emit(IRInstruction.ifFalse(lastExprOperand, endLbl));
            for (ASTNode stmt : node.thenBody) {
                stmt.accept(this);
            }
            emit(IRInstruction.label(endLbl));
        }
    }

    @Override
    public void visit(WhileNode node) {
        Operand headLbl = newLabel("L_while");
        Operand exitLbl = newLabel("L_endwhile");

        emit(IRInstruction.label(headLbl));
        node.cond.accept(this);
        emit(IRInstruction.ifFalse(lastExprOperand, exitLbl));

        if (node.body != null) {
            for (ASTNode stmt : node.body) {
                stmt.accept(this);
            }
        }
        emit(IRInstruction.jump(headLbl));
        emit(IRInstruction.label(exitLbl));
    }

    @Override
    public void visit(BinaryOpNode node) {
        node.left.accept(this);
        Operand leftOp = lastExprOperand;
        node.right.accept(this);
        Operand rightOp = lastExprOperand;

        Operand dst = newTemp(node.exprType != Type.UNKNOWN ? node.exprType : Type.INT);
        emit(IRInstruction.binop(dst, node.op, leftOp, rightOp));
        lastExprOperand = dst;
    }

    @Override
    public void visit(UnaryOpNode node) {
        node.expr.accept(this);
        Operand src = lastExprOperand;
        Operand dst = newTemp(node.exprType != Type.UNKNOWN ? node.exprType : Type.INT);
        emit(IRInstruction.unop(dst, node.op, src));
        lastExprOperand = dst;
    }

    @Override
    public void visit(IntLitNode node) {
        lastExprOperand = Operand.ofInt(node.value);
    }

    @Override
    public void visit(BoolLitNode node) {
        lastExprOperand = Operand.ofBool(node.value);
    }

    @Override
    public void visit(VarNode node) {
        Type t = varTypes.getOrDefault(node.name, node.exprType);
        lastExprOperand = Operand.ofVar(node.name, t);
    }

    @Override
    public void visit(ArrayAccessNode node) {
        node.indexExpr.accept(this);
        Operand idxOp = lastExprOperand;
        Type arrType = varTypes.getOrDefault(node.arrayName, Type.INT_ARRAY);
        Operand arrOp = Operand.ofVar(node.arrayName, arrType);
        Operand dst = newTemp(node.exprType != Type.UNKNOWN ? node.exprType : Type.INT);
        emit(IRInstruction.loadArr(dst, arrOp, idxOp));
        lastExprOperand = dst;
    }

    @Override
    public void visit(ArrayAssignNode node) {
        node.indexExpr.accept(this);
        Operand idxOp = lastExprOperand;
        node.valueExpr.accept(this);
        Operand valOp = lastExprOperand;
        Type arrType = varTypes.getOrDefault(node.arrayName, Type.INT_ARRAY);
        Operand arrOp = Operand.ofVar(node.arrayName, arrType);
        emit(IRInstruction.storeArr(arrOp, idxOp, valOp));
    }

    @Override
    public void visit(FuncCallNode node) {
        List<Operand> argOps = new ArrayList<>();
        if (node.args != null) {
            for (ASTNode arg : node.args) {
                arg.accept(this);
                argOps.add(lastExprOperand);
            }
        }

        if ("length".equals(node.name) && argOps.size() == 1) {
            Operand dst = newTemp(Type.INT);
            emit(IRInstruction.length(dst, argOps.get(0)));
            lastExprOperand = dst;
            return;
        }

        Type retType = node.exprType != Type.UNKNOWN ? node.exprType : Type.INT;
        Operand dst = newTemp(retType);
        emit(IRInstruction.call(Collections.singletonList(dst), node.name, argOps));
        lastExprOperand = dst;
    }

    @Override
    public void visit(AssertNode node) {
        // В IR транслируем выражение проверки
        if (node.expr != null) {
            node.expr.accept(this);
        }
    }

    @Override
    public void visit(AssumeNode node) {
        if (node.expr != null) {
            node.expr.accept(this);
        }
    }

    @Override
    public void visit(ReturnNode node) {
        if (node.expr != null) {
            node.expr.accept(this);
        }
    }
}