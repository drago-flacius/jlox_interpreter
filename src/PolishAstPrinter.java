
class PolishAstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme,
                expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null)
            return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    private String parenthesize(String operation, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(operation);
        for (Expr e : exprs) {
            builder.append(" ");
            builder.append(e.accept(this));
        }

        builder.append(")");
        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));

        Expr expression2 = new Expr.Binary(
                new Expr.Binary(
                        new Expr.Literal(1),
                        new Token(TokenType.PLUS, "+", null, 1),
                        new Expr.Literal(2)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(
                        new Expr.Literal(4),
                        new Token(TokenType.PLUS, "-", null, 1),
                        new Expr.Literal(3)));

        System.out.println(new PolishAstPrinter().print(expression2));
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitVariableExpr'");
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAssignExpr'");
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitLogicalExpr'");
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
    }

    @Override
    public String visitFunctionExpression(Expr.FunctionExpression expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitFunctionExpression'");
    }

    @Override
    public String visitGetExpression(Expr.GetExpression expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitGetExpression'");
    }

    @Override
    public String visitSetExpression(Expr.SetExpression expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitSetExpression'");
    }

    @Override
    public String visitThisExpression(Expr.ThisExpression expr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitThisExpression'");
    }

}