all: classes

classes:
	mkdir -p classes
	javac -classpath classes:jna-3.5.1.jar -d classes $$(find src -name "*.java")

test: classes
	java -classpath classes:jna-3.5.1.jar foo.Main

clean: FORCE
	rm -rf classes

jna: FORCE
	wget -q https://maven.java.net/content/repositories/releases/net/java/dev/jna/jna/3.5.1/jna-3.5.1.jar

FORCE:
