all: compile

compile:
	java -cp "lib/jflex.jar:lib/java-cup-11b.jar" jflex.Main -d src/lexer src/lexer/lexer.jflex
	java -cp "lib/java-cup-11b.jar" java_cup.Main -destdir src/parser -parser FunnyParser src/parser/parser.cup
	javac -cp "lib/java-cup-11b.jar:src" src/ast/*.java src/semantic/*.java src/lexer/*.java src/parser/*.java src/Main.java

test: compile
	bash run_tests.sh

snapshots: compile
	@for f in tests/test_*.txt; do \
		case "$$f" in *neg*) continue ;; esac; \
		java -cp "lib/java-cup-11b.jar:src" Main "$$f" > "$${f%.txt}.ast"; \
	done

clean:
	rm -f src/lexer/FunnyLexer.java src/parser/FunnyParser.java src/parser/sym.java
	find src -name "*.class" -type f -delete