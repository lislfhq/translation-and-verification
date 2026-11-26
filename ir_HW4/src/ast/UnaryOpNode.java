package ast;

public class UnaryOpNode extends ASTNode {
    public final String op;
    public final ASTNode expr;

    public UnaryOpNode(String op, ASTNode expr, int line, int column) {
        super(line, column);
        this.op = op;
        this.expr = expr;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String dump(String indent) {
        String typeStr = (exprType != Type.UNKNOWN) ? " [" + exprType + "]" : "";
        return indent + "UnaryOp (" + op + ")" + typeStr + "\n"
                + expr.dump(indent + "  ");
    }
}