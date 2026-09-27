CC = gcc
FLEX = flex

all: funny_lexer

funny_lexer: lex.yy.c
	$(CC) lex.yy.c -o funny_lexer

lex.yy.c: lexer.l
	$(FLEX) lexer.l

clean:
	rm -f lex.yy.c funny_lexer

test: funny_lexer
	./funny_lexer --tokens < test_funny.txt
