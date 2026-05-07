package tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

// helper class to generate classes automatically

public class GenerateAst {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign   : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Logical  : Expr left, Token operator, Expr right",
                "Unary    : Token operator, Expr right",
                "Variable : Token name"

        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Expression : Expr expression",
                "Print      : Expr expression",
                "Function   : Token name, List<Token> params," +
                        " List<Stmt> body",
                "Break : Token token",
                "Class      : Token name, List<Stmt.Function> methods",
                "If         : Expr condition, Stmt thenBranch," +
                        " Stmt elseBranch",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body",
                "Return : Expr value"));

    }

    private static void defineAst(String outputDir, String baseName, List<String> types)
            throws IOException {

        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        // Package imports
        writer.println("import java.util.List;");
        writer.println();

        // Abstract base class
        writer.println("abstract class " + baseName + " {");

        // Generate Visitor interface
        defineVisitor(writer, baseName, types);

        // Abstract accept method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");
        writer.println();

        // Generate subclasses
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
            writer.println();
        }

        // Close abstract class
        writer.println("}");
        writer.close();
    }

    private static void defineType(PrintWriter writer, String baseName,
            String className, String fieldList) {

        writer.println("    static class " + className + " extends " + baseName + " {");

        // Fields
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }
        writer.println();

        // Constructor
        writer.println("        " + className + "(" + fieldList + ") {");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");
        writer.println();

        // accept() override
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        // Close subclass
        writer.println("    }");
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" + typeName + " " +
                    baseName.toLowerCase() + ");");
        }

        writer.println("    }");
    }
}