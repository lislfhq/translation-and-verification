import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java_cup.runtime.Symbol;

import lexer.FunnyLexer;
import parser.FunnyParser;
import ast.ASTNode;
import semantic.SemanticAnalyzer;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Использование: java Main <исходный_файл> [эталонный_ast_файл]");
            System.exit(1);
        }

        String sourceFile = args[0];
        String expectedAstFile = (args.length > 1) ? args[1] : null;

        try {
            FunnyLexer lexer = new FunnyLexer(new FileReader(sourceFile));
            FunnyParser parser = new FunnyParser(lexer);

            Symbol parseResult = parser.parse();
            ASTNode root = (ASTNode) parseResult.value;

            if (root != null) {
                // ====================================================
                // ЗАПУСК СЕМАНТИЧЕСКОГО АНАЛИЗА (HW3)
                // ====================================================
                SemanticAnalyzer analyzer = new SemanticAnalyzer();
                analyzer.analyze(root);

                if (analyzer.hasErrors()) {
                    System.err.println("\n=== Найдены семантические ошибки ===");
                    analyzer.printErrors();
                    // По заданию стараемся не падать после первой ошибки,
                    // поэтому просто выводим их на экран.
                    // Если хотите прерывать компиляцию при ошибках, раскомментируйте строку ниже:
                    // System.exit(1);
                } else {
                    System.out.println("\n[УСПЕХ] Семантический анализ пройден без ошибок.");
                }
                // ====================================================

                // Обратите внимание: теперь root.dump("") вернет АННОТИРОВАННОЕ AST (с типами)
                String actualAst = root.dump("") + "\n";
                System.out.println("\n=== Сгенерированное (Аннотированное) AST ===");
                System.out.print(actualAst);

                if (expectedAstFile != null) {
                    String expectedAst = Files.readString(Paths.get(expectedAstFile));

                    String normalizedActual = actualAst.replace("\r\n", "\n").trim();
                    String normalizedExpected = expectedAst.replace("\r\n", "\n").trim();

                    if (normalizedActual.equals(normalizedExpected)) {
                        System.out.println("\n[УСПЕХ] AST полностью совпадает с эталонным снимком (snapshot)");
                    } else {
                        System.err.println("\n[ОШИБКА] AST не совпадает с эталоном!");
                        System.err.println("--- Ожидалось ---:\n" + normalizedExpected);
                        System.err.println("--- Получено ---:\n" + normalizedActual);

                        // Если старые эталоны были без типов, они сейчас не совпадут.
                        // Нужно будет перезаписать эталонные файлы для HW3.
                        System.exit(2);
                    }
                }
            } else {
                System.out.println("Парсинг завершился, но AST пустое.");
            }

        } catch (Exception e) {
            System.err.println("Ошибка во время работы парсера или анализатора: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}