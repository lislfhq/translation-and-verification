package ast;

import java.util.List;

public class TupleAssignNode extends ASTNode {
    public final List<String> targets;
    public final FuncCallNode call;

    public TupleAssignNode(List<String> targets, FuncCallNode call, int line, int column) {
        super(line, column);
        this.targets = targets;
        this.call = call;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String dump(String indent) {
        return indent + "TupleAssign (" + String.join(", ", targets) + ")\n"
                + call.dump(indent + "  ");
    }
}