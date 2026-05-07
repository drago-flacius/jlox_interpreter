import java.util.List;

// interface for all 'callable' things ... functions, classes etc
interface LoxCallable {
    Object call(Interpreter interpreter, List<Object> arguments);

    int arity();
}
