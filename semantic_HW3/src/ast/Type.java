package ast;

public enum Type {
    INT,
    BOOL,
    STRING,
    VOID,
    ARRAY,
    FUNC,
    UNKNOWN, //тип до прохождения семантического анализатора
    ERROR    // тип, если при проверке найдена ошибка
}