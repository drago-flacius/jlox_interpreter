import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Variable>> scopes = new Stack<>();
    private final Stack<Integer> scopeVariableIndexes = new Stack<>(); // Track next index for each scope
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClassType = ClassType.NONE;

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    private void endScope() {
        Map<String, Variable> scope = scopes.pop();
        scopeVariableIndexes.pop(); // Pop the index counter
        scope.forEach((key, value) -> {
            if (!value.used && key != "this" && key != "inner") {
                Lox.error(value.token, "Variable declared but never used.");
            }
        });
    }

    public void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt statement) {
        statement.accept(this);
    }

    private void resolve(Expr expression) {
        expression.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Variable>());
        scopeVariableIndexes.push(0); // Start index counter at 0 for new scope
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null)
            resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private void define(Token name) {
        if (scopes.isEmpty())
            return;
        scopes.peek().get(name.lexeme).defined = true;
    }

    private void declare(Token name) {
        if (scopes.isEmpty())
            return;
        Map<String, Variable> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name,
                    "Already a variable with this name in this scope");
        }
        Variable variable = new Variable();
        variable.token = name;
        int currentIndex = scopeVariableIndexes.pop();
        variable.index = currentIndex;
        scopeVariableIndexes.push(currentIndex + 1); // Increment for next variable
        scope.put(name.lexeme, variable);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.name, "Can't return from top-level code.");
        }
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.name,
                        "Can't return a value from a constructor");
            }
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty()) {
            Variable var = scopes.peek().get(expr.name.lexeme);
            if (var != null) {
                if (!var.defined) {
                    Lox.error(expr.name, "Can't read local variable in its own initializer.");
                }
                var.used = true;
            }
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                int distance = scopes.size() - 1 - i;
                int index = scopes.get(i).get(name.lexeme).index;
                interpreter.resolveWithIndex(expr, distance, index);
                return;
            }
        }
    }

    @Override
    public Void visitFunctionExpression(Expr.FunctionExpression expr) {
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClassType;
        currentClassType = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        if (stmt.superClass != null &&
                stmt.name.lexeme.equals(stmt.superClass.name.lexeme)) {
            Lox.error(stmt.superClass.name,
                    "A class can't inherit from itself.");
        }

        if (stmt.superClass != null) {
            currentClassType = ClassType.SUBCLASS;
            resolve(stmt.superClass);
        }

        if (stmt.superClass != null) {
            beginScope();
            Variable v = new Variable();
            v.declared = true;
            v.defined = true;
            v.used = true;
            scopes.peek().put("super", v);
        }

        beginScope();
        int currentIndex = scopeVariableIndexes.pop();
        Variable v = new Variable();
        v.declared = true;
        v.defined = true;
        v.used = true;
        v.index = currentIndex;
        scopes.peek().put("this", v);
        currentIndex++;
        Variable v1 = new Variable();
        v1.declared = true;
        v1.defined = true;
        v1.used = true;
        v1.index = currentIndex;
        scopes.peek().put("inner", v1);
        scopeVariableIndexes.push(currentIndex + 1);

        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method, declaration);
        }
        endScope();

        if (stmt.superClass != null)
            endScope();

        currentClassType = enclosingClass;
        return null;
    }

    @Override
    public Void visitGetExpression(Expr.GetExpression expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSetExpression(Expr.SetExpression expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpression(Expr.ThisExpression expr) {
        if (currentClassType == ClassType.NONE) {
            Lox.error(expr.keyword,
                    "Can't use 'this' outside of a class");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

}