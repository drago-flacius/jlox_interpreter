import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;
    final LoxClass superClass;

    LoxClass(String name, Map<String, LoxFunction> methods, LoxClass superClass) {
        this.name = name;
        this.methods = methods;
        this.superClass = superClass;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);

        LoxFunction initializer = findMethod(instance, "init");
        if (initializer != null) {
            initializer.call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        // // No instance here → pass null
        // LoxFunction initializer = findMethod(null, "init");
        // if (initializer == null)
        // return 0;
        // return initializer.arity();
        return 0;
    }

    LoxFunction findMethod(LoxInstance instance, String name) {
        LoxFunction method = null;
        LoxFunction inner = null;
        LoxClass klass = this;

        while (klass != null) {
            if (klass.methods.containsKey(name)) {
                inner = method;
                method = klass.methods.get(name);
            }
            klass = klass.superClass;
        }

        if (method != null) {
            return method.bind(instance, inner); // ONLY bind happens here
        }

        return null;
    }

    // Static access (class-level)
    @Override
    Object get(Token name, Interpreter interpreter) {
        if (methods.containsKey(name.lexeme)) {
            return methods.get(name.lexeme);
        }

        // Use new signature
        LoxFunction method = findMethod(null, name.lexeme);
        if (method != null)
            return method;

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }
}