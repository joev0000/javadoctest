#!/usr/bin/env bash
set -x
javadoc -docletpath /home/joev/git/javadoctest/build/libs/javadoctest-0.1.jar -doclet joev.javadoctest.JavaDocTestDoclet -sourcepath src/main/java -J-Djavadoctest.classpath=build/classes/java/main calculator
