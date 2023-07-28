/*
import com.github.javaparser...
CompilationUnit cu = StaticJavaParser.parse(new File(args[0]));
cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
    Find all methods
    Find all method calls
    Get class source code
}

save results to classes.json
*/

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

class Parser {
    public static void main(String[] args) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(Paths.get(args[0])));

        VoidVisitor<List<ClassOrInterfaceDeclaration>> classNodeCollector = new ClassNodeCollector();
        VoidVisitor<List<MethodDeclaration>> methodNodeCollector = new MethodNodeCollector();
        VoidVisitor<List<String[]>> methodCallCollector = new MethodCallCollector();

        List<ClassOrInterfaceDeclaration> classes = new ArrayList<>();
        classNodeCollector.visit(cu, classes);

        classes.forEach(cls -> {
            System.out.println(cls.getNameAsString() + ":");

            List<MethodDeclaration> methods = new ArrayList<>();
            methodNodeCollector.visit(cls, methods);
            List<String[]> methodCalls = new ArrayList<>();
            methodCallCollector.visit(cls, methodCalls);

            methods.forEach(method -> {
                System.out.println("...  " + method.getNameAsString());
            });
        });
    }

    private static class ClassNodeCollector extends VoidVisitorAdapter<List<ClassOrInterfaceDeclaration>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration cd, List<ClassOrInterfaceDeclaration> collector) {
            super.visit(cd, collector);
            collector.add(cd);
        }
    }

    private static class MethodNodeCollector extends VoidVisitorAdapter<List<MethodDeclaration>> {
        @Override
        public void visit(MethodDeclaration md, List<MethodDeclaration> collector) {
            super.visit(md, collector);
            collector.add(md);
        }
    }

    private static class MethodCallCollector extends VoidVisitorAdapter<List<String[]>> {
        @Override
        public void visit(MethodCallExpr mc, List<String[]> collector) {
            super.visit(mc, collector);
            String[] methodCall = { mc.getScope().toString().replace("Optional[", "").replace("]", ""),
                    mc.getNameAsString() };
            collector.add(methodCall);
        }
    }
}
