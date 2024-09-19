package joev.javadoctest;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * A doclet that runs tests found in JavaDoc snippets, introduced with JEP-413.
 *
 * <pre>
 * javadoc \
 *   -docletpath path/to/javadoctest.jar \
 *   -doclet joev.javadoctest.JavaDocTestDoclet \
 *   -sourcepath path/to/source \
 *   -J-Djavadoctest.classpath path/to/compiled/classes \
 *   -J-ea \
 *   packageName [packageName ...]
 * </pre>
 */
public class JavaDocTestDoclet implements Doclet {
  TestResult result = TestResult.ZERO;
  Reporter reporter = null;

  /**
   * Create a new instance of JavaDocTestDoclet.
   */
  public JavaDocTestDoclet() {}

  // @formatter:off
  /**
   * Get the name of this test runner.
   * {@snippet test import=jdk.javadoc.doclet.Doclet:
   * // @highlight substring=doclet type=italic:
   * Doclet doclet = new JavaDocTestDoclet();
   * assert "Javadoc Test Runner".equals(doclet.getName());
   * }
   * @return "Javadoc Test Runner"
   */
  // @formatter:on
  @Override
  public String getName() {
    return "Javadoc Test Runner";
  }

  // @formatter:off
  /**
   * Get the supported options for this doclet.
   *
   * {@snippet test import=jdk.javadoc.doclet.Doclet,java.util.Collections :
   *
   * Doclet doclet = new JavaDocTestDoclet();
   * assert Collections.emptySet().equals(doclet.getSupportedOptions());
   * }
   */
  // @formatter:on
  @Override
  public Set<? extends Doclet.Option> getSupportedOptions() {
    return Collections.emptySet();
  }

  // @formatter:off
  /**
   * Get the supported source version.
   *
   * {@snippet test=junit5 foo=bar
   *   import=jdk.javadoc.doclet.Doclet,javax.lang.model.SourceVersion:
   * Doclet doclet = new JavaDocTestDoclet();
   * assert SourceVersion.latest().equals(doclet.getSupportedSourceVersion());
   * }
   */
  // @formatter:on
  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  /**
   * Initialize the doclet. This implemenation does nothing.
   */
  @Override
  public void init(Locale locale, Reporter reporter) {
    this.reporter = reporter;
  }

  /**
   * Processes the doctrees in the environment, passing their elements with the SnippetVisitor.
   */
  @Override
  public boolean run(DocletEnvironment environment) {
    DocTrees docTrees = environment.getDocTrees();
    DocTreeVisitor<TestResult, SnippetVisitor.Data> snippetVisitor = new SnippetVisitor();
    for (Element e : environment.getSpecifiedElements()) {
      handleElement(e, docTrees, snippetVisitor);
    }
    if (reporter != null) {
      reporter.print(Diagnostic.Kind.NOTE, "Tests passed: " + result.pass() + ", failed: "
          + result.fail() + ", skipped: " + result.skip());
    }
    return true;
  }

  /**
   * Recursively walk through the element tree to find document comments.
   *
   * @param element The element to check for doc comments.
   * @param docTrees The DocTrees tool from the docklet environment.
   * @param visitor The visitor that is called with the doc comment tree.
   */
  public void handleElement(Element element, DocTrees docTrees,
      DocTreeVisitor<TestResult, SnippetVisitor.Data> visitor) {
    DocCommentTree dct = docTrees.getDocCommentTree(element);
    if (dct != null) {
      result = result.reduce(dct.accept(visitor, new SnippetVisitor.Data(element, reporter)));
    }

    for (Element child : element.getEnclosedElements()) {
      handleElement(child, docTrees, visitor);
    }
  }
}