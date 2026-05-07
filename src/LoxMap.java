import java.util.List;

public class LoxMap<K, V> extends LoxInstance {

    static class Node<K, V> {
        Long hash;
        K key;
        V value;
        Node<K, V> next;
    }

    Node<K, V>[] table;

    public LoxMap() {
        table = new Node[16];
    }

    @Override
    Object get(Token name, Interpreter interpreter) {
        if (name.lexeme.equals("put")) {
            return new LoxCallable() {

                @Override
                public Object call(Interpreter interpreter, List<Object> arguments) {
                    Object key = arguments.get(0);
                    Object value = arguments.get(1);

                    int i = index(key);
                    Node<K, V> node = table[i];

                    while (node != null) {
                        if (node.key.equals(key)) {
                            node.value = (V) value;
                            return null;
                        }
                        node = node.next;

                    }

                    Node<K, V> newNode = new Node<>();
                    newNode.key = (K) key;
                    newNode.value = (V) value;
                    newNode.next = table[i];
                    table[i] = newNode;
                    return null;

                }

                @Override
                public int arity() {
                    return 2;
                }

            };
        } else if (name.lexeme.equals("get")) {
            return new LoxCallable() {

                @Override
                public Object call(Interpreter interpreter, List<Object> arguments) {
                    Object key = arguments.get(0);

                    int i = index(key);
                    Node<K, V> node = table[i];
                    while (node != null) {
                        if (node.key.equals(key)) {
                            return node.value;
                        }
                        node = node.next;
                    }

                    throw new RuntimeError(name, "Key is not present in map");

                }

                @Override
                public int arity() {
                    return 1;
                }

            };
        } else {
            throw new RuntimeError(name, "Undefined property '" + name.lexeme + ".");
        }
    }

    private int index(Object key) {
        if (key == null)
            return 0;
        return (key.hashCode() & 0x7fffffff) % table.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;

        for (Node<K, V> bucket : table) {
            Node<K, V> node = bucket;

            while (node != null) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;

                sb.append(node.key)
                        .append("=")
                        .append(node.value);

                node = node.next;
            }
        }

        sb.append("}");
        return sb.toString();
    }

}
