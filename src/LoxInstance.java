import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    public LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    LoxInstance() {

    }

    Object get(Token name, Interpreter interpreter) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = klass.findMethod(this, name.lexeme);
        if (method != null) {
            return method; // already bound
        }

        throw new RuntimeError(name,
                "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }

}