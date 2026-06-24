/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * JSR 269 annotation processor that finds {@link CopilotTool}-annotated methods
 * and generates {@code $$CopilotToolMeta} companion classes containing tool
 * definitions, JSON Schema, and invocation lambdas.
 *
 * <p>
 * For a class {@code com.example.MyTools} containing {@code @CopilotTool}
 * methods, this processor generates
 * {@code com.example.MyTools$$CopilotToolMeta} in the same package.
 *
 * @since 1.0.2
 */
@SupportedAnnotationTypes("com.github.copilot.tool.CopilotTool")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CopilotToolProcessor extends AbstractProcessor {

    private final SchemaGenerator schemaGenerator = new SchemaGenerator();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(CopilotTool.class)) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) element;

            // Validate: private methods are not allowed
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@CopilotTool methods must not be private", method);
                continue;
            }

            // Validate @Param conflicts
            for (VariableElement param : method.getParameters()) {
                Param paramAnnotation = param.getAnnotation(Param.class);
                if (paramAnnotation != null && paramAnnotation.required()
                        && !paramAnnotation.defaultValue().isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@Param cannot have both required=true and a non-empty defaultValue", param);
                }
            }
        }

        // Group methods by enclosing type
        Map<TypeElement, List<ExecutableElement>> methodsByClass = new LinkedHashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(CopilotTool.class)) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) element;
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                continue;
            }
            TypeElement enclosingType = (TypeElement) method.getEnclosingElement();
            methodsByClass.computeIfAbsent(enclosingType, k -> new ArrayList<>()).add(method);
        }

        // Generate $$CopilotToolMeta for each class
        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : methodsByClass.entrySet()) {
            generateMetaClass(entry.getKey(), entry.getValue());
        }

        return false;
    }

    private void generateMetaClass(TypeElement classElement, List<ExecutableElement> methods) {
        String packageName = processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString();
        String simpleClassName = classElement.getSimpleName().toString();
        String metaClassName = simpleClassName + "$$CopilotToolMeta";
        String qualifiedMetaClassName = packageName.isEmpty() ? metaClassName : packageName + "." + metaClassName;

        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(qualifiedMetaClassName, classElement);
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                writeMetaClass(out, packageName, simpleClassName, metaClassName, classElement, methods);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate " + metaClassName + ": " + e.getMessage(), classElement);
        }
    }

    private void writeMetaClass(PrintWriter out, String packageName, String simpleClassName, String metaClassName,
            TypeElement classElement, List<ExecutableElement> methods) {
        out.println("// GENERATED by CopilotToolProcessor — do not edit");

        if (!packageName.isEmpty()) {
            out.println("package " + packageName + ";");
            out.println();
        }

        out.println("import com.github.copilot.rpc.ToolDefinition;");
        out.println("import com.github.copilot.rpc.ToolDefer;");
        out.println("import com.fasterxml.jackson.databind.ObjectMapper;");
        out.println("import java.util.*;");
        out.println("import java.util.concurrent.CompletableFuture;");
        out.println();

        out.println("final class " + metaClassName + " {");
        out.println();

        // Helper method for adding description/default to schema maps
        if (needsWithMetaHelper(methods)) {
            out.println(
                    "    private static Map<String, Object> withMeta(Map<String, Object> base, String description, String defaultValue) {");
            out.println("        var result = new LinkedHashMap<String, Object>(base);");
            out.println("        if (description != null) result.put(\"description\", description);");
            out.println("        if (defaultValue != null) result.put(\"default\", defaultValue);");
            out.println("        return Collections.unmodifiableMap(result);");
            out.println("    }");
            out.println();
        }

        // definitions method
        out.println("    @SuppressWarnings({\"unchecked\", \"rawtypes\"})");
        out.println(
                "    static List<ToolDefinition> definitions(" + simpleClassName + " instance, ObjectMapper mapper) {");
        out.println("        return List.of(");

        for (int i = 0; i < methods.size(); i++) {
            ExecutableElement method = methods.get(i);
            writeToolDefinition(out, method);
            if (i < methods.size() - 1) {
                out.println(",");
            } else {
                out.println();
            }
        }

        out.println("        );");
        out.println("    }");
        out.println("}");
    }

    private boolean needsWithMetaHelper(List<ExecutableElement> methods) {
        for (ExecutableElement method : methods) {
            for (VariableElement param : method.getParameters()) {
                Param paramAnnotation = param.getAnnotation(Param.class);
                if (paramAnnotation != null
                        && (!paramAnnotation.value().isEmpty() || !paramAnnotation.defaultValue().isEmpty())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void writeToolDefinition(PrintWriter out, ExecutableElement method) {
        CopilotTool annotation = method.getAnnotation(CopilotTool.class);
        String toolName = annotation.name().isEmpty()
                ? toSnakeCase(method.getSimpleName().toString())
                : annotation.name();
        String description = annotation.value();
        boolean overridesBuiltIn = annotation.overridesBuiltInTool();
        boolean skipPermission = annotation.skipPermission();
        com.github.copilot.rpc.ToolDefer defer = annotation.defer();

        // Generate schema with @Param metadata (descriptions, names, defaults)
        String schemaSource = generateSchemaWithParamMetadata(method.getParameters());

        // Generate invocation lambda
        String lambdaBody = generateLambdaBody(method);

        // Determine factory method and arguments
        out.print("            ");
        if (overridesBuiltIn) {
            out.println("ToolDefinition.createOverride(");
        } else if (skipPermission) {
            out.println("ToolDefinition.createSkipPermission(");
        } else if (defer != com.github.copilot.rpc.ToolDefer.NONE) {
            out.println("ToolDefinition.createWithDefer(");
        } else {
            out.println("ToolDefinition.create(");
        }

        out.println("                \"" + escapeJava(toolName) + "\",");
        out.println("                \"" + escapeJava(description) + "\",");
        out.println("                " + schemaSource + ",");
        out.println("                invocation -> {");
        out.println("                    " + lambdaBody);
        out.println("                }");

        // Add defer parameter if needed
        if (defer != com.github.copilot.rpc.ToolDefer.NONE && !overridesBuiltIn && !skipPermission) {
            out.println("                , ToolDefer." + defer.name());
        }

        out.print("            )");
    }

    private String generateSchemaWithParamMetadata(List<? extends VariableElement> parameters) {
        if (parameters.isEmpty()) {
            return "Map.of(\"type\", \"object\", \"properties\", Map.of(), \"required\", List.of())";
        }

        List<String> propertyEntries = new ArrayList<>();
        List<String> requiredNames = new ArrayList<>();

        for (VariableElement param : parameters) {
            String paramName = getParamName(param);
            TypeMirror paramType = param.asType();
            Param paramAnnotation = param.getAnnotation(Param.class);

            // Generate the type schema for this parameter
            String typeSchema = schemaGenerator.generateSchemaSource(paramType, processingEnv.getTypeUtils(),
                    processingEnv.getElementUtils());

            // Build property schema with description and default if present
            String propertySchema = buildPropertySchema(typeSchema, paramAnnotation);

            // Cast to Map<String, Object> via raw type for consistent Map.ofEntries typing
            propertyEntries.add("Map.entry(\"" + paramName + "\", (Map<String, Object>)(Map) " + propertySchema + ")");

            // Determine if required
            if (paramAnnotation == null || paramAnnotation.required()) {
                requiredNames.add("\"" + paramName + "\"");
            }
        }

        String properties = "Map.ofEntries(" + String.join(", ", propertyEntries) + ")";
        String required = "List.of(" + String.join(", ", requiredNames) + ")";

        return "Map.of(\"type\", \"object\", \"properties\", " + properties + ", \"required\", " + required + ")";
    }

    private String buildPropertySchema(String typeSchema, Param paramAnnotation) {
        if (paramAnnotation == null) {
            return typeSchema;
        }

        String desc = paramAnnotation.value();
        String defaultValue = paramAnnotation.defaultValue();

        boolean hasDescription = !desc.isEmpty();
        boolean hasDefault = !defaultValue.isEmpty();

        if (!hasDescription && !hasDefault) {
            return typeSchema;
        }

        // Use the withMeta helper method in the generated class
        String descArg = hasDescription ? "\"" + escapeJava(desc) + "\"" : "null";
        String defaultArg = hasDefault ? "\"" + escapeJava(defaultValue) + "\"" : "null";

        return "withMeta(" + typeSchema + ", " + descArg + ", " + defaultArg + ")";
    }

    private String generateLambdaBody(ExecutableElement method) {
        List<? extends VariableElement> params = method.getParameters();
        StringBuilder sb = new StringBuilder();

        // Generate argument extraction
        if (!params.isEmpty()) {
            sb.append("Map<String, Object> args = invocation.getArguments();\n");

            // Check if single-record-parameter shortcut applies
            if (params.size() == 1 && isRecordOrPojo(params.get(0).asType())) {
                String typeName = getTypeString(params.get(0).asType());
                String paramName = params.get(0).getSimpleName().toString();
                sb.append("                    ").append(typeName).append(" ").append(paramName)
                        .append(" = invocation.getArgumentsAs(").append(typeName).append(".class);\n");
            } else {
                for (VariableElement param : params) {
                    String paramName = getParamName(param);
                    String varName = param.getSimpleName().toString();
                    TypeMirror paramType = param.asType();

                    // Handle default values
                    Param paramAnnotation = param.getAnnotation(Param.class);
                    boolean hasDefault = paramAnnotation != null && !paramAnnotation.defaultValue().isEmpty();

                    if (hasDefault) {
                        String defaultValue = paramAnnotation.defaultValue();
                        sb.append("                    Object ").append(varName).append("Raw = args.containsKey(\"")
                                .append(paramName).append("\") ? args.get(\"").append(paramName).append("\") : ")
                                .append(generateDefaultLiteral(paramType, defaultValue)).append(";\n");
                        sb.append("                    ").append(getTypeString(paramType)).append(" ").append(varName)
                                .append(" = ").append(generateArgExtraction(varName + "Raw", paramType)).append(";\n");
                    } else {
                        sb.append("                    ").append(getTypeString(paramType)).append(" ").append(varName)
                                .append(" = ").append(generateArgExtractionFromMap(paramName, paramType)).append(";\n");
                    }
                }
            }
        }

        // Generate method invocation based on return type
        TypeMirror returnType = method.getReturnType();
        String methodCall = "instance." + method.getSimpleName() + "(" + generateArgList(params) + ")";

        if (returnType.getKind() == TypeKind.VOID) {
            sb.append("                    ").append(methodCall).append(";\n");
            sb.append("                    return CompletableFuture.completedFuture(\"Success\");");
        } else if (isCompletableFuture(returnType)) {
            TypeMirror typeArg = getCompletableFutureTypeArg(returnType);
            if (typeArg != null && isStringType(typeArg)) {
                // CompletableFuture<String> -> CompletableFuture<Object> via thenApply
                sb.append("                    return ").append(methodCall).append(".thenApply(r -> (Object) r);");
            } else {
                // CompletableFuture<T> -> serialize to JSON
                sb.append("                    return ").append(methodCall)
                        .append(".thenApply(r -> { try { return (Object) mapper.writeValueAsString(r); }")
                        .append(" catch (Exception e) { throw new RuntimeException(e); } });");
            }
        } else if (isStringType(returnType)) {
            sb.append("                    return CompletableFuture.completedFuture(").append(methodCall).append(");");
        } else {
            sb.append("                    try { return CompletableFuture.completedFuture(mapper.writeValueAsString(")
                    .append(methodCall).append(")); } catch (Exception e) { throw new RuntimeException(e); }");
        }

        return sb.toString();
    }

    private String generateArgList(List<? extends VariableElement> params) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params.get(i).getSimpleName().toString());
        }
        return sb.toString();
    }

    private String generateArgExtractionFromMap(String paramName, TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return generatePrimitiveExtraction("args.get(\"" + paramName + "\")", type);
        }
        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
            String qualifiedName = typeElement.getQualifiedName().toString();
            if ("java.lang.String".equals(qualifiedName)) {
                return "(String) args.get(\"" + paramName + "\")";
            }
            if (isBoxedNumeric(qualifiedName)) {
                return generateBoxedNumericExtraction("args.get(\"" + paramName + "\")", qualifiedName);
            }
            if ("java.lang.Boolean".equals(qualifiedName)) {
                return "(Boolean) args.get(\"" + paramName + "\")";
            }
            // Complex types: enums, records, POJOs
            return "mapper.convertValue(args.get(\"" + paramName + "\"), " + qualifiedName + ".class)";
        }
        return "(Object) args.get(\"" + paramName + "\")";
    }

    private String generateArgExtraction(String varExpr, TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return generatePrimitiveExtraction(varExpr, type);
        }
        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
            String qualifiedName = typeElement.getQualifiedName().toString();
            if ("java.lang.String".equals(qualifiedName)) {
                return "(String) " + varExpr;
            }
            if (isBoxedNumeric(qualifiedName)) {
                return generateBoxedNumericExtraction(varExpr, qualifiedName);
            }
            if ("java.lang.Boolean".equals(qualifiedName)) {
                return "(Boolean) " + varExpr;
            }
            return "mapper.convertValue(" + varExpr + ", " + qualifiedName + ".class)";
        }
        return "(Object) " + varExpr;
    }

    private String generatePrimitiveExtraction(String expr, TypeMirror type) {
        switch (type.getKind()) {
            case INT :
                return "((Number) " + expr + ").intValue()";
            case LONG :
                return "((Number) " + expr + ").longValue()";
            case DOUBLE :
                return "((Number) " + expr + ").doubleValue()";
            case FLOAT :
                return "((Number) " + expr + ").floatValue()";
            case SHORT :
                return "((Number) " + expr + ").shortValue()";
            case BYTE :
                return "((Number) " + expr + ").byteValue()";
            case BOOLEAN :
                return "(Boolean) " + expr;
            case CHAR :
                return "((String) " + expr + ").charAt(0)";
            default :
                return "(" + type + ") " + expr;
        }
    }

    private boolean isBoxedNumeric(String qualifiedName) {
        return "java.lang.Integer".equals(qualifiedName) || "java.lang.Long".equals(qualifiedName)
                || "java.lang.Double".equals(qualifiedName) || "java.lang.Float".equals(qualifiedName)
                || "java.lang.Short".equals(qualifiedName) || "java.lang.Byte".equals(qualifiedName);
    }

    private String generateBoxedNumericExtraction(String expr, String qualifiedName) {
        switch (qualifiedName) {
            case "java.lang.Integer" :
                return "((Number) " + expr + ").intValue()";
            case "java.lang.Long" :
                return "((Number) " + expr + ").longValue()";
            case "java.lang.Double" :
                return "((Number) " + expr + ").doubleValue()";
            case "java.lang.Float" :
                return "((Number) " + expr + ").floatValue()";
            case "java.lang.Short" :
                return "((Number) " + expr + ").shortValue()";
            case "java.lang.Byte" :
                return "((Number) " + expr + ").byteValue()";
            default :
                return "(" + qualifiedName + ") " + expr;
        }
    }

    private String generateDefaultLiteral(TypeMirror type, String defaultValue) {
        if (type.getKind().isPrimitive()) {
            switch (type.getKind()) {
                case INT :
                case LONG :
                case SHORT :
                case BYTE :
                    return defaultValue;
                case DOUBLE :
                case FLOAT :
                    return defaultValue;
                case BOOLEAN :
                    return defaultValue;
                case CHAR :
                    return "\"" + escapeJava(defaultValue) + "\"";
                default :
                    return "\"" + escapeJava(defaultValue) + "\"";
            }
        }
        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
            String qualifiedName = typeElement.getQualifiedName().toString();
            if ("java.lang.String".equals(qualifiedName)) {
                return "\"" + escapeJava(defaultValue) + "\"";
            }
            if (isBoxedNumeric(qualifiedName) || "java.lang.Boolean".equals(qualifiedName)) {
                return defaultValue;
            }
        }
        return "\"" + escapeJava(defaultValue) + "\"";
    }

    private String getParamName(VariableElement param) {
        Param paramAnnotation = param.getAnnotation(Param.class);
        if (paramAnnotation != null && !paramAnnotation.name().isEmpty()) {
            return paramAnnotation.name();
        }
        return param.getSimpleName().toString();
    }

    private String getTypeString(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return type.toString();
        }
        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
            return typeElement.getQualifiedName().toString();
        }
        return type.toString();
    }

    private boolean isRecordOrPojo(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getKind() == ElementKind.RECORD || (typeElement.getKind() == ElementKind.CLASS
                && !isSimpleType(typeElement.getQualifiedName().toString()));
    }

    private boolean isSimpleType(String qualifiedName) {
        return "java.lang.String".equals(qualifiedName) || "java.lang.Integer".equals(qualifiedName)
                || "java.lang.Long".equals(qualifiedName) || "java.lang.Double".equals(qualifiedName)
                || "java.lang.Float".equals(qualifiedName) || "java.lang.Boolean".equals(qualifiedName)
                || "java.lang.Short".equals(qualifiedName) || "java.lang.Byte".equals(qualifiedName)
                || "java.lang.Character".equals(qualifiedName) || "java.lang.Object".equals(qualifiedName);
    }

    private boolean isCompletableFuture(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return "java.util.concurrent.CompletableFuture".equals(typeElement.getQualifiedName().toString());
    }

    private TypeMirror getCompletableFutureTypeArg(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        DeclaredType declaredType = (DeclaredType) type;
        List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
        if (typeArgs.isEmpty()) {
            return null;
        }
        return typeArgs.get(0);
    }

    private boolean isStringType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return "java.lang.String".equals(typeElement.getQualifiedName().toString());
    }

    /**
     * Converts a camelCase method name to snake_case.
     *
     * @param name
     *            the method name
     * @return the snake_case tool name
     */
    static String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeJava(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
                "\\t");
    }
}
