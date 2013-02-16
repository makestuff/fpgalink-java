all: classes

classes:
	mkdir -p classes
	javac -classpath classes:jna-3.5.1.jar -d classes $$(find src -name "*.java")

test: classes
	java -classpath classes:jna-3.5.1.jar foo.Main

clean: FORCE
	rm -rf classes

FORCE:
