package ast;
import java.util.List;

public class WhileNode extends ASTNode {
    public final ASTNode cond;
    public final List<ASTNode> body;

    public WhileNode(ASTNode cond, List<ASTNode> body, int l, int c) {
        super(l, c);
        this.cond = cond;
        this.body = body;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("While:\n");
        sb.append(indent).append("  Condition:\n");
        if (cond != null) sb.append(cond.dump(indent + "    "));
        sb.append(indent).append("  Body:\n");
        if (body != null) {
            for (ASTNode node : body) {
                if (node != null) sb.append(node.dump(indent + "    "));
            }
        }
        return sb.toString();
    }
}