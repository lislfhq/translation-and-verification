package ast;

public class BinaryOpNode extends ASTNode {
    public final String op;
    public final ASTNode left;
    public final ASTNode right;

    public BinaryOpNode(String op, ASTNode left, ASTNode right, int line, int column) {
        super(line, column);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String dump(String indent) {
        return indent + "BinaryOp (" + op + ")\n"
                + left.dump(indent + "  ")
                + right.dump(indent + "  ");
    }
}