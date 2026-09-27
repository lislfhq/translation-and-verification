%{
#include <stdio.h>

typedef struct {
    int token_type;
    char* lexeme;
    int line;
    int column;
} Token;

int current_line = 1;
int current_column = 1;

typedef enum {
	TOK_EOF = 0,
 	TOK_ERROR,

	TOK_KW_FUNCTION,
    	TOK_KW_RETURNS,
    	TOK_KW_WHILE,
    	TOK_KW_IF,
    	TOK_KW_ELSE,
    	TOK_KW_ASSERT,
    	TOK_KW_ASSUME,
    	TOK_KW_INVARIANT,
    	TOK_KW_LENGTH,
    	TOK_KW_INT,
	TOK_KW_USES,      
     TOK_KW_REQUIRES,  
     TOK_KW_ENSURES,

	TOK_IDENT,
    	TOK_INT_LIT,

	TOK_PLUS,
    	TOK_MINUS,
    	TOK_STAR,
    	TOK_SLASH,
    	TOK_PERCENT,
    	TOK_ASSIGN,
    	TOK_EQ,
    	TOK_NEQ,
    	TOK_LT,
    	TOK_GT,
    	TOK_LEQ,
    	TOK_GEQ,
	
	TOK_LPAREN,
	TOK_RPAREN,
	TOK_LBRACE,
	TOK_RBRACE,
	TOK_LBRACKET,
	TOK_RBRACKET,
	TOK_SEMI,
	TOK_COMMA,
	TOK_COLON
} TokenType;

void update_coords() {
	for (int i = 0; yytext[i] != '\0'; i++) {
		if (yytext[i] == '\n') {
			current_line++;
			current_column = 1;
		} else {
			current_column++;
		}
	}
}

void report_error(const char* msg) {
    fprintf(stderr, "Лексическая ошибка [%d:%d]: %s ('%s')\n", 
            current_line, current_column, msg, yytext);
}


%}

%option noyywrap

DIGIT		[0-9]
NO_ZERO	[1-9]
LETTER	[A-Za-z_]
ALNUM		[A-Za-z0-9_]

IDENT		{LETTER}{ALNUM}*
INT_LIT	0|({NO_ZERO}{DIGIT}*)
BAD_ALNUM	{DIGIT}+{LETTER}{ALNUM}*
WS		[ \t\r\n]+

%%

%{
	
%}

{WS}		{ update_coords(); /* пропуск */ }

"//".*	{ update_coords(); /* пропуск комментариев */ }

"if"		{ update_coords(); return TOK_KW_IF; }
"else"		{ update_coords(); return TOK_KW_ELSE; }
"while"		{ update_coords(); return TOK_KW_WHILE; }
"returns"   { update_coords(); return TOK_KW_RETURNS; }
"uses"      { update_coords(); return TOK_KW_USES; }
"requires"  { update_coords(); return TOK_KW_REQUIRES; }
"ensures"   { update_coords(); return TOK_KW_ENSURES; }
"int"       { update_coords(); return TOK_KW_INT; }
"assert"    { update_coords(); return TOK_KW_ASSERT; }
"assume"    { update_coords(); return TOK_KW_ASSUME; }

{BAD_ALNUM} {
	update_coords();
	report_error("Идентификатор не может начинаться с цифры");
	return TOK_ERROR;
}

{INT_LIT}   { update_coords(); return TOK_INT_LIT; }
{IDENT}     { update_coords(); return TOK_IDENT; }

"=="        { update_coords(); return TOK_EQ; }
"!="        { update_coords(); return TOK_NEQ; }
"<="        { update_coords(); return TOK_LEQ; }
">="        { update_coords(); return TOK_GEQ; }

"="         { update_coords(); return TOK_ASSIGN; }
"+"         { update_coords(); return TOK_PLUS; }
"-"         { update_coords(); return TOK_MINUS; }
"*"         { update_coords(); return TOK_STAR; }
"/"         { update_coords(); return TOK_SLASH; }
"<"         { update_coords(); return TOK_LT; }
">"         { update_coords(); return TOK_GT; }
"("         { update_coords(); return TOK_LPAREN; }
")"         { update_coords(); return TOK_RPAREN; }
"{"         { update_coords(); return TOK_LBRACE; }
"}"         { update_coords(); return TOK_RBRACE; }
"["         { update_coords(); return TOK_LBRACKET; }
"]"         { update_coords(); return TOK_RBRACKET; }
":"         { update_coords(); return TOK_COLON; }
";"         { update_coords(); return TOK_SEMI; }
","         { update_coords(); return TOK_COMMA; }

. 		  {
	update_coords();
	report_error("Неизвестный или недопустимый символ");
	return TOK_ERROR;
}

%%

int main(int argc, char** argv) {
	if (argc > 1 && strcmp(argv[1], "--tokens") == 0) {
	int tok;
	while ((tok = yylex()) != TOK_EOF) {
		printf("[%d:%d] TOK_TYPE=%d, LEXEME='%s'\n",
			current_line, current_column, tok, yytext);
	}
   } else {
		printf("Использование: %s --tokens < input.funny\n", argv[0]);
   }
   return 0;
}
