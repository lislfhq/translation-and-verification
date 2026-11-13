package semantic;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, SymbolInfo> symbols = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    public Scope getParent() {
        return parent;
    }

    public boolean define(SymbolInfo symbol) {
        if (symbols.containsKey(symbol.name)) {
            return false;
        }
        symbols.put(symbol.name, symbol);
        return true;
    }

    public SymbolInfo resolve(String name) {
        if (symbols.containsKey(name)) {
            return symbols.get(name);
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        return null;
    }
}