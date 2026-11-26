package semantic;

import ast.Type;
import ast.VarDeclNode;
import java.util.List;

public class SymbolInfo {
    public final String name;
    public Type type;
    public final boolean isFunction;
    public final boolean isMutable;
    public final List<Type> paramTypes;
    public final List<Type> returnTypes;
    public final VarDeclNode declNode;

    public SymbolInfo(String name, Type type, boolean isMutable, VarDeclNode declNode) {
        this.name = name;
        this.type = type;
        this.isFunction = false;
        this.isMutable = isMutable;
        this.paramTypes = null;
        this.returnTypes = null;
        this.declNode = declNode;
    }

    public SymbolInfo(String name, Type returnType, List<Type> paramTypes, List<Type> returnTypes) {
        this.name = name;
        this.type = returnType;
        this.isFunction = true;
        this.isMutable = false;
        this.paramTypes = paramTypes;
        this.returnTypes = returnTypes;
        this.declNode = null;
    }
}