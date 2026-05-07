import java.util.List;

public class LoxArray extends LoxInstance {
    Object[] elements;

    public LoxArray(int size) {
        elements = new Object[size];
    }

    @Override
    Object get(Token name, Interpreter interpreter) {
        if (name.lexeme.equals("get")) {
            return new LoxCallable() {

                @Override
                public Object call(Interpreter interpreter, List<Object> arguments) {
                    int index = (int) (double) arguments.get(0);
                    return elements[index];
                }

                @Override
                public int arity() {
                    return 1;
                }

            };
        } else if (name.lexeme.equals("set")) {
            return new LoxCallable() {

                @Override
                public Object call(Interpreter interpreter, List<Object> arguments) {
                    int index = (int) (double) arguments.get(0);
                    Object value = arguments.get(1);
                    return elements[index] = value;
                }

                @Override
                public int arity() {
                    return 2;
                }

            };
        } else if (name.lexeme.equals("length")) {
            return (int) (double) elements.length;
        }

        throw new RuntimeError(name,
                "Undefined property '" + name.lexeme + "'.");
    }

    @Override
    void set(Token name, Object value) {
        throw new UnsupportedOperationException("Array doesn't have fields");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object o : elements) {
            sb.append(o).append(", ");
        }
        if (elements.length != 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");

        return sb.toString();
    }

}
