package ast;

public class AssignNode extends ASTNode {
    public final String name;
    public final ASTNode expr;

    public AssignNode(String name, ASTNode expr, int l, int c) {
        super(l, c);
        this.name = name;
        this.expr = expr;
    }

    @Override
    public String dump(String indent) {
        return indent + "Assign: " + name + "\n" + (expr != null ? expr.dump(indent + "  ") : "");
    }
}