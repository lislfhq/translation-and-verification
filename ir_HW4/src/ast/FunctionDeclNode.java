package ast;

import java.util.List;

public class FunctionDeclNode extends ASTNode {
    public final String name;
    public final List<VarDeclNode> parameters;
    public final ASTNode requiresExpr;
    public final List<VarDeclNode> returnParams;
    public final ASTNode ensuresExpr;
    public final List<VarDeclNode> usesParams;
    public final List<ASTNode> body;

    public FunctionDeclNode(String name,
                            List<VarDeclNode> parameters,
                            ASTNode requiresExpr,
                            List<VarDeclNode> returnParams,
                            ASTNode ensuresExpr,
                            List<VarDeclNode> usesParams,
                            List<ASTNode> body,
                            int l, int c) {
        super(l, c);
        this.name = name;
        this.parameters = parameters;
        this.requiresExpr = requiresExpr;
        this.returnParams = returnParams;
        this.ensuresExpr = ensuresExpr;
        this.usesParams = usesParams;
        this.body = body;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("FunctionDeclNode: ").append(name).append("\n");

        sb.append(indent).append("  Params:\n");
        if (parameters != null) {
            for (VarDeclNode param : parameters) {
                sb.append(param.dump(indent + "    "));
            }
        }

        if (requiresExpr != null) {
            sb.append(indent).append("  Requires:\n");
            sb.append(requiresExpr.dump(indent + "    "));
        }

        sb.append(indent).append("  Returns:\n");
        if (returnParams != null) {
            for (VarDeclNode ret : returnParams) {
                sb.append(ret.dump(indent + "    "));
            }
        }

        if (ensuresExpr != null) {
            sb.append(indent).append("  Ensures:\n");
            sb.append(ensuresExpr.dump(indent + "    "));
        }

        sb.append(indent).append("  Uses:\n");
        if (usesParams != null) {
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

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}