package joev.javadoctest;

/**
 * The result of running a test.
 *
 * @param pass The number of passed tests.
 * @param fail The number of failed tests.
 * @param skip The number of skipped tests.
 */
public record TestResult(int pass, int fail, int skip) {
  /** A TestResult with no passing, failing, or skipped tests. */
  public static final TestResult ZERO = new TestResult(0,0,0);

  /** A TestResult with a single passing test. */
  public static final TestResult PASS = new TestResult(1,0,0);

  /** A TestResult with a single failed test. */
  public static final TestResult FAIL = new TestResult(0,1,0);

  /** A TestResult with a single skipped test. */
  public static final TestResult SKIP = new TestResult(0,0,1);

  /**
   * Reduce two TestResults into one, by adding their components.
   *
   * {@snippet test :
   * TestResult tr1 = new TestResult(1, 2, 3);
   * TestResult tr2 = new TestResult(10, 20, 30);
   * TestResult reduced = tr1.reduce(tr2);
   *
   * assert reduced.equals(new TestResult(11, 22, 33));
   * }
   *
   * @param other the other TestResult to reduce with this one.
   * @return The reduced TestResult.
   */
  public TestResult reduce(TestResult other) {
    if (other == ZERO || other == null) {
      return this;
    }
    if (this == ZERO) {
      return other;
    }
    return new TestResult(this.pass + other.pass, this.fail + other.fail, this.skip + other.skip);
  }
}
