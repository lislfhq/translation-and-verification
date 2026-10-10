package ast;

public class AssumeNode extends ASTNode {
    public final ASTNode expr;

    public AssumeNode(ASTNode expr, int l, int c) {
        super(l, c);
        this.expr = expr;
    }

    @Override
    public String dump(String indent) {
        return indent + "Assume:\n" + (expr != null ? expr.dump(indent + "  ") : "");
    }
}