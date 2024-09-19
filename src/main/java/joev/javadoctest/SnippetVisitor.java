package joev.javadoctest;

import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.SnippetTree;
import com.sun.source.util.DocTreeScanner;
import jdk.javadoc.doclet.Reporter;

// @formatter:off
/**
 * A DocTreeScanner that compiles and runs test snippets.
 *
 * {@snippet :
 * DockletEnvironment environment = ...;
 * Reporter reporter = ...;
 * Element element = ...;
 *
 * DocTrees docTrees = environment.getDocTrees();
 * DocCommentTree commentTree = docTrees.getDocCommentTree(element);
 * TestResult result =
 *   commentTree.accept(new SnippetVisitor(), new SnippetVisitorData(element, reporter));
 * }
 */
// @formatter:on
public class SnippetVisitor extends DocTreeScanner<TestResult, SnippetVisitor.Data> {
  /**
   * The SnippetVisitor data parameters.
   *
   * @param element the element to visit.
   * @param reporter the JavaDoc tool reporter for diagnostic output.
   */
  public record Data(Element element, Reporter reporter) {
  }

  /**
   * An Attribute has a name and a list of values.
   */
  private record Attribute(String name, List<String> values) {
  };

  /**
   * A DocTreeScanner that extracts an Attribute from an AttributeTree node.
   */
  private static DocTreeScanner<Attribute, Void> attributeExtractor = new DocTreeScanner<>() {
    @Override
    public Attribute visitAttribute(AttributeTree node, Void p) {
      return new Attribute(node.getName().toString(),
          requireNonNullElse(node.getValue(), Collections.emptyList()).stream()
              .map(Object::toString).toList());
    }
  };

  /** The runtime classloader. */
  private final ClassLoader classLoader;

  /** The Path of compiled test classes. */
  private final Path classDir;

  /**
   * Create a new SnippetVisitor. This expects the javadoctest.classpath system property to be set
   * to the path that contains the compiled classes under test.
   */
  public SnippetVisitor() {
    Path tmpPath;
    boolean registerShutdownHook = false;
    try {
      tmpPath = Files.createTempDirectory("javadoctest-");
      registerShutdownHook = true;
    } catch (IOException e) {
      tmpPath = Paths.get(".");
    }
    classDir = tmpPath;

    // Generate the list of paths in the classpath by concatenating
    // the value of the javadoctest.classpath system property with
    // the temporary directory that is the destination of the
    // compiled snippet test classes.
    List<Path> paths = Stream.concat(Arrays
        .asList(
            Objects.requireNonNullElse(System.getProperty("javadoctest.classpath"), "").split(":"))
        .stream().map(Paths::get), Stream.of(classDir)).toList();
    classLoader = new DynamicClassLoader(paths);

    // If we created a temporary directory, make sure it's deleted
    // when we're done.
    if (registerShutdownHook) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          Files.walkFileTree(classDir, DeleteTreeFileVisitor.instance());
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }));
    }
  }

  /**
   * A map of counts used to make sure elements with multiple test snippets don't collide.
   */
  private static Map<String, Integer> classCount = new HashMap<>();

  /**
   * Generate the test class name for an Element.
   *
   * @param e the element to generate the test class for.
   * @return the generated test class name.
   */
  private static String generateTestClassName(Element e) {
    Deque<String> deque = new ArrayDeque<>();
    String kind = e.getKind().toString();
    Element current = e;

    // Walk up the tree of enclosing elements, add names to the deque.
    while (current != null) {
      Name name =
          (current instanceof PackageElement) ? ((PackageElement) current).getQualifiedName()
              : current.getSimpleName();
      deque.addFirst(name.toString().replace(".", "_"));
      current = current.getEnclosingElement();
    }

    String name = kind + deque.stream().collect(StringBuilder::new, (a, el) -> {
      a.append("_");
      a.append(el);
    }, StringBuilder::append).toString();

    // Keep the count of any classes already generated for this name,
    // and append a _N for each test beyond the first.
    Integer count = classCount.getOrDefault(name, 0);
    classCount.put(name, count + 1);
    if (count != 0) {
      name = name + "_" + count;
    }
    return name;
  }

  /**
   * Walk up the element tree to find the package name of the element.
   *
   * @param e The element to search for an enclosing package.
   * @return An Optional containing the package element, or an empty Optional if none if found.
   */
  private Optional<PackageElement> getEnclosingPackage(Element e) {
    Element current = e;
    while (current != null) {
      if (current.getKind() == ElementKind.PACKAGE) {
        return Optional.of((PackageElement) current);
      }
      current = current.getEnclosingElement();
    }
    return Optional.empty();
  }

  /**
   * Runs tests in the provided snippet if it has the test attribute.
   */
  @Override
  public TestResult visitSnippet(SnippetTree node, SnippetVisitor.Data data) {
    TestResult result = TestResult.ZERO;
    List<Attribute> attributes =
        node.getAttributes().stream().map((dt) -> dt.accept(attributeExtractor, null)).toList();
    boolean isTest = attributes.stream().anyMatch((a) -> "test".equals(a.name()));
    if (isTest) {
      Element element = data.element();
      String generatedClassName = generateTestClassName(element);
      data.reporter().print(Diagnostic.Kind.NOTE, "Running snippet test " + generatedClassName);

      // The imports list includes the "star" import for the current package and
      // any imports provided in the import attribute of the snippet.
      String imports = Stream.concat(
          getEnclosingPackage(element).stream().map((x) -> x.getQualifiedName().toString() + ".*"),
          attributes.stream().filter((a) -> "import".equals(a.name())).flatMap(
              (a) -> a.values().stream().flatMap((v) -> Arrays.asList(v.split(",")).stream())))
          .map((s) -> "import " + s + "; ")
          .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

      // Generate the test class
      String java = imports + "public class " + generatedClassName
          + " { public static void test() throws Exception { " + node.getBody() + " } }";

      // Create a Java File Object that lives in memory
      JavaFileObject jfo = new SimpleJavaFileObject(
          URI.create("string:///" + generatedClassName + ".java"), JavaFileObject.Kind.SOURCE) {
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
          return java;
        }
      };
      Iterable<JavaFileObject> compilationUnits = List.of(jfo);

      // Compile the test code.
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
      List<String> options = List.of("-classpath", System.getProperty("javadoctest.classpath"),
          "-d", classDir.toString());
      compiler.getTask(null, fileManager, null, options, null, compilationUnits).call();

      // Run the test.
      try {
        Class<?> cls = classLoader.loadClass(generatedClassName);
        Method test = cls.getMethod("test");
        test.invoke(null);
        result = result.reduce(TestResult.PASS);
      } catch (InvocationTargetException e) {
        data.reporter().print(Diagnostic.Kind.ERROR, element, "Failure in snippet test for:");
        requireNonNullElse(e.getCause(), e).printStackTrace(data.reporter().getDiagnosticWriter());
        result = result.reduce(TestResult.FAIL);
      } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return result;
  }

  /**
   * Reduce TestResults by calling TestResult::reduce.
   *
   * @param r1 the first test result
   * @param r2 the second test result
   * @return the reduced test result.
   */
  @Override
  public TestResult reduce(TestResult r1, TestResult r2) {
    return (r1 == null ? TestResult.ZERO : r1).reduce(r2);
  }
}
