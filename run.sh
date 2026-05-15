#/bin/bash
set -x
javadoc \
  -quiet \
  -docletpath build/libs/javadoctest-*.jar \
  -doclet joev.javadoctest.JavaDocTestDoclet \
  -sourcepath src/main/java \
  -J-enableassertions \
  -J-Djavadoctest.classpath=build/classes/java/main\
  joev.javadoctest
