JCUP_JAR = lib/java-cup-11b.jar
JFLEX_JAR = lib/jflex.jar
SRC_DIR = src

.PHONY: all compile clean snapshots test

all: compile

compile:
	java -cp "$(JFLEX_JAR):$(JCUP_JAR)" jflex.Main -d $(SRC_DIR)/lexer $(SRC_DIR)/lexer/lexer.jflex
	java -cp "$(JCUP_JAR)" java_cup.Main -destdir $(SRC_DIR)/parser -parser FunnyParser $(SRC_DIR)/parser/parser.cup
	javac -cp "$(JCUP_JAR):$(SRC_DIR)" $(SRC_DIR)/ast/*.java $(SRC_DIR)/semantic/*.java $(SRC_DIR)/ir/*.java $(SRC_DIR)/opt/*.java $(SRC_DIR)/regalloc/*.java $(SRC_DIR)/lexer/*.java $(SRC_DIR)/parser/*.java $(SRC_DIR)/Main.java

clean:
	rm -f $(SRC_DIR)/lexer/FunnyLexer.java $(SRC_DIR)/lexer/FunnyLexer.java~ $(SRC_DIR)/parser/FunnyParser.java $(SRC_DIR)/parser/sym.java
	find $(SRC_DIR) -name "*.class" -type f -delete

snapshots: compile
	java -cp "$(JCUP_JAR):$(SRC_DIR)" Main tests/test_positive.txt > tests/test_positive.ast
	java -cp "$(JCUP_JAR):$(SRC_DIR)" Main tests/test_contracts.txt > tests/test_contracts.ast
	java -cp "$(JCUP_JAR):$(SRC_DIR)" Main tests/test_recursion_multi.txt > tests/test_recursion_multi.ast
	java -cp "$(JCUP_JAR):$(SRC_DIR)" Main tests/test_arrays.txt > tests/test_arrays.ast
	java -cp "$(JCUP_JAR):$(SRC_DIR)" Main tests/test_tuples_logic.txt > tests/test_tuples_logic.ast

test: compile
	bash run_tests.sh