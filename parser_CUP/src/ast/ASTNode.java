package ast;

public abstract class ASTNode {
    public final int line;
    public final int column;

    public ASTNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public String dump(String indent) {
        return indent + getClass().getSimpleName() + "\n";
    }
}