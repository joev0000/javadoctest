/*
 * Copyright (c) 2024-2026 Joseph Vigneau
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.joev.javadoctest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A ClassLoader that loads classes by checking the directories in the classpath.
 */
public class DynamicClassLoader extends ClassLoader {
  private List<Path> classpath;

  /**
   * Create a classloader with the given classpath.
   *
   * @param classpath the list of paths, in the order they will be searched.
   */
  public DynamicClassLoader(List<Path> classpath) {
    this.classpath = classpath == null ? Collections.emptyList() : classpath;
  }

  /**
   * Find the class with the given name by searching through each path in the classpath.
   *
   * @param name the name of the class to load.
   * @return the defined Class.
   * @throws ClassNotFoundException if a file with the given name isn't found.
   */
  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    String fileName = name.replace(".", "/") + ".class";
    for (Path path : classpath) {
      if (path.toString().endsWith(".jar") && Files.exists(path)) {
        try (JarFile jar = new JarFile(path.toString())) {
          JarEntry entry = jar.getJarEntry(fileName);
          if (entry != null) {
            byte[] b = jar.getInputStream(entry).readAllBytes();
            return defineClass(name, b, 0, b.length);
          }
        } catch (IOException ioe) {
          // do something
          ioe.printStackTrace();
        }

      } else {
        Path filePath = path.resolve(fileName);
        if (Files.exists(filePath)) {
          try {
            byte[] b = Files.readAllBytes(filePath);
            return defineClass(name, b, 0, b.length);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return ClassLoader.getSystemClassLoader().loadClass(name);
  }
}
