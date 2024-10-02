# README

A collection of generic and not necessarily connected libs for everyday Java development.
Generally assumes Java 21.

## Builder
Annotation Processor that generates builder classes based on classes annotated with
the `Builder` annotation. Also offers `BuilderIgnore` annotation if you want to 
exclude some property from the builder.

For example, class 
```java
@Builder
class Model {
	String var1;
	String var2;
	
	// Getters, setters, methods etc.
}
```
Will generate a builder that can be used as follows:
```java
ModelBuilder builder = new ModelBuilder();
builder.setVar1("123");
builder.setVar2("456");
Model m = builder.build();
```
The point of the library is that these builder classes are automatically generated once you compile your program and as such
allow convenient use of builders.

### TODO work: 
A future improvement that should be made is checking if fields have setters. Using reflection is generally
slower so if setters exist we should use those. Additionally we should check if the class is a Java Record. In that case
we should instead search for the canonical constructor using Element API and use that instead of setting fields via
reflection.