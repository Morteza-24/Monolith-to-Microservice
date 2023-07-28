import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Parser {
    public static void main(String[] args) throws Exception {
        JSONObject jsonObject = new JSONObject();
        CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(Paths.get(args[0])));

        VoidVisitor<List<ClassOrInterfaceDeclaration>> classNodeCollector = new ClassNodeCollector();
        VoidVisitor<List<String>> methodNameCollector = new MethodNameCollector();
        VoidVisitor<List<JSONObject>> methodCallCollector = new MethodCallCollector();
        VoidVisitor<List<String>> variableNameCollector = new VariableNameCollector();
        VoidVisitor<List<String>> parameterNameCollector = new ParameterNameCollector();

        List<ClassOrInterfaceDeclaration> classes = new ArrayList<>();
        classNodeCollector.visit(cu, classes);

        classes.forEach(cls -> {
            List<String> methodNames = new ArrayList<>();
            methodNameCollector.visit(cls, methodNames);
            List<JSONObject> methodCalls = new ArrayList<>();
            methodCallCollector.visit(cls, methodCalls);
            List<String> variableNames = new ArrayList<>();
            variableNameCollector.visit(cls, variableNames);
            List<String> parameterNames = new ArrayList<>();
            parameterNameCollector.visit(cls, parameterNames);

            List<String> words = new ArrayList<>();
            words.add(cls.getNameAsString());
            words.addAll(methodNames);
            words.addAll(variableNames);
            words.addAll(parameterNames);

            cls.getAllContainedComments().forEach(comment -> {
                String commentString = comment.asString().replaceAll("/\\*", "").replaceAll("\\*/", "").replaceAll("//",
                        "");
                String[] commentWords = commentString.split(" ");
                for (String w : commentWords) {
                    if (w.trim().length() > 0) {
                        words.add(w.trim());
                    }
                }
                ;
            });

            JSONObject classJson = new JSONObject();
            classJson.put("methods", methodNames);
            classJson.put("method_calls", methodCalls);
            classJson.put("words", words);
            jsonObject.put(cls.getNameAsString(), classJson);
        });

        try (FileWriter fileWriter = new FileWriter("JavaParser/classes.json")) {
            fileWriter.write(jsonObject.toString(4));
        } catch (IOException e) {
            System.out.println("Error! " + e.getMessage());
        }
    }

    private static class ClassNodeCollector extends VoidVisitorAdapter<List<ClassOrInterfaceDeclaration>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration cd, List<ClassOrInterfaceDeclaration> collector) {
            super.visit(cd, collector);
            if (!cd.isInnerClass()) {
                collector.add(cd);
            }
        }
    }

    private static class MethodNameCollector extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(MethodDeclaration md, List<String> collector) {
            super.visit(md, collector);
            collector.add(md.getNameAsString());
        }
    }

    private static class MethodCallCollector extends VoidVisitorAdapter<List<JSONObject>> {
        @Override
        public void visit(MethodCallExpr mc, List<JSONObject> collector) {
            super.visit(mc, collector);
            JSONObject methodCall = new JSONObject();
            methodCall.put("class_name", mc.getScope().toString().replace("Optional[", "").replace("]", ""));
            methodCall.put("method_name", mc.getNameAsString());
            collector.add(methodCall);
        }
    }

    private static class VariableNameCollector extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(VariableDeclarator vd, List<String> collector) {
            super.visit(vd, collector);
            collector.add(vd.getNameAsString());
        }
    }

    private static class ParameterNameCollector extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(Parameter p, List<String> collector) {
            super.visit(p, collector);
            collector.add(p.getNameAsString());
        }
    }
}
