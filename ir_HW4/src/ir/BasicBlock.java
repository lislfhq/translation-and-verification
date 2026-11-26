package ir;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BasicBlock {
    public final int id;
    public final String name;
    public final List<IRInstruction> instructions = new ArrayList<>();
    public final List<BasicBlock> predecessors = new ArrayList<>();
    public final List<BasicBlock> successors = new ArrayList<>();

    // Множества для анализа живости (Liveness)
    public Set<String> use = new LinkedHashSet<>();
    public Set<String> def = new LinkedHashSet<>();
    public Set<String> liveIn = new LinkedHashSet<>();
    public Set<String> liveOut = new LinkedHashSet<>();

    public BasicBlock(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addSuccessor(BasicBlock succ) {
        if (!successors.contains(succ)) {
            successors.add(succ);
        }
        if (!succ.predecessors.contains(this)) {
            succ.predecessors.add(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block ").append(name).append(" (preds: ");
        for (int i = 0; i < predecessors.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(predecessors.get(i).name);
        }
        sb.append("; succs: ");
        for (int i = 0; i < successors.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(successors.get(i).name);
        }
        sb.append(")\n");
        for (IRInstruction ins : instructions) {
            sb.append(ins).append("\n");
        }
        return sb.toString();
    }
}