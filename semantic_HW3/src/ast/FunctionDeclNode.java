package ast;
import java.util.List;

public class FunctionDeclNode extends ASTNode {
    public final String name;
    public final List<VarDeclNode> parameters;
    public final List<VarDeclNode> returnParams;
    public final List<VarDeclNode> usesParams;
    public final List<ASTNode> body;

    public FunctionDeclNode(String name,
                            List<VarDeclNode> parameters,
                            List<VarDeclNode> returnParams,
                            List<VarDeclNode> usesParams,
                            List<ASTNode> body,
                            int l, int c) {
        super(l, c);
        this.name = name;
        this.parameters = parameters;
        this.returnParams = returnParams;
        this.usesParams = usesParams;
        this.body = body;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("FunctionDeclNode: ").append(name).append("\n");

        sb.append(indent).append("  Params:\n");
        if (parameters != null && !parameters.isEmpty()) {
            for (VarDeclNode param : parameters) {
                sb.append(param.dump(indent + "    "));
            }
        }

        sb.append(indent).append("  Returns:\n");
        if (returnParams != null && !returnParams.isEmpty()) {
            for (VarDeclNode ret : returnParams) {
                sb.append(ret.dump(indent + "    "));
            }
        }

        sb.append(indent).append("  Uses:\n");
        if (usesParams != null && !usesParams.isEmpty()) {
            for (VarDeclNode use : usesParams) {
                sb.append(use.dump(indent + "    "));
            }
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