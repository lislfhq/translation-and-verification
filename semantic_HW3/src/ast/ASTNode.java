package ast;

public abstract class ASTNode {
    public final int line;
    public final int column;

    public Type exprType = Type.UNKNOWN;

    public ASTNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public abstract void accept(ASTVisitor visitor);

    public String dump(String indent) {
        return indent + getClass().getSimpleName() +
                (exprType != Type.UNKNOWN ? " [" + exprType + "]" : "") + "\n";
    }
}