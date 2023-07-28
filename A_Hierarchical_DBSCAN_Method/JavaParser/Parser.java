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



import java.nio.file.Files;

import java.nio.file.Paths;

import java.util.ArrayList;

import java.util.List;

import java.util.HashMap;



class Parser {

    public static void main(String[] args) throws Exception {

        CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(Paths.get(args[0])));



        List<ClassOrInterfaceDeclaration> classes = new ArrayList<>();

        VoidVisitor<List<ClassOrInterfaceDeclaration>> classNodeCollector = new ClassNodeCollector();

        classNodeCollector.visit(cu, classes);



        classes.forEach(n -> System.out.println(n.getNameAsString()));

    }



    private static class ClassNodeCollector extends VoidVisitorAdapter<List<ClassOrInterfaceDeclaration>> {

        @Override

        public void visit(ClassOrInterfaceDeclaration cd, List<ClassOrInterfaceDeclaration> collector) {

            super.visit(cd, collector);

            collector.add(cd);

        }

    }

}