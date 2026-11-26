package regalloc;

import java.util.*;

public class GreedyColoring {
    public final Map<String, Integer> colors;
    public final int numColors;

    private GreedyColoring(Map<String, Integer> colors, int numColors) {
        this.colors = colors;
        this.numColors = numColors;
    }

    /** Жадная раскраска узлов графа конфликтов с упорядочением по убыванию степени */
    public static GreedyColoring color(InterferenceGraph ig) {
        List<String> nodes = new ArrayList<>(ig.adj.keySet());
        nodes.sort((a, b) -> Integer.compare(ig.adj.get(b).size(), ig.adj.get(a).size()));

        Map<String, Integer> colorMap = new LinkedHashMap<>();
        int maxColor = -1;

        for (String node : nodes) {
            Set<Integer> neighborColors = new HashSet<>();
            for (String neighbor : ig.adj.getOrDefault(node, Collections.emptySet())) {
                if (colorMap.containsKey(neighbor)) {
                    neighborColors.add(colorMap.get(neighbor));
                }
            }

            int color = 0;
            while (neighborColors.contains(color)) {
                color++;
            }
            colorMap.put(node, color);
            if (color > maxColor) {
                maxColor = color;
            }
        }

        return new GreedyColoring(colorMap, maxColor + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Colors used (JVM local slots): ").append(numColors).append("\n");
        for (Map.Entry<String, Integer> e : colors.entrySet()) {
            sb.append("  ").append(e.getKey()).append(" -> r").append(e.getValue()).append("\n");
        }
        return sb.toString();
    }
}