set CLASSPATH=.;classes;lib\joquery-0.15.jar;lib\log4j-1.2.17.jar
set SOURCEPATH=source
javac -sourcepath %SOURCEPATH% -d classes %SOURCEPATH%\*.java

java PatternsParser %SOURCEPATH%\Patterns.txt c:/raheli/input output/OutputReport.html source