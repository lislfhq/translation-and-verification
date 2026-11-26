package ast;

public interface ASTVisitor {
    void visit(ProgramNode node);
    void visit(FunctionDeclNode node);
    void visit(VarDeclNode node);
    void visit(AssignNode node);
    void visit(TupleAssignNode node);
    void visit(IfNode node);
    void visit(WhileNode node);
    void visit(BinaryOpNode node);
    void visit(UnaryOpNode node);
    void visit(IntLitNode node);
    void visit(BoolLitNode node);
    void visit(VarNode node);
    void visit(ReturnNode node);
    void visit(ArrayAssignNode node);
    void visit(ArrayAccessNode node);
    void visit(FuncCallNode node);
    void visit(AssertNode node);
    void visit(AssumeNode node);
}