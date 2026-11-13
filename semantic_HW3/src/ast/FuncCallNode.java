package ast;
import java.util.List;

public class FuncCallNode extends ASTNode {
    public final String name;
    public final List<ASTNode> args;

    public FuncCallNode(String name, List<ASTNode> args, int line, int column) {
        super(line, column);
        this.name = name;
        this.args = args;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        String typeStr = (exprType != Type.UNKNOWN) ? " [" + exprType + "]" : "";
        sb.append(indent).append("FuncCall: ").append(name).append(typeStr).append("\n");
        if (args != null) {
            for (ASTNode arg : args) {
                sb.append(arg.dump(indent + "  "));
            }
        }
        return sb.toString();
    }
}