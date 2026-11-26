package ast;

import java.util.List;

public class WhileNode extends ASTNode {
    public final ASTNode cond;
    public final ASTNode invariant;
    public final List<ASTNode> body;

    public WhileNode(ASTNode cond, ASTNode invariant, List<ASTNode> body, int l, int c) {
        super(l, c);
        this.cond = cond;
        this.invariant = invariant;
        this.body = body;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("While:\n");
        sb.append(indent).append("  Cond:\n");
        if (cond != null) sb.append(cond.dump(indent + "    "));
        if (invariant != null) {
            sb.append(indent).append("  Invariant:\n");
            sb.append(invariant.dump(indent + "    "));
        }
        sb.append(indent).append("  Body:\n");
        if (body != null) {
            for (ASTNode stmt : body) {
                if (stmt != null) sb.append(stmt.dump(indent + "    "));
            }
        }
        return sb.toString();
    }
}