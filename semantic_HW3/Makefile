JFLEX = jflex
CUP_JAR = lib/java-cup-11b.jar
JAVAC = javac
JAVA = java

SRC_DIR = src
LEXER_DIR = $(SRC_DIR)/lexer
PARSER_DIR = $(SRC_DIR)/parser
BUILD_DIR = build
TEST_DIR = tests

MAIN_CLASS = Main

all: build

gen_parser: $(PARSER_DIR)/parser.cup
	$(JAVA) -cp $(CUP_JAR) java_cup.Main -destdir $(PARSER_DIR) -parser FunnyParser -symbols sym $(PARSER_DIR)/parser.cup

gen_lexer: $(LEXER_DIR)/lexer.jflex gen_parser
	$(JFLEX) -d $(LEXER_DIR) $(LEXER_DIR)/lexer.jflex

build: gen_lexer
	mkdir -p $(BUILD_DIR)
	$(JAVAC) -cp "$(CUP_JAR):$(SRC_DIR)" -d $(BUILD_DIR) \
	   $(SRC_DIR)/*.java \
	   $(LEXER_DIR)/*.java \
	   $(PARSER_DIR)/*.java \
	   $(SRC_DIR)/ast/*.java

test: build
	@echo "=== Запуск позитивного теста с проверкой AST ==="
	$(JAVA) -cp "$(BUILD_DIR):$(CUP_JAR)" $(MAIN_CLASS) $(TEST_DIR)/positive_main.txt $(TEST_DIR)/expected_ast.txt

test-neg: build
	@echo "=== Запуск негативных тестов ==="
	@echo "\n--- Тест 1: Лексическая ошибка (цифра в начале) ---"
	-$(JAVA) -cp "$(BUILD_DIR):$(CUP_JAR)" $(MAIN_CLASS) $(TEST_DIR)/negative_lexical.txt
	@echo "\n--- Тест 2: Синтаксическая ошибка (незакрытая скобка) ---"
	-$(JAVA) -cp "$(BUILD_DIR):$(CUP_JAR)" $(MAIN_CLASS) $(TEST_DIR)/negative_syntax.txt
	@echo "\n--- Тест 3: Лишние токены ---"
	-$(JAVA) -cp "$(BUILD_DIR):$(CUP_JAR)" $(MAIN_CLASS) $(TEST_DIR)/negative_extra_tokens.txt
	@echo "\n--- Тест 4: Неверный оператор ---"
	-$(JAVA) -cp "$(BUILD_DIR):$(CUP_JAR)" $(MAIN_CLASS) $(TEST_DIR)/negative_wrong_operator.txt
	@echo "\n--- Тест 5: Пустой ввод ---"
	-$(JAVA) -cp "$(BUILD_DIR):$(CUP_JAR)" $(MAIN_CLASS) $(TEST_DIR)/negative_empty.txt

test-all: test test-neg

clean:
	rm -rf $(BUILD_DIR)
	rm -f $(LEXER_DIR)/FunnyLexer.java
	rm -f $(LEXER_DIR)/FunnyLexer.java~
	rm -f $(PARSER_DIR)/FunnyParser.java
	rm -f $(PARSER_DIR)/sym.java

.PHONY: all gen_parser gen_lexer build test test-neg test-all clean