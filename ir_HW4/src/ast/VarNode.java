package ast;

public class VarNode extends ASTNode {
    public final String name;

    public VarNode(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }

    @Override
    public String dump(String indent) {
        String typeStr = (exprType != Type.UNKNOWN) ? " [" + exprType + "]" : "";
        return indent + "Var (" + name + ")" + typeStr + "\n";
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}