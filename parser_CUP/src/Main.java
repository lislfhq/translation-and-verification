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
                String actualAst = root.dump("") + "\n";
                System.out.println("=== Сгенерированное AST ===");
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
                        System.exit(2);
                    }
                }
            } else {
                System.out.println("Парсинг завершился, но AST пустое.");
            }

        } catch (Exception e) {
            System.err.println("Ошибка во время работы парсера: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}