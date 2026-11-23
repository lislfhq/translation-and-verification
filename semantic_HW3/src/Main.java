import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import parser.FunnyParser;
import lexer.FunnyLexer;
import ast.ASTNode;
import semantic.SemanticAnalyzer;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Использование: java Main <файл_кода> [файл_ast_для_сверки]");
            System.exit(1);
        }

        try {
            FunnyLexer lexer = new FunnyLexer(new FileReader(args[0]));
            FunnyParser parser = new FunnyParser(lexer);
            ASTNode root = (ASTNode) parser.parse().value;

            if (root == null) {
                System.err.println("Ошибка: Парсер вернул пустое дерево.");
                System.exit(1);
            }

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);

            boolean failed = false;
            if (analyzer.hasErrors()) {
                System.err.println("\n=== Найдены семантические ошибки ===");
                analyzer.printErrors();
                failed = true;
            } else {
                System.err.println("\n[УСПЕХ] Семантический анализ пройден без ошибок.");
            }

            String astDump = root.dump("");
            System.err.println("=== Сгенерированное (Аннотированное) AST ===");
            System.out.print(astDump);

            if (args.length == 2) {
                String snapshotPath = args[1];
                String expectedAst = new String(Files.readAllBytes(Paths.get(snapshotPath))).trim();
                String actualAst = astDump.trim();

                if (expectedAst.equals(actualAst)) {
                    System.err.println("\n[УСПЕХ] AST полностью совпадает с эталонным снимком (snapshot)");
                } else {
                    System.err.println("\n[ОШИБКА] AST не совпадает с эталоном!");
                    System.err.println("--- Ожидалось ---:\n" + expectedAst);
                    System.err.println("--- Получено ---:\n" + actualAst);
                    failed = true;
                }
            }

            if (failed) {
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Ошибка во время работы парсера или анализатора: " + e.getMessage());
            System.exit(1);
        }
    }
}