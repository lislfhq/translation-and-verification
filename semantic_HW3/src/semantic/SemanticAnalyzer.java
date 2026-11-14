package semantic;

import ast.*;
import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private Scope currentScope;
    private final List<String> errors;

    public SemanticAnalyzer() {
        this.currentScope = new Scope(null);
        this.errors = new ArrayList<>();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printErrors() {
        for (String err : errors) {
            System.err.println(err);
        }
    }

    private void reportError(int line, int col, String msg) {
        errors.add("Семантическая ошибка [" + line + ":" + col + "]: " + msg);
    }

    public void analyze(ASTNode root) {
        visit(root);
    }

    private void visit(ASTNode node) {
        if (node == null) return;

        if (node instanceof FunctionDeclNode) visitFunctionDecl((FunctionDeclNode) node);
        else if (node instanceof VarDeclNode) visitVarDecl((VarDeclNode) node);
        else if (node instanceof AssignNode) visitAssign((AssignNode) node);
        else if (node instanceof IfNode) visitIf((IfNode) node);
        else if (node instanceof WhileNode) visitWhile((WhileNode) node);
        else if (node instanceof BinaryOpNode) visitBinaryOp((BinaryOpNode) node);
        else if (node instanceof IntLitNode) visitIntLit((IntLitNode) node);
        else if (node instanceof VarNode) visitVar((VarNode) node);
        else if (node instanceof ReturnNode) visitReturn((ReturnNode) node);
        else if (node instanceof ArrayAssignNode) visitArrayAssign((ArrayAssignNode) node);
        else if (node instanceof ArrayAccessNode) visitArrayAccess((ArrayAccessNode) node);
        else if (node instanceof FuncCallNode) visitFuncCall((FuncCallNode) node);

        else if (node instanceof AssertNode) visit(((AssertNode) node).expr);
        else if (node instanceof AssumeNode) visit(((AssumeNode) node).expr);
        else if (node instanceof RequiresNode) visit(((RequiresNode) node).expr);
        else if (node instanceof EnsuresNode) visit(((EnsuresNode) node).expr);
    }

    private void visitFunctionDecl(FunctionDeclNode node) {
        List<Type> paramTypes = new ArrayList<>();
        if (node.parameters != null) {
            for (VarDeclNode p : node.parameters) {
                paramTypes.add(p.declaredType);
            }
        }

        SymbolInfo funcSym = new SymbolInfo(node.name, Type.UNKNOWN, true, paramTypes);
        if (!currentScope.define(funcSym)) {
            reportError(node.line, node.column, "Функция '" + node.name + "' уже объявлена.");
        }

        currentScope = new Scope(currentScope);

        registerVars(node.parameters);
        registerVars(node.returnParams);
        registerVars(node.usesParams);

        if (node.body != null) {
            for (ASTNode stmt : node.body) {
                visit(stmt);
            }
        }

        currentScope = currentScope.getParent();
    }

    private void registerVars(List<VarDeclNode> list) {
        if (list != null) {
            for (VarDeclNode v : list) {
                Type t = v.declaredType != null ? v.declaredType : Type.INT;
                if (!currentScope.define(new SymbolInfo(v.name, t, false, null))) {
                    reportError(v.line, v.column, "Переменная '" + v.name + "' уже объявлена в этой области видимости.");
                }
            }
        }
    }

    private void visitFuncCall(FuncCallNode node) {
        SymbolInfo sym = currentScope.resolve(node.name);
        if (sym == null) {
            reportError(node.line, node.column, "Функция '" + node.name + "' не объявлена.");
            node.exprType = Type.ERROR;
            return;
        }
        if (!sym.isFunction) {
            reportError(node.line, node.column, "'" + node.name + "' не является функцией.");
            node.exprType = Type.ERROR;
            return;
        }

        int expectedArgs = sym.paramTypes != null ? sym.paramTypes.size() : 0;
        int actualArgs = node.args != null ? node.args.size() : 0;

        if (expectedArgs != actualArgs) {
            reportError(node.line, node.column, "Неверное число аргументов функции '" + node.name + "'. Ожидалось " + expectedArgs + ", передано " + actualArgs);
        }

        if (node.args != null) {
            for (int i = 0; i < node.args.size(); i++) {
                ASTNode arg = node.args.get(i);
                visit(arg);
                if (i < expectedArgs) {
                    Type expectedType = sym.paramTypes.get(i);
                    if (arg.exprType != expectedType && arg.exprType != Type.ERROR) {
                        reportError(arg.line, arg.column, "Несовпадение типа аргумента " + (i+1) + " в вызове '" + node.name + "'. Ожидался " + expectedType + ", получен " + arg.exprType);
                    }
                }
            }
        }
        node.exprType = sym.type;
    }

    private void visitVarDecl(VarDeclNode node) {
        if (node.expr != null) {
            visit(node.expr);
        }

        Type expectedType = node.declaredType;

        if (expectedType == null && node.expr != null) {
            expectedType = node.expr.exprType;
            node.declaredType = expectedType;
        } else if (expectedType == null) {
            reportError(node.line, node.column, "Невозможно вывести тип для '" + node.name + "' (нет инициализатора).");
            expectedType = Type.ERROR;
        }

        if (node.expr != null && expectedType != node.expr.exprType && node.expr.exprType != Type.ERROR && node.expr.exprType != Type.UNKNOWN) {
            reportError(node.line, node.column, "Несовпадение типов. Ожидался " + expectedType + ", получено " + node.expr.exprType);
        }

        SymbolInfo sym = new SymbolInfo(node.name, expectedType, false, null);
        if (!currentScope.define(sym)) {
            reportError(node.line, node.column, "Переменная '" + node.name + "' уже объявлена в этой области видимости.");
        }
    }

    private void visitAssign(AssignNode node) {
        visit(node.expr);

        SymbolInfo sym = currentScope.resolve(node.name);
        if (sym == null) {
            reportError(node.line, node.column, "Переменная '" + node.name + "' используется до объявления.");
        } else if (sym.isFunction) {
            reportError(node.line, node.column, "Имя функции '" + node.name + "' нельзя использовать как переменную для присваивания.");
        } else if (sym.type != node.expr.exprType && node.expr.exprType != Type.ERROR) {
            reportError(node.line, node.column, "Несовпадение типов при присваивании. Переменная '" + node.name + "' имеет тип " + sym.type + ", получено " + node.expr.exprType);
        }
    }

    private void visitVar(VarNode node) {
        SymbolInfo sym = currentScope.resolve(node.name);
        if (sym == null) {
            reportError(node.line, node.column, "Переменная '" + node.name + "' не объявлена.");
            node.exprType = Type.ERROR;
        } else if (sym.isFunction) {
            reportError(node.line, node.column, "Имя функции '" + node.name + "' нельзя использовать как переменную.");
            node.exprType = Type.ERROR;
        } else {
            node.exprType = sym.type;
        }
    }

    private void visitBinaryOp(BinaryOpNode node) {
        visit(node.left);
        visit(node.right);

        Type leftType = node.left.exprType;
        Type rightType = node.right.exprType;

        if (leftType == Type.ERROR || rightType == Type.ERROR) {
            node.exprType = Type.ERROR;
            return;
        }

        if (node.op.matches("\\+|-|\\*|/|%")) {
            if (leftType == Type.INT && rightType == Type.INT) {
                node.exprType = Type.INT;
            } else {
                reportError(node.line, node.column, "Оператор '" + node.op + "' требует операнды типа INT.");
                node.exprType = Type.ERROR;
            }
        } else if (node.op.matches("==|!=|<|<=|>|>=")) {
            if (leftType == rightType) {
                node.exprType = Type.BOOL;
            } else {
                reportError(node.line, node.column, "Несовпадение типов в операторе сравнения '" + node.op + "'.");
                node.exprType = Type.ERROR;
            }
        }
    }

    private void visitIf(IfNode node) {
        visit(node.cond);
        if (node.cond.exprType != Type.BOOL && node.cond.exprType != Type.ERROR) {
            reportError(node.cond.line, node.cond.column, "Условие ветвления должно быть логическим (BOOL), а получено " + node.cond.exprType);
        }

        currentScope = new Scope(currentScope);
        if (node.thenBody != null) {
            for (ASTNode n : node.thenBody) visit(n);
        }
        currentScope = currentScope.getParent();

        if (node.elseBody != null && !node.elseBody.isEmpty()) {
            currentScope = new Scope(currentScope);
            for (ASTNode n : node.elseBody) visit(n);
            currentScope = currentScope.getParent();
        }
    }

    private void visitWhile(WhileNode node) {
        visit(node.cond);
        if (node.cond.exprType != Type.BOOL && node.cond.exprType != Type.ERROR) {
            reportError(node.cond.line, node.cond.column, "Условие цикла должно быть логическим (BOOL), а получено " + node.cond.exprType);
        }

        currentScope = new Scope(currentScope);
        if (node.body != null) {
            for (ASTNode n : node.body) visit(n);
        }
        currentScope = currentScope.getParent();
    }

    private void visitReturn(ReturnNode node) {
        if (node.expr != null) {
            visit(node.expr);
        }
    }

    private void visitIntLit(IntLitNode node) {
        node.exprType = Type.INT;
    }

    private void visitArrayAccess(ArrayAccessNode node) {
        SymbolInfo sym = currentScope.resolve(node.arrayName);
        if (sym == null) {
            reportError(node.line, node.column, "Массив '" + node.arrayName + "' не объявлен.");
        }

        visit(node.indexExpr);
        if (node.indexExpr.exprType != Type.INT && node.indexExpr.exprType != Type.ERROR) {
            reportError(node.indexExpr.line, node.indexExpr.column, "Индекс массива должен быть типа INT.");
        }
        node.exprType = Type.INT;
    }

    private void visitArrayAssign(ArrayAssignNode node) {
        SymbolInfo sym = currentScope.resolve(node.arrayName);
        if (sym == null) {
            reportError(node.line, node.column, "Массив '" + node.arrayName + "' не объявлен.");
        }

        visit(node.indexExpr);
        visit(node.valueExpr);
        if (node.indexExpr.exprType != Type.INT && node.indexExpr.exprType != Type.ERROR) {
            reportError(node.indexExpr.line, node.indexExpr.column, "Индекс массива должен быть типа INT.");
        }
    }
}