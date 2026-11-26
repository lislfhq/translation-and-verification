package ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FuncCallNode extends ASTNode {
    public final String name;
    public final List<ASTNode> args;
    public List<Type> resultTypes = new ArrayList<>();

    public FuncCallNode(String name, List<ASTNode> args, int line, int column) {
        super(line, column);
        this.name = name;
        this.args = args;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        String typeStr = "";
        if (exprType == Type.TUPLE && resultTypes != null && !resultTypes.isEmpty()) {
            String joined = resultTypes.stream().map(Enum::name).collect(Collectors.joining(", "));
            typeStr = " [(" + joined + ")]";
        } else if (exprType != Type.UNKNOWN) {
            typeStr = " [" + exprType + "]";
        }

        sb.append(indent).append("FuncCall: ").append(name).append(typeStr).append("\n");
        if (args != null) {
            for (ASTNode arg : args) {
                if (arg != null) {
                    sb.append(arg.dump(indent + "  "));
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}