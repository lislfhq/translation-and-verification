package ast;

public class EnsuresNode extends ASTNode {
    public final ASTNode expr;

    public EnsuresNode(ASTNode expr, int l, int c) {
        super(l, c);
        this.expr = expr;
    }

    @Override
    public String dump(String indent) {
        return indent + "Ensures:\n" + (expr != null ? expr.dump(indent + "  ") : "");
    }
}