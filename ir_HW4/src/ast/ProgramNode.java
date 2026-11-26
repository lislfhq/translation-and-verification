package ast;

import java.util.List;

public class ProgramNode extends ASTNode {
    public final List<FunctionDeclNode> functions;

    public ProgramNode(List<FunctionDeclNode> functions, int line, int column) {
        super(line, column);
        this.functions = functions;
    }

    @Override
    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Program:\n");
        if (functions != null) {
            for (FunctionDeclNode fn : functions) {
                sb.append(fn.dump(indent + "  "));
            }
        }
        return sb.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}