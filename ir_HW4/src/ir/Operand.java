package ir;

import ast.Type;
import java.util.Objects;

public class Operand {
    public enum Kind {
        CONST,
        VAR,
        LABEL
    }

    public final Kind kind;
    public final String name;
    public final int intValue;
    public final Type type;

    private Operand(Kind kind, String name, int intValue, Type type) {
        this.kind = kind;
        this.name = name;
        this.intValue = intValue;
        this.type = type;
    }

    public static Operand ofInt(int val) {
        return new Operand(Kind.CONST, Integer.toString(val), val, Type.INT);
    }

    public static Operand ofBool(boolean val) {
        return new Operand(Kind.CONST, val ? "1" : "0", val ? 1 : 0, Type.BOOL);
    }

    public static Operand ofVar(String name, Type type) {
        return new Operand(Kind.VAR, name, 0, type);
    }

    public static Operand ofLabel(String labelName) {
        return new Operand(Kind.LABEL, labelName, 0, Type.VOID);
    }

    public boolean isConst() {
        return kind == Kind.CONST;
    }

    public boolean isVar() {
        return kind == Kind.VAR;
    }

    public boolean isTemp() {
        return kind == Kind.VAR && name != null && name.startsWith("t") && name.length() > 1 && Character.isDigit(name.charAt(1));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operand)) return false;
        Operand operand = (Operand) o;
        if (kind != operand.kind) return false;
        if (kind == Kind.CONST) return intValue == operand.intValue;
        return Objects.equals(name, operand.name);
    }

    @Override
    public int hashCode() {
        return kind == Kind.CONST ? Objects.hash(kind, intValue) : Objects.hash(kind, name);
    }
}