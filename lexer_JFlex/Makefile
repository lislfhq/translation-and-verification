JFLEX = jflex
JAVAC = javac
JAVA = java

SRC_DIR = src
JFLEX_FILE = $(SRC_DIR)/lexer.jflex
MAIN_CLASS = Main
TEST_FILE = test_funny.txt

all: build

generate: $(JFLEX_FILE)
	$(JFLEX) $(JFLEX_FILE)

build: generate
	$(JAVAC) $(SRC_DIR)/*.java

test: build
	$(JAVA) -cp $(SRC_DIR) $(MAIN_CLASS) --tokens < $(TEST_FILE)

clean:
	rm -f $(SRC_DIR)/*.class
	rm -f $(SRC_DIR)/FunnyLexer.java
	rm -f $(SRC_DIR)/FunnyLexer.java~

.PHONY: all generate build test clean
