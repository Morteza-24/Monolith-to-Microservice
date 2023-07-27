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
import java.nio.file.Files;
import java.nio.file.Paths;

class Parser {
    public static void main(String[] args) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(Paths.get(args[0])));
    }
}
