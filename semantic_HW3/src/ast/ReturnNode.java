package ast;

public class ReturnNode extends ASTNode {
    public final ASTNode expr;

    public ReturnNode(ASTNode expr, int l, int c) {
        super(l, c);
        this.expr = expr;
    }

    @Override
    public String dump(String indent) {
        return indent + "Return:\n" + (expr != null ? expr.dump(indent + "  ") : "");
    }
}