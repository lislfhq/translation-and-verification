import java.io.InputStreamReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--tokens")) {
            try {
                FunnyLexer lexer = new FunnyLexer(new InputStreamReader(System.in));
                TokenType tok;

                while ((tok = lexer.yylex()) != null) {
                    System.out.printf("[%d:%d] TOK_TYPE=%s, LEXEME='%s'%n",
                            lexer.getLine(), lexer.getColumn(), tok, lexer.yytext());
                }
            } catch (IOException e) {
                System.err.println("Ошибка при чтении файла: " + e.getMessage());
            }
        } else {
            System.out.println("Использование: java Main --tokens < input.funny");
        }
    }
}