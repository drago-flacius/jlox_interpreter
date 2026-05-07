import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    private Object[] variableArray = new Object[256];
    private int variableCount = 0;
    final Environment enclosing;

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
        int index = variableCount++;
        defineAtIndex(index, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            if (values.get(name.lexeme) == null) {
                throw new RuntimeError(name,
                        "Variable:" + " " + name.lexeme + " has not been initalized or assigned to.");
            }
            return values.get(name.lexeme);
        }
        if (enclosing != null) // walk the entire chain of enclosing environments.
            return enclosing.get(name);

        throw new RuntimeError(name,
                "Undefined variable:" + " " + name.lexeme + ".");
    }

    // variable already needs to exist.
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                "Undefined variable" + " " + name.lexeme + ".");
    }

    public Object getAt(Integer distance, String name) {
        return ancestor(distance).values.get(name);
    }

    private Environment ancestor(Integer distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }

    public void assignAt(Integer distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    public Object getAtIndex(int distance, int index) {
        Environment env = ancestor(distance);
        if (index < env.variableArray.length && env.variableArray[index] != null) {
            return env.variableArray[index];
        }
        throw new RuntimeError(null, "Uninitialized variable access at index " + index);
    }

    public void assignAtIndex(int distance, int index, Object value) {
        Environment env = ancestor(distance);
        // Expand array if needed
        if (index >= env.variableArray.length) {
            Object[] newArray = new Object[index + 256];
            System.arraycopy(env.variableArray, 0, newArray, 0, env.variableArray.length);
            env.variableArray = newArray;
        }
        env.variableArray[index] = value;
    }

    public void defineAtIndex(int index, Object value) {
        if (index >= variableArray.length) {
            Object[] newArray = new Object[index + 256];
            System.arraycopy(variableArray, 0, newArray, 0, variableArray.length);
            variableArray = newArray;
        }
        variableArray[index] = value;
    }

}
