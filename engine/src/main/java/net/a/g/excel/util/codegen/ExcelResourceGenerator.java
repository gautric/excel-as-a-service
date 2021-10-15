package net.a.g.excel.util.codegen;

// https://github.com/square/javapoet
import java.io.File;
import java.io.IOException;

import javax.lang.model.element.Modifier;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

public class ExcelResourceGenerator {
	public static void main(String[] args) throws Exception {

		createResourceClass("kyc");
		createResourceClass("tio");
		createInputClass("kyc");
		createInputClass("tio");

	}

	public static void createInputClass(String name) throws IOException {
		// AnnotationSpec path = AnnotationSpec.builder(Path.class).addMember("value",
		// "$S", "/i").build();

		FieldSpec field = FieldSpec.builder(String.class, "attr").build();
		FieldSpec android = FieldSpec.builder(String.class, "android").addModifiers(Modifier.PRIVATE).build();

		TypeSpec helloWorld = TypeSpec.classBuilder("Excel" + name.toUpperCase() + "Input").addField(field)
				.addField(android).addModifiers(Modifier.PUBLIC, Modifier.FINAL).build();

		JavaFile javaFile = JavaFile.builder("net.a.g.excel.instance.model", helloWorld).build();

		javaFile.writeTo(System.out);

		javaFile.writeToFile(
				new File("/Users/gautric/Source/git/quarkus/excel-app/engine/target/generated-sources/main/java"));
	}

	public static void createOutputClass(String name) throws IOException {

		TypeSpec helloWorld = TypeSpec.classBuilder("Excel" + name.toUpperCase() + "Output")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL).build();

		JavaFile javaFile = JavaFile.builder("net.a.g.excel.instance.model", helloWorld).build();

		javaFile.writeTo(System.out);

		javaFile.writeToFile(
				new File("/Users/gautric/Source/git/quarkus/excel-app/engine/target/generated-sources/main/java"));
	}

	public static void createResourceClass(String name) throws IOException {
		AnnotationSpec path = AnnotationSpec.builder(Path.class).addMember("value", "$S", "/i").build();

		TypeSpec helloWorld = TypeSpec.classBuilder("Excel" + name.toUpperCase() + "InstanceResource")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL).addMethod(generatePOSTMethod(name))
				.addMethod(generateGETMethod(name)).addAnnotation(path).build();

		JavaFile javaFile = JavaFile.builder("net.a.g.excel.instance.rest", helloWorld).build();

		javaFile.writeTo(System.out);

		javaFile.writeToFile(
				new File("/Users/gautric/Source/git/quarkus/excel-app/engine/target/generated-sources/main/java"));
	}

	public static MethodSpec generateGETMethod(String name) {
		AnnotationSpec instancePath = AnnotationSpec.builder(Path.class).addMember("value", "$S", name).build();
		AnnotationSpec instanceGet = AnnotationSpec.builder(GET.class).build();
		AnnotationSpec operation = AnnotationSpec.builder(Operation.class)
				.addMember("summary", "$S", name.toUpperCase() + " Operation Summary")
				.addMember("description", "$S", name.toUpperCase() + " Operation Description").build();

		MethodSpec main = MethodSpec.methodBuilder(name + "_GET").addModifiers(Modifier.PUBLIC).returns(Response.class)
				// .addParameter(String[].class, "args")
				.addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
				.addStatement("return $T.ok().build()", Response.class).addAnnotation(instancePath)
				.addAnnotation(operation).addAnnotation(instanceGet).build();
		return main;
	}

	public static MethodSpec generatePOSTMethod(String name) {
		AnnotationSpec instancePath = AnnotationSpec.builder(Path.class).addMember("value", "$S", name).build();
		AnnotationSpec instanceGet = AnnotationSpec.builder(POST.class).build();
		AnnotationSpec operation = AnnotationSpec.builder(Operation.class)
				.addMember("summary", "$S", name.toUpperCase() + " Operation Summary")
				.addMember("description", "$S", name.toUpperCase() + " Operation Description").build();

		MethodSpec main = MethodSpec.methodBuilder(name + "_POST").addModifiers(Modifier.PUBLIC).returns(Response.class)
				// .addParameter(String[].class, "args")
				.addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
				.addStatement("return $T.ok().build()", Response.class).addAnnotation(instancePath)
				.addAnnotation(operation).addAnnotation(instanceGet).build();
		return main;
	}

}
