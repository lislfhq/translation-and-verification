package ast;

public class ArrayAssignNode extends ASTNode {
    public final String arrayName;
    public final ASTNode indexExpr;
    public final ASTNode valueExpr;

    public ArrayAssignNode(String arrayName, ASTNode indexExpr, ASTNode valueExpr, int l, int c) {
        super(l, c);
        this.arrayName = arrayName;
        this.indexExpr = indexExpr;
        this.valueExpr = valueExpr;
    }

    @Override
    public String dump(String indent) {
        return indent + "ArrayAssign (" + arrayName + ")\n"
                + indexExpr.dump(indent + "  ")
                + valueExpr.dump(indent + "  ");
    }
}