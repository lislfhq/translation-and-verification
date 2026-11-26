package opt;

public class OptimizationConfig {
    public boolean enableConstProp = true;
    public boolean enableAlgebraic = true;
    public boolean enableCSE = true;
    public boolean enableDCE = true;

    public boolean isAnyEnabled() {
        return enableConstProp || enableAlgebraic || enableCSE || enableDCE;
    }

    public void disableAll() {
        enableConstProp = false;
        enableAlgebraic = false;
        enableCSE = false;
        enableDCE = false;
    }

    @Override
    public String toString() {
        return "const=" + enableConstProp
                + ", alg=" + enableAlgebraic
                + ", cse=" + enableCSE
                + ", dce=" + enableDCE;
    }
}