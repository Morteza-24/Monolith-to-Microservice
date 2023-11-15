import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ClassSources {
    public static void main(String[] args) throws Exception {
        JSONObject jsonObject = new JSONObject();
        CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(Paths.get(args[0])));

        VoidVisitor<List<ClassOrInterfaceDeclaration>> classNodeCollector = new ClassNodeCollector();

        List<ClassOrInterfaceDeclaration> classes = new ArrayList<>();
        classNodeCollector.visit(cu, classes);

        classes.forEach(cls -> {
            jsonObject.put(cls.getNameAsString(), cls.toString());
        });

        try (FileWriter fileWriter = new FileWriter(args[1])) {
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
}
