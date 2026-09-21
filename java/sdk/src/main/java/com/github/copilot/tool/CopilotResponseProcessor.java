/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.CopilotResponse;

/**
 * Generates response metadata using the existing custom-tool schema generator.
 */
@SupportedAnnotationTypes("com.github.copilot.CopilotResponse")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@CopilotExperimental
public class CopilotResponseProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(CopilotResponse.class)) {
            if (!(element instanceof TypeElement type)
                    || (type.getKind() != ElementKind.CLASS && type.getKind() != ElementKind.RECORD)
                    || type.getModifiers().contains(Modifier.PRIVATE) || !type.getTypeParameters().isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@CopilotResponse requires an accessible, non-generic class or record", element);
                continue;
            }
            String binaryName = processingEnv.getElementUtils().getBinaryName(type).toString();
            String packageName = processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
            String metadataName = binaryName.substring(packageName.isEmpty() ? 0 : packageName.length() + 1)
                    + "$$CopilotResponseMeta";
            String qualifiedName = packageName.isEmpty() ? metadataName : packageName + "." + metadataName;
            String schema;
            try {
                schema = new SchemaGenerator(true).generateSchemaSource(type.asType(), processingEnv.getTypeUtils(),
                        processingEnv.getElementUtils());
            } catch (IllegalArgumentException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), type);
                continue;
            }
            try (var writer = new PrintWriter(
                    processingEnv.getFiler().createSourceFile(qualifiedName, type).openWriter())) {
                if (!packageName.isEmpty())
                    writer.println("package " + packageName + ";");
                writer.println("import java.util.Map;");
                writer.println("import java.util.List;");
                writer.println("public final class " + metadataName + " {");
                writer.println("public static Map<String, Object> schema() { return " + schema + "; }");
                writer.println("}");
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Cannot generate response schema: " + e.getMessage(), type);
            }
        }
        return true;
    }
}
