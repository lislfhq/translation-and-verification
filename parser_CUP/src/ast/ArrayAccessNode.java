package ast;

public class ArrayAccessNode extends ASTNode {
    public final String arrayName;
    public final ASTNode indexExpr;

    public ArrayAccessNode(String arrayName, ASTNode indexExpr, int l, int c) {
        super(l, c);
        this.arrayName = arrayName;
        this.indexExpr = indexExpr;
    }

    @Override
    public String dump(String indent) {
        return indent + "ArrayAccess (" + arrayName + ")\n"
                + indexExpr.dump(indent + "  ");
    }
}