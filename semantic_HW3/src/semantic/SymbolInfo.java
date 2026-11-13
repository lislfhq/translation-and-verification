package semantic;

import ast.Type;
import java.util.List;

public class SymbolInfo {
    public final String name;
    public final Type type;
    public final boolean isFunction;
    public final List<Type> paramTypes; //для аргументов функций

    public SymbolInfo(String name, Type type, boolean isFunction, List<Type> paramTypes) {
        this.name = name;
        this.type = type;
        this.isFunction = isFunction;
        this.paramTypes = paramTypes;
    }
}