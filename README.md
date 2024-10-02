# README

A collection of generic and not necessarily connected libs for everyday Java development.
Generally assumes Java 21.

## Builder
Annotation Processor that generates builder classes based on classes annotated with
the `Builder` annotation. Also offers `BuilderIgnore` annotation if you want to 
exclude some property from the builder.

##### **TODO work:** 
A future improvement that should be made is checking if fields have setters. Using reflection is generally
slower so if setters exist we should use those. Additionally we should check if the class is a Java Record. In that case
we should instead search for the canonical constructor using Element API and use that instead of setting fields via
reflection.