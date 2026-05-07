import java.util.List;

public class LoxFunction implements LoxCallable {
    private final String name; // null for anonymous functions
    private final List<Token> params;
    private final List<Stmt> body;
    private final Environment closure;
    private final boolean isInitializer;
    private final boolean isGetter;

    public LoxFunction(String name,
            List<Token> params,
            List<Stmt> statements,
            Environment closure,
            boolean isInitializer) {
        this.name = name;
        this.params = params;
        this.body = statements;
        this.closure = closure;
        this.isInitializer = isInitializer;
        this.isGetter = false;
    }

    public LoxFunction(String name,
            List<Token> params,
            List<Stmt> statements,
            Environment closure,
            boolean isInitializer,
            boolean isGetter) {
        this.name = name;
        this.params = params;
        this.body = statements;
        this.closure = closure;
        this.isInitializer = isInitializer;
        this.isGetter = isGetter;
    }

    @Override
    public Object call(Interpreter interpreter,
            List<Object> arguments) {

        Environment environment = new Environment(closure);
        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).lexeme,
                    arguments.get(i));
        }

        try {
            interpreter.executeBlock(body, environment);
        } catch (Return returnValue) {
            if (isInitializer)
                return closure.getAt(0, "this");

            return returnValue.value;
        }

        if (isInitializer)
            return closure.getAt(0, "this");
        return null;
    }

    LoxFunction bind(LoxInstance instance, LoxFunction inner) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        // If there's no inner method, define a no-op callable
        if (inner == null) {
            environment.define("inner", new LoxCallable() {
                @Override
                public Object call(Interpreter interpreter, List<Object> arguments) {
                    return null; // Do nothing
                }

                @Override
                public int arity() {
                    return 0;
                }
            });
        } else {
            environment.define("inner", inner);
        }
        return new LoxFunction(name, params, body, environment, isInitializer);
    }

    @Override
    public int arity() {
        return params.size();
    }

    @Override
    public String toString() {
        return name == null ? "<fn>" : "<fn " + name + ">";
    }

    public boolean isGetter() {
        return isGetter;
    }
}