package semantic;

import ast.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SemanticAnalyzer implements ASTVisitor {
    private Scope currentScope;
    private final List<String> errors;

    public SemanticAnalyzer() {
        this.currentScope = new Scope(null);
        this.errors = new ArrayList<>();

        // Встроенная функция length: (int[]) -> (int)
        currentScope.define(new SymbolInfo(
                "length",
                Type.INT,
                Collections.singletonList(Type.INT_ARRAY),
                Collections.singletonList(Type.INT)
        ));
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
        visitNode(root);
    }

    private void visitNode(ASTNode node) {
        if (node != null) {
            node.accept(this);
        }
    }

    @Override
    public void visit(ProgramNode node) {
        if (node.functions == null) return;

        for (FunctionDeclNode fn : node.functions) {
            registerFunctionSignature(fn);
        }

        for (FunctionDeclNode fn : node.functions) {
            visitFunctionBody(fn);
        }
    }

    private void registerFunctionSignature(FunctionDeclNode node) {
        List<Type> paramTypes = new ArrayList<>();
        if (node.parameters != null) {
            for (VarDeclNode p : node.parameters) {
                paramTypes.add(p.declaredType != null ? p.declaredType : Type.ERROR);
            }
        }

        List<Type> returnTypes = new ArrayList<>();
        if (node.returnParams != null) {
            for (VarDeclNode r : node.returnParams) {
                returnTypes.add(r.declaredType != null ? r.declaredType : Type.ERROR);
            }
        }

        Type primaryRetType = (returnTypes.size() == 1) ? returnTypes.get(0) : Type.VOID;
        SymbolInfo funcSym = new SymbolInfo(node.name, primaryRetType, paramTypes, returnTypes);
        if (!currentScope.define(funcSym)) {
            reportError(node.line, node.column, "Функция '" + node.name + "' уже объявлена.");
        }
    }

    @Override
    public void visit(FunctionDeclNode node) {
        registerFunctionSignature(node);
        visitFunctionBody(node);
    }

    private void visitFunctionBody(FunctionDeclNode node) {
        currentScope = new Scope(currentScope);

        registerVars(node.parameters, false, false);

        if (node.requiresExpr != null) {
            visitBoolClause(node.requiresExpr, "requires");
        }

        registerVars(node.returnParams, true, false);

        if (node.ensuresExpr != null) {
            visitBoolClause(node.ensuresExpr, "ensures");
        }

        registerVars(node.usesParams, true, true);

        if (node.body != null) {
            for (ASTNode stmt : node.body) {
                visitNode(stmt);
            }
        }

        if (node.usesParams != null) {
            for (VarDeclNode v : node.usesParams) {
                SymbolInfo sym = currentScope.resolve(v.name);
                if (sym != null && !sym.isFunction && sym.type == Type.UNKNOWN) {
                    reportError(v.line, v.column,
                            "Невозможно вывести тип локальной переменной '" + v.name + "' (отсутствует корректное присваивание).");
                    sym.type = Type.ERROR;
                    v.declaredType = Type.ERROR;
                }
            }
        }

        currentScope = currentScope.getParent();
    }

    private void visitBoolClause(ASTNode expr, String clauseName) {
        if (expr == null) return;
        visitNode(expr);
        if (expr.exprType != Type.BOOL && expr.exprType != Type.ERROR) {
            reportError(expr.line, expr.column,
                    "Выражение в '" + clauseName + "' должно иметь тип BOOL, а получено " + expr.exprType);
        }
    }

    private void registerVars(List<VarDeclNode> list, boolean isMutable, boolean allowInference) {
        if (list == null) return;
        for (VarDeclNode v : list) {
            Type t = v.declaredType;
            if (t == null) {
                if (allowInference) {
                    t = Type.UNKNOWN;
                } else {
                    reportError(v.line, v.column, "Отсутствует обязательная аннотация типа для '" + v.name + "'.");
                    t = Type.ERROR;
                }
            }
            if (!currentScope.define(new SymbolInfo(v.name, t, isMutable, v))) {
                reportError(v.line, v.column, "Переменная '" + v.name + "' уже объявлена в этой или внешней области видимости.");
            }
        }
    }

    // Общая проверка вызова функции; возвращает SymbolInfo при успехе или null при ошибке
    private SymbolInfo checkCallArguments(FuncCallNode node) {
        boolean hasArgError = false;
        if (node.args != null) {
            for (ASTNode arg : node.args) {
                visitNode(arg);
                if (arg.exprType == Type.ERROR) {
                    hasArgError = true;
                }
            }
        }

        SymbolInfo sym = currentScope.resolve(node.name);
        if (sym == null) {
            reportError(node.line, node.column, "Функция '" + node.name + "' не объявлена.");
            node.exprType = Type.ERROR;
            return null;
        }
        if (!sym.isFunction) {
            reportError(node.line, node.column, "'" + node.name + "' не является функцией.");
            node.exprType = Type.ERROR;
            return null;
        }

        int expectedArgs = sym.paramTypes != null ? sym.paramTypes.size() : 0;
        int actualArgs = node.args != null ? node.args.size() : 0;

        if (expectedArgs != actualArgs) {
            reportError(node.line, node.column,
                    "Неверное число аргументов функции '" + node.name + "'. Ожидалось " + expectedArgs + ", передано " + actualArgs);
            hasArgError = true;
        }

        if (node.args != null) {
            for (int i = 0; i < Math.min(expectedArgs, actualArgs); i++) {
                ASTNode arg = node.args.get(i);
                Type expectedType = sym.paramTypes.get(i);
                if (arg.exprType != expectedType && arg.exprType != Type.ERROR && expectedType != Type.ERROR) {
                    reportError(arg.line, arg.column,
                            "Несовпадение типа аргумента " + (i + 1) + " в вызове '" + node.name + "'. Ожидался " + expectedType + ", получен " + arg.exprType);
                    hasArgError = true;
                }
            }
        }

        if (hasArgError) {
            node.exprType = Type.ERROR;
            return null;
        }
        return sym;
    }

    @Override
    public void visit(FuncCallNode node) {
        SymbolInfo sym = checkCallArguments(node);
        if (sym == null) {
            return;
        }

        if (sym.returnTypes != null && sym.returnTypes.size() == 1) {
            node.exprType = sym.returnTypes.get(0);
        } else {
            reportError(node.line, node.column,
                    "Функция '" + node.name + "' возвращает не одно значение и не может использоваться в скалярном выражении.");
            node.exprType = Type.ERROR;
        }
    }

    @Override
    public void visit(TupleAssignNode node) {
        SymbolInfo sym = checkCallArguments(node.call);
        if (sym == null) {
            return;
        }

        List<Type> retTypes = sym.returnTypes != null ? sym.returnTypes : Collections.emptyList();
        if (retTypes.size() != node.targets.size()) {
            reportError(node.line, node.column,
                    "Несоответствие числа возвращаемых значений при кортежном присваивании: функция '" + node.call.name
                            + "' возвращает " + retTypes.size() + ", а переменных слева " + node.targets.size());
            return;
        }

        for (int i = 0; i < node.targets.size(); i++) {
            assignToTarget(node.targets.get(i), retTypes.get(i), node.line, node.column);
        }
    }

    private void assignToTarget(String name, Type rhsType, int line, int col) {
        SymbolInfo sym = currentScope.resolve(name);
        if (sym == null) {
            reportError(line, col, "Переменная '" + name + "' используется до объявления.");
            return;
        }
        if (sym.isFunction) {
            reportError(line, col, "Имя функции '" + name + "' нельзя использовать как переменную для присваивания.");
            return;
        }
        if (!sym.isMutable) {
            reportError(line, col, "Входной параметр '" + name + "' неизменяем (read-only).");
            return;
        }

        if (sym.type == Type.UNKNOWN) {
            if (rhsType != Type.ERROR && rhsType != Type.UNKNOWN) {
                sym.type = rhsType;
                if (sym.declNode != null) {
                    sym.declNode.declaredType = sym.type;
                }
            }
            return;
        }

        if (sym.type != rhsType && rhsType != Type.ERROR && sym.type != Type.ERROR) {
            reportError(line, col,
                    "Несовпадение типов при присваивании. Переменная '" + name + "' имеет тип " + sym.type + ", получено " + rhsType);
        }
    }

    @Override
    public void visit(VarDeclNode node) {
        if (node.expr != null) {
            visitNode(node.expr);
        }

        Type expectedType = node.declaredType;
        if (expectedType == null && node.expr != null) {
            expectedType = node.expr.exprType;
            node.declaredType = expectedType;
        } else if (expectedType == null) {
            reportError(node.line, node.column, "Невозможно вывести тип для '" + node.name + "' (нет инициализатора).");
            expectedType = Type.ERROR;
        }

        if (node.expr != null && expectedType != node.expr.exprType
                && node.expr.exprType != Type.ERROR && node.expr.exprType != Type.UNKNOWN) {
            reportError(node.line, node.column, "Несовпадение типов. Ожидался " + expectedType + ", получено " + node.expr.exprType);
        }

        SymbolInfo sym = new SymbolInfo(node.name, expectedType, true, node);
        if (!currentScope.define(sym)) {
            reportError(node.line, node.column, "Переменная '" + node.name + "' уже объявлена в этой или внешней области видимости.");
        }
    }

    @Override
    public void visit(AssignNode node) {
        visitNode(node.expr);
        assignToTarget(node.name, node.expr.exprType, node.line, node.column);
    }

    @Override
    public void visit(VarNode node) {
        SymbolInfo sym = currentScope.resolve(node.name);
        if (sym == null) {
            reportError(node.line, node.column, "Переменная '" + node.name + "' не объявлена.");
            node.exprType = Type.ERROR;
        } else if (sym.isFunction) {
            reportError(node.line, node.column, "Имя функции '" + node.name + "' нельзя использовать как переменную.");
            node.exprType = Type.ERROR;
        } else if (sym.type == Type.UNKNOWN) {
            reportError(node.line, node.column, "Переменная '" + node.name + "' используется до вывода её типа.");
            node.exprType = Type.ERROR;
        } else {
            node.exprType = sym.type;
        }
    }

    @Override
    public void visit(UnaryOpNode node) {
        visitNode(node.expr);
        if (node.expr.exprType == Type.ERROR) {
            node.exprType = Type.ERROR;
            return;
        }

        if (node.op.equals("-")) {
            if (node.expr.exprType == Type.INT) {
                node.exprType = Type.INT;
            } else {
                reportError(node.line, node.column, "Унарный '-' требует операнд типа INT, получено " + node.expr.exprType);
                node.exprType = Type.ERROR;
            }
        } else if (node.op.equals("not")) {
            if (node.expr.exprType == Type.BOOL) {
                node.exprType = Type.BOOL;
            } else {
                reportError(node.line, node.column, "Оператор 'not' требует операнд типа BOOL, получено " + node.expr.exprType);
                node.exprType = Type.ERROR;
            }
        }
    }

    @Override
    public void visit(BinaryOpNode node) {
        visitNode(node.left);
        visitNode(node.right);

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
        } else if (node.op.matches("<|<=|>|>=")) {
            if (leftType == Type.INT && rightType == Type.INT) {
                node.exprType = Type.BOOL;
            } else {
                reportError(node.line, node.column,
                        "Оператор сравнения '" + node.op + "' применим только к операндам типа INT, получено " + leftType + " и " + rightType);
                node.exprType = Type.ERROR;
            }
        } else if (node.op.matches("==|!=")) {
            if (leftType == rightType) {
                node.exprType = Type.BOOL;
            } else {
                reportError(node.line, node.column, "Несовпадение типов в операторе сравнения '" + node.op + "'.");
                node.exprType = Type.ERROR;
            }
        } else if (node.op.equals("and") || node.op.equals("or")) {
            if (leftType == Type.BOOL && rightType == Type.BOOL) {
                node.exprType = Type.BOOL;
            } else {
                reportError(node.line, node.column,
                        "Логический оператор '" + node.op + "' требует операнды типа BOOL, получено " + leftType + " и " + rightType);
                node.exprType = Type.ERROR;
            }
        }
    }

    @Override
    public void visit(IfNode node) {
        visitNode(node.cond);
        if (node.cond.exprType != Type.BOOL && node.cond.exprType != Type.ERROR) {
            reportError(node.cond.line, node.cond.column, "Условие ветвления должно быть логическим (BOOL), а получено " + node.cond.exprType);
        }

        currentScope = new Scope(currentScope);
        if (node.thenBody != null) {
            for (ASTNode n : node.thenBody) visitNode(n);
        }
        currentScope = currentScope.getParent();

        if (node.elseBody != null && !node.elseBody.isEmpty()) {
            currentScope = new Scope(currentScope);
            for (ASTNode n : node.elseBody) visitNode(n);
            currentScope = currentScope.getParent();
        }
    }

    @Override
    public void visit(WhileNode node) {
        visitNode(node.cond);
        if (node.cond.exprType != Type.BOOL && node.cond.exprType != Type.ERROR) {
            reportError(node.cond.line, node.cond.column, "Условие цикла должно быть логическим (BOOL), а получено " + node.cond.exprType);
        }

        if (node.invariant != null) {
            visitBoolClause(node.invariant, "invariant");
        }

        currentScope = new Scope(currentScope);
        if (node.body != null) {
            for (ASTNode n : node.body) visitNode(n);
        }
        currentScope = currentScope.getParent();
    }

    @Override
    public void visit(ReturnNode node) {
        if (node.expr != null) {
            visitNode(node.expr);
        }
    }

    @Override
    public void visit(IntLitNode node) {
        node.exprType = Type.INT;
    }

    @Override
    public void visit(BoolLitNode node) {
        node.exprType = Type.BOOL;
    }

    @Override
    public void visit(AssertNode node) {
        visitBoolClause(node.expr, "assert");
    }

    @Override
    public void visit(AssumeNode node) {
        visitBoolClause(node.expr, "assume");
    }

    private Type getArrayElementType(Type arrayType) {
        if (arrayType == Type.INT_ARRAY) return Type.INT;
        if (arrayType == Type.BOOL_ARRAY) return Type.BOOL;
        return null;
    }

    @Override
    public void visit(ArrayAccessNode node) {
        visitNode(node.indexExpr);
        boolean badIndex = false;
        if (node.indexExpr.exprType != Type.INT) {
            if (node.indexExpr.exprType != Type.ERROR) {
                reportError(node.indexExpr.line, node.indexExpr.column, "Индекс массива должен быть типа INT.");
            }
            badIndex = true;
        }

        SymbolInfo sym = currentScope.resolve(node.arrayName);
        if (sym == null) {
            reportError(node.line, node.column, "Массив '" + node.arrayName + "' не объявлен.");
            node.exprType = Type.ERROR;
            return;
        }
        if (sym.isFunction) {
            reportError(node.line, node.column, "Имя функции '" + node.arrayName + "' нельзя индексировать.");
            node.exprType = Type.ERROR;
            return;
        }

        Type elemType = getArrayElementType(sym.type);
        if (elemType == null) {
            reportError(node.line, node.column, "Переменная '" + node.arrayName + "' не является массивом (тип " + sym.type + ").");
            node.exprType = Type.ERROR;
            return;
        }

        node.exprType = badIndex ? Type.ERROR : elemType;
    }

    @Override
    public void visit(ArrayAssignNode node) {
        visitNode(node.indexExpr);
        visitNode(node.valueExpr);

        if (node.indexExpr.exprType != Type.INT && node.indexExpr.exprType != Type.ERROR) {
            reportError(node.indexExpr.line, node.indexExpr.column, "Индекс массива должен быть типа INT.");
        }

        SymbolInfo sym = currentScope.resolve(node.arrayName);
        if (sym == null) {
            reportError(node.line, node.column, "Массив '" + node.arrayName + "' не объявлен.");
            return;
        }
        if (sym.isFunction) {
            reportError(node.line, node.column, "Имя функции '" + node.arrayName + "' нельзя использовать как массив.");
            return;
        }
        if (!sym.isMutable) {
            reportError(node.line, node.column, "Входной параметр-массив '" + node.arrayName + "' неизменяем (read-only).");
            return;
        }

        Type elemType = getArrayElementType(sym.type);
        if (elemType == null) {
            reportError(node.line, node.column, "Переменная '" + node.arrayName + "' не является массивом (тип " + sym.type + ").");
            return;
        }

        if (node.valueExpr.exprType != elemType && node.valueExpr.exprType != Type.ERROR) {
            reportError(node.line, node.column,
                    "Несовпадение типа при записи в массив '" + node.arrayName + "'. Ожидался " + elemType + ", получено " + node.valueExpr.exprType);
        }
    }
}