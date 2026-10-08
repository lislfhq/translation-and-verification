import java.io.InputStreamReader;

%%

%class FunnyLexer
%public
%type TokenType
%line
%column

%{
    private void reportError(String msg) {
        System.err.printf("Лексическая ошибка [%d:%d]: %s ('%s')%n", 
                yyline + 1, yycolumn + 1, msg, yytext());
    }

    public int getLine() {
        return yyline + 1;
    }

    public int getColumn() {
        return yycolumn + 1;
    }
%}

DIGIT     = [0-9]
NO_ZERO   = [1-9]
LETTER    = [A-Za-z_]
ALNUM     = [A-Za-z0-9_]

IDENT     = {LETTER}{ALNUM}*
INT_LIT   = 0|({NO_ZERO}{DIGIT}*)
BAD_ALNUM = {DIGIT}+{LETTER}{ALNUM}*
WS        = [ \t\r\n]+

%%

{WS}        { /* пропуск */ }
"//".*      { /* пропуск комментариев */ }

"function"  { return TokenType.TOK_KW_FUNCTION; }
"returns"   { return TokenType.TOK_KW_RETURNS; }
"while"     { return TokenType.TOK_KW_WHILE; }
"if"        { return TokenType.TOK_KW_IF; }
"else"      { return TokenType.TOK_KW_ELSE; }
"assert"    { return TokenType.TOK_KW_ASSERT; }
"assume"    { return TokenType.TOK_KW_ASSUME; }
"invariant" { return TokenType.TOK_KW_INVARIANT; }
"length"    { return TokenType.TOK_KW_LENGTH; }
"int"       { return TokenType.TOK_KW_INT; }
"uses"      { return TokenType.TOK_KW_USES; }
"requires"  { return TokenType.TOK_KW_REQUIRES; }
"ensures"   { return TokenType.TOK_KW_ENSURES; }

{BAD_ALNUM} {
    reportError("Идентификатор не может начинаться с цифры");
    return TokenType.TOK_ERROR;
}

{INT_LIT}   { return TokenType.TOK_INT_LIT; }
{IDENT}     { return TokenType.TOK_IDENT; }

"=="        { return TokenType.TOK_EQ; }
"!="        { return TokenType.TOK_NEQ; }
"<="        { return TokenType.TOK_LEQ; }
">="        { return TokenType.TOK_GEQ; }

"="         { return TokenType.TOK_ASSIGN; }
"+"         { return TokenType.TOK_PLUS; }
"-"         { return TokenType.TOK_MINUS; }
"*"         { return TokenType.TOK_STAR; }
"/"         { return TokenType.TOK_SLASH; }
"%"         { return TokenType.TOK_PERCENT; }
"<"         { return TokenType.TOK_LT; }
">"         { return TokenType.TOK_GT; }
"("         { return TokenType.TOK_LPAREN; }
")"         { return TokenType.TOK_RPAREN; }
"{"         { return TokenType.TOK_LBRACE; }
"}"         { return TokenType.TOK_RBRACE; }
"["         { return TokenType.TOK_LBRACKET; }
"]"         { return TokenType.TOK_RBRACKET; }
":"         { return TokenType.TOK_COLON; }
";"         { return TokenType.TOK_SEMI; }
","         { return TokenType.TOK_COMMA; }

.           {
    reportError("Неизвестный или недопустимый символ");
    return TokenType.TOK_ERROR;
}