package ba.ekapic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Builder classes for classes annotated with {@link ba.ekapic.Builder}. Builder classes have methods
 * of form withX(T X) for every field X of said classes.
 */
@SupportedAnnotationTypes("ba.ekapic.Builder")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class BuilderAnnotationProcessor extends AbstractProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(BuilderAnnotationProcessor.class);

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		for (final TypeElement annotation : annotations) {
			final Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

			for (final Element element : annotatedElements) {
				if (ElementKind.CLASS != element.getKind()) {
					LOGGER.warn("Detected @Builder annotation on element %s which is not a class. Ignoring it.".formatted(element.getSimpleName()));
					continue;
				}

				final Set<Element> fieldElements = element.getEnclosedElements().stream()
						.filter(el -> ElementKind.FIELD == el.getKind())
						.filter(el -> el.getAnnotation(BuilderIgnore.class) == null)
						.collect(Collectors.toSet());

				final Map<String, String> fields = fieldElements.stream().collect(Collectors.toMap(
						key -> key.getSimpleName().toString(),
						value -> value.asType().toString()
				));

				// Qualified name = package name + class name e.g. ba.ekapic.TestClass
				final String qualifiedName = ((QualifiedNameable) element).getQualifiedName().toString();

				generate(element, qualifiedName, fields);
			}
		}

		// Allow other processors to handle the annotation.
		return false;
	}

	private void generate(final Element clazz, final String qualifiedName, final Map<String, String> fields) {
		try{
			final String generatedInstant = Clock.systemUTC().instant().toString();

			final String fileName = qualifiedName + "Builder";
			final JavaFileObject sourceFile = this.processingEnv.getFiler().createSourceFile(fileName);

			final int lastDotIndex = fileName.lastIndexOf(".");

			final String metaClassName = fileName.substring(lastDotIndex + 1);

			try(final PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
				out.println("package " + fileName.substring(0, lastDotIndex) + ";");
				out.println();

				out.println("import javax.annotation.processing.Generated;");
				out.println("import java.lang.reflect.Field;");
				out.println();

				out.println("@Generated(value = \"" + this.getClass().getName() + "\", date = \"" + generatedInstant + "\")");
				out.println("public class " + metaClassName + " {");

				generateBuilderProperties(fields, out);
				out.println();

				generateWithMethods(metaClassName, fields, out);

				generateBuildMethod(clazz, qualifiedName, fields, out);

				out.println("}");
			}
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	private void generateBuilderProperties(final Map<String, String> fields, final PrintWriter out) {
		for (final Map.Entry<String, String> entry : fields.entrySet()) {
			out.println("    private " + entry.getValue() + " " + entry.getKey() + ";");
		}
	}

	private void generateWithMethods(final String metaClassName,
									 final Map<String, String> fields,
									 final PrintWriter out) {
		for (final Map.Entry<String, String> entry : fields.entrySet()) {
			out.println("	public " + metaClassName + " with" + camelCaseToPascalCase(entry.getKey())
						+ "(" + entry.getValue() + " " + entry.getKey() + ") {");

			out.println("		this." + entry.getKey() + " = " + entry.getKey() + ";");
			out.println("		return this;");
			out.println("	}");
			out.println();
		}
	}

	private void generateBuildMethod(final Element classElement,
									 final String className,
									 final Map<String, String> fields,
									 final PrintWriter out) {
		out.println("	public " + className + " build() {");
		out.println("		final " + className + " model = new " + className + "();");
		out.println();

		out.println("		try {");
		for (final Map.Entry<String, String> field : fields.entrySet()) {
			out.println("			final Field " + field.getKey() + " = model.getClass().getDeclaredField(\"" + field.getKey() + "\");");
			out.println("			" + field.getKey() + ".setAccessible(true);");
			out.println("			" + field.getKey() + ".set(model, " + "this." + field.getKey() + ");");
			out.println();
		}
		out.println("		} catch(NoSuchFieldException | IllegalAccessException e) {");
		out.println("			//");
		out.println("		}");

		out.println();
		out.println("		return model;");
		out.println("	}");
	}

	private String camelCaseToPascalCase(final String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
