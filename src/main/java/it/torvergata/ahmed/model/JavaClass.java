package it.torvergata.ahmed.model;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import it.torvergata.ahmed.utilities.JavaParserUtil;
import lombok.Getter;

import java.util.*;

@Getter
public class JavaClass {
    private static final String MAIN_METHOD_SIGNATURE = " public static void main(String[] args)";

    /**
     * the class name package/path/ClassName
     */
    private final String name;
    /**
     * Class body
     */

    private final String classBody;

    private String packageName = "";
    private  String simpleName = "";
    /**
     * Key: Method declaration
     * Value: method String body
     */
    private final Map<String, String> methods;
    /**
     * Key: Method declaration
     * Value: Metrics
     *
     * @see Metrics
     */
    private final Map<String, MethodMetrics> methodsMetrics;
    /**
     * Which release this class of
     *
     * @see Release
     */
    private final Release release;
    /**
     * Class metrics
     *
     * @see Metrics
     */
    private final Metrics metrics;
    /**
     * All commits of this class in this release
     *
     * @see Commit
     */
    private final List<Commit> classCommits;
    /**
     * -- GETTER --
     *  Return the Lines of code added in the commit
     */
    private final List<Integer> lOCAddedByClass;
    private final List<Integer> lOCRemovedByClass;

    public JavaClass(String name, String classBody, Release release, boolean update) {
        this.name = name;
        this.classBody = classBody;
        this.methods = new HashMap<>();
        this.methodsMetrics = new HashMap<>();
        this.release = release;
        this.updateMethodsMap(update);
        metrics = new Metrics();
        classCommits = new ArrayList<>();
        lOCAddedByClass = new ArrayList<>();
        lOCRemovedByClass = new ArrayList<>();
    }

    /**
     * Parse java class and get in string format method declaration and initialize a method
     * Metrics map
     *
     * @see Metrics
     */
    private void updateMethodsMap(boolean update) {
        CompilationUnit cu = StaticJavaParser.parse(this.classBody);
        cu.getPackageDeclaration().ifPresent(packageDeclaration ->
                this.packageName = packageDeclaration.getNameAsString());
        cu.getTypes().stream()
                .findFirst()
                .map(TypeDeclaration::getNameAsString).ifPresent(className ->
                        this.simpleName = className);

        cu.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {

            String signature = JavaParserUtil.getSignature(methodDeclaration);
            methods.put(signature, JavaParserUtil.getStringBody(methodDeclaration));
            if (update) {
                MethodMetrics methodMetrics = new MethodMetrics();
                methodsMetrics.put(signature, methodMetrics);
                methodMetrics.setParameterCount(JavaParserUtil.computeParameterCount(methodDeclaration));
                methodMetrics.setLinesOfCode(JavaParserUtil.computeEffectiveLOC(methodDeclaration));
                methodMetrics.setStatementCount(JavaParserUtil.computeStatementCount(methodDeclaration));
                methodMetrics.setCyclomaticComplexity(JavaParserUtil.computeCyclomaticComplexity(methodDeclaration));
                methodMetrics.setNestingDepth(JavaParserUtil.computeNestingDepth(methodDeclaration));
                methodMetrics.setMethodAccessor(methodDeclaration.getAccessSpecifier().asString());
                methodDeclaration.getBody().ifPresent(body ->
                        methodMetrics.setCognitiveComplexity(JavaParserUtil.calculateCognitiveComplexity(body)));
                methodMetrics.setBeginLine(methodDeclaration.getBegin().orElse(new Position(0, 0)).line);
                methodMetrics.setEndLine(methodDeclaration.getEnd().orElse(new Position(0, 0)).line);
                methodMetrics.setSimpleName(methodDeclaration.getNameAsString());
                methodMetrics.setAge(this.release.getId());
            }
        });
        // we won't check main methods that could be added to do something
        methodsMetrics.keySet().removeIf(key -> key.contains(MAIN_METHOD_SIGNATURE));
        methods.keySet().removeIf(key -> key.contains(MAIN_METHOD_SIGNATURE));
    }


    /**
     * Add the commit for this class
     *
     * @param commit the GitHub RevCommit that touched this class
     */
    public void addCommitToClass(Commit commit) {
        this.classCommits.add(commit);
    }

    /**
     * Add info about Lines of code added
     *
     * @param lOCAddedByEntry num of loc added
     */
    public void addLOCAddedByClass(Integer lOCAddedByEntry) {
        lOCAddedByClass.add(lOCAddedByEntry);
    }

    /**
     * lines of code have been removed
     *
     * @param lOCRemovedByEntry number of LOC removed
     */
    public void addLOCRemovedByClass(Integer lOCRemovedByEntry) {
        lOCRemovedByClass.add(lOCRemovedByEntry);
    }



    @Override
    public String toString() {
        return "JavaClass{" +
                "name='" + name + '\'' +
                ", contentOfClass='" + classBody + '\'' +
                ", release=" + release +
                ", metrics=" + metrics +
                ", commitsThatTouchTheClass=" + classCommits +
                ", lOCAddedByClass=" + lOCAddedByClass +
                ", lOCRemovedByClass=" + lOCRemovedByClass +
                '}';
    }

    public String getClassName() {
        return this.packageName + '.' + this.simpleName;
    }
}
