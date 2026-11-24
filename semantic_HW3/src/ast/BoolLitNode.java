package ast;

public class BoolLitNode extends ASTNode {
    public final boolean value;

    public BoolLitNode(boolean value, int line, int column) {
        super(line, column);
        this.value = value;
        this.exprType = Type.BOOL;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String dump(String indent) {
        return indent + "BoolLit (" + value + ") [" + exprType + "]\n";
    }
}