package regalloc;

import ir.BasicBlock;
import ir.IRFunction;
import ir.IRInstruction;
import ir.Operand;
import java.util.*;

public class InterferenceGraph {
    public final String funcName;
    public final Map<String, Set<String>> adj = new LinkedHashMap<>();

    public InterferenceGraph(String funcName) {
        this.funcName = funcName;
    }

    public void addNode(String var) {
        adj.computeIfAbsent(var, k -> new LinkedHashSet<>());
    }

    public void addEdge(String u, String v) {
        if (u == null || v == null || u.equals(v)) return;
        addNode(u);
        addNode(v);
        adj.get(u).add(v);
        adj.get(v).add(u);
    }

    /** Строит граф конфликтов по результатам анализа живости */
    public static InterferenceGraph build(IRFunction fn) {
        fn.rebuildCFG();
        LivenessAnalyzer.computeLiveness(fn.cfg);

        InterferenceGraph ig = new InterferenceGraph(fn.name);

        // Входные параметры конфликтуют друг с другом в точке входа функции
        for (Operand p : fn.params) {
            ig.addNode(p.name);
        }
        for (int i = 0; i < fn.params.size(); i++) {
            for (int j = i + 1; j < fn.params.size(); j++) {
                ig.addEdge(fn.params.get(i).name, fn.params.get(j).name);
            }
        }

        // Регистрируем все переменные и временные из инструкций
        for (BasicBlock block : fn.cfg.blocks) {
            for (IRInstruction ins : block.instructions) {
                for (String w : ins.getWrittenVars()) ig.addNode(w);
                for (String r : ins.getReadVars()) ig.addNode(r);
            }
        }

        // Обходим инструкции блока в обратном порядке и добавляем рёбра конфликта
        for (BasicBlock block : fn.cfg.blocks) {
            Set<String> live = new LinkedHashSet<>(block.liveOut);

            for (int i = block.instructions.size() - 1; i >= 0; i--) {
                IRInstruction ins = block.instructions.get(i);
                List<String> written = ins.getWrittenVars();

                for (String defVar : written) {
                    for (String liveVar : live) {
                        // Для обычного копирования dst = src не добавляем конфликт между dst и src
                        if (ins.op == IRInstruction.OpCode.ASSIGN && ins.src1 != null && ins.src1.isVar()
                                && ins.src1.name.equals(liveVar)) {
                            continue;
                        }
                        ig.addEdge(defVar, liveVar);
                    }
                }

                live.removeAll(written);
                live.addAll(ins.getReadVars());
            }
        }

        return ig;
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph Interference_").append(funcName).append(" {\n");
        sb.append("  node [shape=circle, fontname=\"monospace\"];\n");
        Set<String> printedEdges = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : adj.entrySet()) {
            String u = entry.getKey();
            sb.append("  \"").append(u).append("\";\n");
            for (String v : entry.getValue()) {
                String edgeKey = u.compareTo(v) < 0 ? u + "--" + v : v + "--" + u;
                if (printedEdges.add(edgeKey)) {
                    sb.append("  \"").append(u).append("\" -- \"").append(v).append("\";\n");
                }
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Interference Graph for '").append(funcName).append("' ===\n");
        for (Map.Entry<String, Set<String>> entry : adj.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" <-> ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}