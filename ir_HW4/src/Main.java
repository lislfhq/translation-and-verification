import ast.*;
import ir.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java_cup.runtime.Symbol;
import lexer.FunnyLexer;
import opt.*;
import parser.FunnyParser;
import regalloc.*;
import semantic.SemanticAnalyzer;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Использование: java Main <input_file> [options]");
            System.err.println("Опции:");
            System.err.println("  --ast-only        Вывести только аннотированное AST (режим HW3)");
            System.err.println("  --no-opt          Отключить все оптимизации");
            System.err.println("  --no-const        Отключить распространение/свёртку констант");
            System.err.println("  --no-alg          Отключить алгебраические упрощения");
            System.err.println("  --no-cse          Отключить локальный CSE");
            System.err.println("  --no-dce          Отключить локальный DCE");
            System.err.println("  --dot-cfg         Вывести CFG в формате Graphviz DOT");
            System.err.println("  --dot-ig          Вывести граф конфликтов в формате Graphviz DOT");
            System.err.println("  --run a,b,...     Выполнить главную функцию до и после оптимизаций");
            System.exit(1);
        }

        String inputFile = args[0];
        String expectedAstFile = null;
        boolean astOnly = false;
        boolean dotCfg = false;
        boolean dotIg = false;
        String runArgsStr = null;
        OptimizationConfig optConfig = new OptimizationConfig();

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--ast-only":
                    astOnly = true;
                    break;
                case "--no-opt":
                    optConfig.disableAll();
                    break;
                case "--no-const":
                    optConfig.enableConstProp = false;
                    break;
                case "--no-alg":
                    optConfig.enableAlgebraic = false;
                    break;
                case "--no-cse":
                    optConfig.enableCSE = false;
                    break;
                case "--no-dce":
                    optConfig.enableDCE = false;
                    break;
                case "--dot-cfg":
                    dotCfg = true;
                    break;
                case "--dot-ig":
                    dotIg = true;
                    break;
                case "--run":
                    if (i + 1 < args.length) {
                        runArgsStr = args[++i];
                    }
                    break;
                default:
                    if (!arg.startsWith("--") && expectedAstFile == null) {
                        expectedAstFile = arg;
                        astOnly = true;
                    }
                    break;
            }
        }

        try {
            Reader reader = new FileReader(inputFile);
            FunnyLexer lexer = new FunnyLexer(reader);
            FunnyParser parser = new FunnyParser(lexer);
            Symbol parseTree = parser.parse();

            ASTNode root = (ASTNode) parseTree.value;
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);

            if (analyzer.hasErrors()) {
                analyzer.printErrors();
                System.exit(1);
            }

            if (astOnly) {
                String actualAst = root.dump("");
                System.out.print(actualAst);
                if (expectedAstFile != null) {
                    String expectedAst = Files.readString(Paths.get(expectedAstFile));
                    if (!actualAst.trim().equals(expectedAst.trim())) {
                        System.err.println("[ОШИБКА] Сгенерированное AST не совпадает с эталонным файлом: " + expectedAstFile);
                        System.exit(1);
                    }
                }
                System.exit(0);
            }

            // === Этап HW4: генерация IR, CFG, оптимизации, граф конфликтов и раскраска ===
            IRGenerator irGenBefore = new IRGenerator();
            List<IRFunction> funcsBefore = irGenBefore.generate(root);

            IRGenerator irGenAfter = new IRGenerator();
            List<IRFunction> funcsAfter = irGenAfter.generate(root);

            LocalOptimizer optimizer = new LocalOptimizer(optConfig);
            for (IRFunction fn : funcsAfter) {
                optimizer.optimize(fn);
            }

            System.out.println("=== IR ДО ОПТИМИЗАЦИЙ ===");
            for (IRFunction fn : funcsBefore) {
                System.out.print(fn);
            }

            System.out.println("=== IR ПОСЛЕ ОПТИМИЗАЦИЙ (" + optConfig + ") ===");
            for (IRFunction fn : funcsAfter) {
                System.out.print(fn);
            }

            for (int i = 0; i < funcsAfter.size(); i++) {
                IRFunction before = funcsBefore.get(i);
                IRFunction after = funcsAfter.get(i);

                if (dotCfg) {
                    System.out.println(after.cfg.toDot());
                } else {
                    System.out.print(after.cfg);
                }

                InterferenceGraph igBefore = InterferenceGraph.build(before);
                GreedyColoring colBefore = GreedyColoring.color(igBefore);

                InterferenceGraph igAfter = InterferenceGraph.build(after);
                GreedyColoring colAfter = GreedyColoring.color(igAfter);

                if (dotIg) {
                    System.out.println(igAfter.toDot());
                } else {
                    System.out.print(igAfter);
                }
                System.out.print(colAfter);

                long tempsBefore = before.instructions.stream()
                        .flatMap(ins -> ins.getWrittenVars().stream())
                        .filter(v -> v.startsWith("t") && v.length() > 1 && Character.isDigit(v.charAt(1)))
                        .distinct().count();
                long tempsAfter = after.instructions.stream()
                        .flatMap(ins -> ins.getWrittenVars().stream())
                        .filter(v -> v.startsWith("t") && v.length() > 1 && Character.isDigit(v.charAt(1)))
                        .distinct().count();

                System.out.println("=== МЕТРИКИ (" + after.name + ") ===");
                System.out.println("  Инструкций IR:      " + before.instructions.size() + " -> " + after.instructions.size());
                System.out.println("  Базовых блоков CFG: " + before.cfg.blocks.size() + " -> " + after.cfg.blocks.size());
                System.out.println("  Временных (tN):     " + tempsBefore + " -> " + tempsAfter);
                System.out.println("  Цветов раскраски:   " + colBefore.numColors + " -> " + colAfter.numColors);
            }

            // Проверка эквивалентности выполнения до и после оптимизаций
            if (runArgsStr != null && !funcsAfter.isEmpty()) {
                List<Object> callArgs = parseRunArgs(runArgsStr);
                String entryFunc = funcsAfter.get(funcsAfter.size() - 1).name;
                IRInterpreter interpBefore = new IRInterpreter(funcsBefore);
                IRInterpreter interpAfter = new IRInterpreter(funcsAfter);

                List<Object> resBefore = interpBefore.execute(entryFunc, callArgs);
                List<Object> resAfter = interpAfter.execute(entryFunc, callArgs);

                System.out.println("=== ПРОВЕРКА ВЫПОЛНЕНИЯ (" + entryFunc + "(" + runArgsStr + ")) ===");
                System.out.println("  Результат ДО:    " + resBefore);
                System.out.println("  Результат ПОСЛЕ: " + resAfter);

                if (!resBefore.equals(resAfter)) {
                    System.err.println("[ОШИБКА] Результаты выполнения до и после оптимизаций различаются!");
                    System.exit(1);
                }
            }

            System.exit(0);
        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            System.exit(1);
        }
    }

    private static List<Object> parseRunArgs(String str) {
        List<Object> list = new ArrayList<>();
        if (str == null || str.trim().isEmpty()) return list;
        for (String part : str.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(Integer.parseInt(trimmed));
            }
        }
        return list;
    }
}