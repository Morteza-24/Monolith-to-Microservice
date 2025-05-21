import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;
import java.io.FileWriter;

class ClassScanner {
    public static void main(String[] args) throws Exception {
        JSONObject jsonObject = new JSONObject();
        Path baseDir = Paths.get(args[0]);
        List<String> classes = new ArrayList<>();
        Files.walk(baseDir).forEach(path -> {
            File file = path.toFile();
            if (!file.isDirectory()) {
                String fileName = file.getName();
                int i = fileName.lastIndexOf('.');
                if (i > 0) {
                    if (fileName.substring(i + 1).toLowerCase().equals("java")) {
                        try {
                            classes.addAll(getClasses(file.getPath()));
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        jsonObject.put("classes", classes);
        try (FileWriter fileWriter = new FileWriter(args[1])) {
            fileWriter.write(jsonObject.toString(4));
        } catch (IOException e) {
            System.out.println("Error! " + e.getMessage());
        }
    }

    static List<String> getClasses(String path) throws Exception {
        final ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        JavaParser javaParser = new JavaParser(parserConfiguration);
        CompilationUnit cu = javaParser.parse(Files.newInputStream(Paths.get(path)))
            .getResult()
            .orElseThrow(() -> new IOException("Failed to parse the file: " + path));
        VoidVisitor<List<String>> classNameCollector = new ClassNameCollector();
        List<String> classNames = new ArrayList<>();
        classNameCollector.visit(cu, classNames);
        return classNames;
    }

    private static class ClassNameCollector extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration cd, List<String> collector) {
            super.visit(cd, collector);
            if (!cd.isInnerClass()) {
                collector.add(cd.getNameAsString());
            }
        }
    }
}
