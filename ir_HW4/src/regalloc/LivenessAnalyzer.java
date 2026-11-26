package regalloc;

import ir.BasicBlock;
import ir.ControlFlowGraph;
import ir.IRInstruction;
import java.util.LinkedHashSet;
import java.util.Set;

public class LivenessAnalyzer {

    public static void computeLiveness(ControlFlowGraph cfg) {
        if (cfg == null) return;

        // 1. Вычисляем use[B] и def[B] для каждого блока
        for (BasicBlock block : cfg.blocks) {
            block.use.clear();
            block.def.clear();
            block.liveIn.clear();
            block.liveOut.clear();

            for (IRInstruction ins : block.instructions) {
                for (String r : ins.getReadVars()) {
                    if (!block.def.contains(r)) {
                        block.use.add(r);
                    }
                }
                for (String w : ins.getWrittenVars()) {
                    block.def.add(w);
                }
            }
        }

        // 2. Итеративный анализ потока данных (назад по CFG)
        boolean changed;
        do {
            changed = false;
            for (int i = cfg.blocks.size() - 1; i >= 0; i--) {
                BasicBlock block = cfg.blocks.get(i);

                Set<String> newOut = new LinkedHashSet<>();
                for (BasicBlock succ : block.successors) {
                    newOut.addAll(succ.liveIn);
                }

                Set<String> newIn = new LinkedHashSet<>(block.use);
                Set<String> outMinusDef = new LinkedHashSet<>(newOut);
                outMinusDef.removeAll(block.def);
                newIn.addAll(outMinusDef);

                if (!newOut.equals(block.liveOut) || !newIn.equals(block.liveIn)) {
                    block.liveOut = newOut;
                    block.liveIn = newIn;
                    changed = true;
                }
            }
        } while (changed);
    }
}