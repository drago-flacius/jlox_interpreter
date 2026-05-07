import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, VariableRef> locals = new HashMap<>(); // map of variable locations
    private int loopDepth = 0;

    Interpreter() {
        globals.define("Map", new LoxCallable() {

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return new LoxMap<>();
            }

            @Override
            public int arity() {
                return 0;
            }

        });
        globals.define("Array", new LoxCallable() {

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return new LoxArray((int) (double) arguments.get(0));
            }

            @Override
            public int arity() {
                return 1;
            }

        });

        globals.define("clock", new LoxCallable() {

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000;
            }

            @Override
            public int arity() {
                return 0;
            }

        });

    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError e) {
            Lox.runtimeError(e);
        }
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                checkDivisionByZero(expr.operator, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                throw new RuntimeError(expr.operator, "Operands must be either two numbers or two strings.");
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right); // no implicit conversion
            case EQUAL_EQUAL:
                return isEqual(left, right); // no implicit conversion
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
        }

        // Unreachable
        return null;
    }

    private Object evaluate(Expr expression) {
        return expression.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object leftOperand, Object rightOperand) {
        if (leftOperand instanceof Double && rightOperand instanceof Double)
            return;

        throw new RuntimeError(operator, "Both operands must be a number.");
    }

    private String stringify(Object object) {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    // assignment
    private void checkDivisionByZero(Token operator, Object right) {
        if ((double) right != 0)
            return;
        throw new RuntimeError(operator, "Divison by zero is not allowed.");
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        return null;

    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);

        System.out.println(stringify(value));

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        VariableRef ref = locals.get(expr);
        if (ref != null) {
            return environment.getAtIndex(ref.distance, ref.index);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        VariableRef ref = locals.get(expr);
        if (ref != null) {
            environment.assignAtIndex(ref.distance, ref.index, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements,
            Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;

    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        loopDepth++;

        try {
            while (isTruthy(evaluate(stmt.condition))) {
                try {
                    execute(stmt.body);
                } catch (BreakException e) {
                    break; // exit loop
                }
            }
        } finally {
            loopDepth--;
        }

        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (loopDepth == 0)
            throw new RuntimeError(stmt.token, "Break statement outside a loop");

        throw new BreakException("null");

    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes");
        }

        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected" +
                    function.arity() + "arguments but got" +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(
                stmt.name.lexeme,
                stmt.params,
                stmt.body,
                environment,
                stmt.name.lexeme.equals("init"),
                stmt.isGetter);

        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null)
            value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Object visitFunctionExpression(Expr.FunctionExpression expr) {

        return new LoxFunction(
                null,
                expr.parameters,
                expr.body,
                environment,
                false, false);
    }

    public void resolveWithIndex(Expr expr, int distance, int index) {
        locals.put(expr, new VariableRef(distance, index));
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superClass != null) {
            superclass = evaluate(stmt.superClass);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superClass.name,
                        "Superclass must be a class");
            }
        }

        environment.define(stmt.name.lexeme, null);

        if (stmt.superClass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method.name.lexeme, method.params, method.body, environment,
                    method.name.lexeme.equals("init"), method.isGetter);
            methods.put(method.name.lexeme, function);
        }
        LoxClass klass = new LoxClass(stmt.name.lexeme, methods, (LoxClass) superclass);

        if (superclass != null) {
            environment = environment.enclosing;
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    public Object visitGetExpression(Expr.GetExpression expr) {
        Object object = evaluate(expr.object);

        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name, this);
        }

        throw new RuntimeError(expr.name, "Only instances have properties");
    }

    @Override
    public Object visitSetExpression(Expr.SetExpression expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name,
                    "Only instance have fields");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance) object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpression(Expr.ThisExpression expr) {
        return lookUpVariable(expr.keyword, expr);

    }

}
