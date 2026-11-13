package lexer;

import java_cup.runtime.Symbol;
import parser.sym;

%%

%class FunnyLexer
%public
%cup
%line
%column

%{
    private Symbol symbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn + 1);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }

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

"function"  { return symbol(sym.KW_FUNCTION); }
"returns"   { return symbol(sym.KW_RETURNS); }
"while"     { return symbol(sym.KW_WHILE); }
"if"        { return symbol(sym.KW_IF); }
"else"      { return symbol(sym.KW_ELSE); }
"assert"    { return symbol(sym.KW_ASSERT); }
"assume"    { return symbol(sym.KW_ASSUME); }
"invariant" { return symbol(sym.KW_INVARIANT); }
"length"    { return symbol(sym.KW_LENGTH); }
"int"       { return symbol(sym.KW_INT); }
"uses"      { return symbol(sym.KW_USES); }
"requires"  { return symbol(sym.KW_REQUIRES); }
"ensures"   { return symbol(sym.KW_ENSURES); }

{BAD_ALNUM} {
    reportError("Идентификатор не может начинаться с цифры");
    return symbol(sym.error);
}

{INT_LIT}   { return symbol(sym.INT_LIT, Integer.parseInt(yytext())); }
{IDENT}     { return symbol(sym.ID, yytext()); }

"=="        { return symbol(sym.EQ); }
"!="        { return symbol(sym.NEQ); }
"<="        { return symbol(sym.LEQ); }
">="        { return symbol(sym.GEQ); }

"="         { return symbol(sym.ASSIGN); }
"+"         { return symbol(sym.PLUS); }
"-"         { return symbol(sym.MINUS); }
"*"         { return symbol(sym.STAR); }
"/"         { return symbol(sym.SLASH); }
"%"         { return symbol(sym.PERCENT); }
"<"         { return symbol(sym.LT); }
">"         { return symbol(sym.GT); }
"("         { return symbol(sym.LPAREN); }
")"         { return symbol(sym.RPAREN); }
"{"         { return symbol(sym.LBRACE); }
"}"         { return symbol(sym.RBRACE); }
"["         { return symbol(sym.LBRACKET); }
"]"         { return symbol(sym.RBRACKET); }
":"         { return symbol(sym.COLON); }
";"         { return symbol(sym.SEMI); }
","         { return symbol(sym.COMMA); }

.           {
    reportError("Неизвестный или недопустимый символ");
    return symbol(sym.error);
}