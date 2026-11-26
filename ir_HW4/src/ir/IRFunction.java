package ir;

import ast.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IRFunction {
    public final String name;
    public final List<Operand> params = new ArrayList<>();
    public final List<Operand> returns = new ArrayList<>();
    public final Map<String, Type> localTypes = new LinkedHashMap<>();
    public List<IRInstruction> instructions = new ArrayList<>();
    public ControlFlowGraph cfg;

    public IRFunction(String name) {
        this.name = name;
    }

    public void rebuildCFG() {
        this.cfg = ControlFlowGraph.build(this);
    }

    public void syncInstructionsFromCFG() {
        if (this.cfg != null) {
            this.instructions = this.cfg.flattenInstructions();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String paramStr = params.stream()
                .map(p -> p.name + ": " + p.type)
                .collect(Collectors.joining(", "));
        String retStr = returns.stream()
                .map(r -> r.name + ": " + r.type)
                .collect(Collectors.joining(", "));
        sb.append("function ").append(name).append("(").append(paramStr).append(") returns (").append(retStr).append(")\n");
        for (IRInstruction ins : instructions) {
            sb.append(ins).append("\n");
        }
        return sb.toString();
    }
}