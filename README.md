# JavadocTest

JavadocTest is a lightweight testing system inspired by Rust doctests, which
allow simple tests to be put into the documentation comments in code. It is
based on Javadoc @snippet directives introduced as [JEP 413] in Java 18, in
combination with the Compiler API included as part of the JDK.

This is implemented as a Doclet that is called by the javadoc tool. The Doclet
scans the syntax tree to find doc comments that include snippets with the
"test" attribute. It wraps the snippet in a small Java wrapper in memory,
compiles the wrapped test code, loads the resulting test class, and executes
it. The test passes if the code runs without throwing an exception.

# Tests

A snippet test can appear anywhere a snippet can be used- typically this would
be a documentation comment for a package, class, method, or field. For example:

```java
public class Calculator {

  /**
   * Add two numbers.
   *
   * {@snippet: test
   *   int a = 2;
   *   int b = 3;
   *   assert a + b == 5
   * }
   */
  public int add(int a, int b) {
    return a + b;
  }
}
```

The javadoc tool will include this in the generated documentation.  The tests
can be run by specifying the JavadocTestDoclet when running the tool:

```sh
javadoc \
  -quiet \
  -docletpath /path/to/javadoctest-*.jar \
  -doclet joev.javadoctest.JavaDocTestDoclet \
  -sourcepath /path/to/source \
  -J-enableassertions \
  -J-Djavadoctest.classpath=/path/to/classes \
  package.name
```

The doclet accepts an optional `import` attribute, which will cause the
generated Java test class to include import directives. This can reduce
clutter in the test, so package names do not need to appear in the test case.
An import for the containing package is also included. For example:

```java
/**
 *
 * {@snippet test import=java.util.* :
 *   List<String> list = new ArrayList<>();
 *   list.add("Test");
 *
 *   assert list.length() == 1
 * }
 */
```


[JEP 413]: https://openjdk.org/jeps/413
