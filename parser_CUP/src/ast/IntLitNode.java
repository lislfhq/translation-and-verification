package ast;

public class IntLitNode extends ASTNode {
    public final int value;

    public IntLitNode(int value, int line, int column) {
        super(line, column);
        this.value = value;
    }

    @Override
    public String dump(String indent) {
        return indent + "IntLit (" + value + ")\n";
    }
}