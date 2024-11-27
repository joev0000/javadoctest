/*
 * Copyright (c) 2024 Joseph Vigneau
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

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
