package ast;

public class VarDeclNode extends ASTNode {
    public final String name;
    public Type declaredType;
    public final ASTNode expr;

    public VarDeclNode(String name, Type declaredType, ASTNode expr, int l, int c) {
        super(l, c);
        this.name = name;
        this.declaredType = declaredType;
        this.expr = expr;
    }

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