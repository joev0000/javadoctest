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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A FileVisitor that deletes the files and directories it encounters.
 *
 * {@snippet :
 * Path dir = ...;
 * try {
 *   Files.walkFileTree(dir, DeleteTreeFileVisitor.instance());
 * }
 * catch (IOException ioe) {
 *   // handle exception
 * }
 * }
 */
public class DeleteTreeFileVisitor implements FileVisitor<Path> {

  /** The singleton instance of DeleteTreeFileVisitor. */
  private static final DeleteTreeFileVisitor instance = new DeleteTreeFileVisitor();

  /** Private constructor to prevent instantiation. */
  private DeleteTreeFileVisitor() {}

  /**
   * Gets the singleton instance of the DeleteTreeFileVisitor.
   *
   * @return the singleton instance of the DeleteTreeFileVisotor.
   */
  public static DeleteTreeFileVisitor instance() {
    return instance;
  }

  /**
   * Deletes the directory after all of the child files and directories have been deleted.
   *
   * @return FileVisitResult.CONTINUE;
   */
  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    if (exc != null) {
      throw exc;
    }
    Files.delete(dir);
    return FileVisitResult.CONTINUE;
  }

  /**
   * Called before the child files and directories have been visited. This implementation always
   * returns CONTINUE.
   *
   * @return FileVisitResult.CONTINUE
   */
  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    return FileVisitResult.CONTINUE;
  }

  /**
   * Deletes the file with the given path.
   *
   * @return FileVisitResult.CONTINUE
   */
  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    Files.delete(file);
    return FileVisitResult.CONTINUE;
  }

  /**
   * Called when visting a path fails. This implementation rethrows the provided exception.
   *
   * @return FileVisitResult.TERMINATE
   */
  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    if (exc != null) {
      throw exc;
    }
    return FileVisitResult.TERMINATE;
  }
}
