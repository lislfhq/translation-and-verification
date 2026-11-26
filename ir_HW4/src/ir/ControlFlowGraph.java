package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ControlFlowGraph {
    public final String funcName;
    public final List<BasicBlock> blocks = new ArrayList<>();

    public ControlFlowGraph(String funcName) {
        this.funcName = funcName;
    }

    /** Разбивает линейный список инструкций функции на базовые блоки и строит рёбра CFG */
    public static ControlFlowGraph build(IRFunction func) {
        ControlFlowGraph cfg = new ControlFlowGraph(func.name);
        List<IRInstruction> code = func.instructions;
        if (code.isEmpty()) {
            return cfg;
        }

        // 1. Поиск лидеров (leaders)
        Set<Integer> leaders = new HashSet<>();
        leaders.add(0);

        Map<String, Integer> labelToIndex = new HashMap<>();
        for (int i = 0; i < code.size(); i++) {
            IRInstruction ins = code.get(i);
            if (ins.op == IRInstruction.OpCode.LABEL) {
                labelToIndex.put(ins.targetLabel.name, i);
                leaders.add(i);
            }
        }

        for (int i = 0; i < code.size(); i++) {
            IRInstruction ins = code.get(i);
            if (ins.op == IRInstruction.OpCode.GOTO
                    || ins.op == IRInstruction.OpCode.IF_FALSE
                    || ins.op == IRInstruction.OpCode.RETURN) {
                if (i + 1 < code.size()) {
                    leaders.add(i + 1);
                }
            }
        }

        List<Integer> sortedLeaders = new ArrayList<>(leaders);
        sortedLeaders.sort(Integer::compareTo);

        // 2. Формирование базовых блоков
        Map<String, BasicBlock> labelToBlock = new HashMap<>();
        for (int b = 0; b < sortedLeaders.size(); b++) {
            int start = sortedLeaders.get(b);
            int end = (b + 1 < sortedLeaders.size()) ? sortedLeaders.get(b + 1) : code.size();

            String blockName = "B" + b;
            if (code.get(start).op == IRInstruction.OpCode.LABEL) {
                blockName = "B" + b + "_" + code.get(start).targetLabel.name;
            }
            BasicBlock block = new BasicBlock(b, blockName);

            for (int i = start; i < end; i++) {
                IRInstruction ins = code.get(i);
                if (ins.op == IRInstruction.OpCode.LABEL) {
                    labelToBlock.put(ins.targetLabel.name, block);
                }
                block.instructions.add(ins);
            }
            cfg.blocks.add(block);
        }

        // 3. Построение переходов между блоками
        for (int b = 0; b < cfg.blocks.size(); b++) {
            BasicBlock block = cfg.blocks.get(b);
            if (block.instructions.isEmpty()) continue;

            IRInstruction last = block.instructions.get(block.instructions.size() - 1);
            if (last.op == IRInstruction.OpCode.GOTO) {
                BasicBlock target = labelToBlock.get(last.targetLabel.name);
                if (target != null) block.addSuccessor(target);
            } else if (last.op == IRInstruction.OpCode.IF_FALSE) {
                BasicBlock target = labelToBlock.get(last.targetLabel.name);
                if (target != null) block.addSuccessor(target);
                if (b + 1 < cfg.blocks.size()) {
                    block.addSuccessor(cfg.blocks.get(b + 1));
                }
            } else if (last.op != IRInstruction.OpCode.RETURN) {
                if (b + 1 < cfg.blocks.size()) {
                    block.addSuccessor(cfg.blocks.get(b + 1));
                }
            }
        }

        return cfg;
    }

    /** Собирает инструкции обратно из всех базовых блоков */
    public List<IRInstruction> flattenInstructions() {
        List<IRInstruction> result = new ArrayList<>();
        for (BasicBlock b : blocks) {
            result.addAll(b.instructions);
        }
        return result;
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CFG_").append(funcName).append(" {\n");
        sb.append("  node [shape=box, fontname=\"monospace\"];\n");
        for (BasicBlock b : blocks) {
            StringBuilder label = new StringBuilder();
            label.append(b.name).append("\\l");
            for (IRInstruction ins : b.instructions) {
                label.append(ins.toString().replace("\"", "\\\"")).append("\\l");
            }
            sb.append("  \"").append(b.name).append("\" [label=\"").append(label).append("\"];\n");
            for (BasicBlock succ : b.successors) {
                sb.append("  \"").append(b.name).append("\" -> \"").append(succ.name).append("\";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CFG for function '").append(funcName).append("' ===\n");
        for (BasicBlock b : blocks) {
            sb.append(b);
        }
        return sb.toString();
    }
}