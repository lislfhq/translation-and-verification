package ast;
import java.util.List;

public class IfNode extends ASTNode {
    public final ASTNode cond;
    public final List<ASTNode> thenBody;
    public final List<ASTNode> elseBody;

    public IfNode(ASTNode cond, List<ASTNode> thenBody, List<ASTNode> elseBody, int l, int c) {
        super(l, c);
        this.cond = cond;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("If:\n");
        sb.append(indent).append("  Condition:\n");
        if (cond != null) sb.append(cond.dump(indent + "    "));
        sb.append(indent).append("  ThenBody:\n");
        if (thenBody != null) {
            for (ASTNode node : thenBody) {
                if (node != null) sb.append(node.dump(indent + "    "));
            }
        }
        if (elseBody != null && !elseBody.isEmpty()) {
            sb.append(indent).append("  ElseBody:\n");
            for (ASTNode node : elseBody) {
                if (node != null) sb.append(node.dump(indent + "    "));
            }
        }
        return sb.toString();
    }
}