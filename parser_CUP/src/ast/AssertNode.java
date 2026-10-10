package ast;

public class AssertNode extends ASTNode {
    public final ASTNode expr;

    public AssertNode(ASTNode expr, int l, int c) {
        super(l, c);
        this.expr = expr;
    }

    @Override
    public String dump(String indent) {
        return indent + "Assert:\n" + (expr != null ? expr.dump(indent + "  ") : "");
    }
}