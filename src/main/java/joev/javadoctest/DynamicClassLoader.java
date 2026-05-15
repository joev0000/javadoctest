package joev.javadoctest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

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
    throw new ClassNotFoundException(name);
  }
}
