package ast;

public class VarDeclNode extends ASTNode {
    public final String name;
    public Type declaredType; // Может быть null (для вывода типов по бонусу)
    public final ASTNode expr;

    // Конструктор с явным типом
    public VarDeclNode(String name, Type declaredType, ASTNode expr, int l, int c) {
        super(l, c);
        this.name = name;
        this.declaredType = declaredType;
        this.expr = expr;
    }

    // Оверлоад для бонуса (если тип не указан в коде и его надо вывести)
    public VarDeclNode(String name, ASTNode expr, int l, int c) {
        this(name, null, expr, l, c);
    }

    @Override
    public String dump(String indent) {
        String t = (declaredType != null) ? declaredType.toString() : "INFERRED";
        return indent + "VarDecl: " + name + " : " + t + "\n"
                + (expr != null ? expr.dump(indent + "  ") : "");
    }
}