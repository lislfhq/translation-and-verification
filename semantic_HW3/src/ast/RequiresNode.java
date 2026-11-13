package ast;

public class RequiresNode extends ASTNode {
    public final ASTNode expr;

    public RequiresNode(ASTNode expr, int l, int c) {
        super(l, c);
        this.expr = expr;
    }

    @Override
    public String dump(String indent) {
        return indent + "Requires:\n" + (expr != null ? expr.dump(indent + "  ") : "");
    }
}