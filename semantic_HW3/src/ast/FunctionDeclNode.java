package ast;
import java.util.List;

public class FunctionDeclNode extends ASTNode {
    public final String name;
    public final Type returnType; // Возвращаемый тип
    public final List<VarDeclNode> parameters; // Аргументы функции (их имена и типы)
    public final List<ASTNode> body;

    public FunctionDeclNode(String name, Type returnType, List<VarDeclNode> parameters, List<ASTNode> body, int l, int c) {
        super(l, c);
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("FunctionDeclNode: ").append(name)
                .append(" -> ").append(returnType).append("\n");

        sb.append(indent).append("  Params:\n");
        if (parameters != null) {
            for (VarDeclNode param : parameters) {
                sb.append(param.dump(indent + "    "));
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