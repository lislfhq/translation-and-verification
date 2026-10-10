package ast;
import java.util.List;

public class FunctionDeclNode extends ASTNode {
    public final String name;
    public final List<ASTNode> body;

    public FunctionDeclNode(String name, List<ASTNode> body, int l, int c) {
        super(l, c);
        this.name = name;
        this.body = body;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("FunctionDeclNode: ").append(name).append("\n");
        sb.append(indent).append("  Body:\n");
        if (body != null) {
            for (ASTNode stmt : body) {
                if (stmt != null) {
                    sb.append(stmt.dump(indent + "    "));
                }
            }
        }
        return sb.toString();
    }
}