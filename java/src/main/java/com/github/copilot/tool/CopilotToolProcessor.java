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

import com.github.copilot.CopilotExperimental;

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
@CopilotExperimental
public class CopilotToolProcessor extends AbstractProcessor {

    private static final String TOOL_INVOCATION_TYPE = "com.github.copilot.rpc.ToolInvocation";

    private final SchemaGenerator schemaGenerator = new SchemaGenerator();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<Element> annotatedElements = getCopilotToolAnnotatedElements(roundEnv);
        for (Element element : annotatedElements) {
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

            // Validate @CopilotToolParam conflicts
            int toolInvocationParamCount = 0;
            for (VariableElement param : method.getParameters()) {
                if (isToolInvocationType(param.asType())) {
                    toolInvocationParamCount++;
                    if (param.getAnnotation(CopilotToolParam.class) != null) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "@CopilotToolParam is not supported on ToolInvocation parameters because ToolInvocation is injected runtime context and not part of the tool schema",
                                param);
                    }
                    continue;
                }
                CopilotToolParam paramAnnotation = param.getAnnotation(CopilotToolParam.class);
                if (paramAnnotation != null && paramAnnotation.required()
                        && !paramAnnotation.defaultValue().isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@CopilotToolParam cannot have both required=true and a non-empty defaultValue", param);
                }
                if (paramAnnotation != null && !paramAnnotation.defaultValue().isEmpty()) {
                    String defaultValidationError = validateDefaultValueCompatibility(param.asType(),
                            paramAnnotation.defaultValue());
                    if (defaultValidationError != null) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, defaultValidationError, param);
                    }
                }
                if (paramAnnotation != null && !paramAnnotation.required() && paramAnnotation.defaultValue().isEmpty()
                        && param.asType().getKind().isPrimitive()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@CopilotToolParam(required=false) primitive parameters must provide defaultValue or use a boxed/Optional type",
                            param);
                }
                if (paramAnnotation != null && !paramAnnotation.schema().isEmpty()
                        && !paramAnnotation.defaultValue().isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@CopilotToolParam cannot have both schema and defaultValue — express defaults inside the schema if needed",
                            param);
                }
                if (paramAnnotation != null && !paramAnnotation.schema().isEmpty()) {
                    String schemaJson = paramAnnotation.schema().trim();
                    if (!schemaJson.startsWith("{") || !schemaJson.endsWith("}")) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "@CopilotToolParam schema must be a valid JSON object string (must start with '{' and end with '}')",
                                param);
                    }
                }
            }
            if (toolInvocationParamCount > 1) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@CopilotTool methods may declare at most one ToolInvocation parameter; ToolInvocation is injected runtime context and not part of the tool schema",
                        method);
            }

            // Validate single-record wrapper parameter metadata
            List<? extends VariableElement> schemaParameters = getSchemaParameters(method.getParameters());
            if (schemaParameters.size() == 1) {
                VariableElement singleParam = schemaParameters.get(0);
                if (isRecord(singleParam.asType())) {
                    CopilotToolParam paramAnnotation = singleParam.getAnnotation(CopilotToolParam.class);
                    if (paramAnnotation != null) {
                        if (!paramAnnotation.defaultValue().isEmpty()) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "@CopilotToolParam(defaultValue=...) is not supported on single-record tool parameters; use record component defaults or a non-record parameter",
                                    singleParam);
                        }
                        if (!paramAnnotation.schema().isEmpty()) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "@CopilotToolParam(schema=...) is not supported on single-record tool parameters",
                                    singleParam);
                        }
                        if (!paramAnnotation.name().isEmpty() || !paramAnnotation.value().isEmpty()
                                || !paramAnnotation.required()) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "@CopilotToolParam name/value/required are not supported on single-record tool parameters; annotate record components instead",
                                    singleParam);
                        }
                    }
                }
            }

            // Validate blank @CopilotToolParam descriptions (exempt single-record wrappers)
            boolean isSingleRecordWrapper = schemaParameters.size() == 1 && isRecord(schemaParameters.get(0).asType());
            for (VariableElement param : schemaParameters) {
                if (isSingleRecordWrapper && param.equals(schemaParameters.get(0))) {
                    continue;
                }
                CopilotToolParam paramAnnotation = param.getAnnotation(CopilotToolParam.class);
                if (paramAnnotation != null && paramAnnotation.value().isBlank()) {
                    TypeElement enclosingClass = (TypeElement) method.getEnclosingElement();
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@CopilotToolParam on parameter '" + param.getSimpleName() + "' in '"
                                    + enclosingClass.getSimpleName() + "." + method.getSimpleName()
                                    + "' has a blank value (description). "
                                    + "Descriptions are required so the LLM can correctly select and invoke the tool",
                            param);
                }
            }
        }

        // Group methods by enclosing type
        Map<TypeElement, List<ExecutableElement>> methodsByClass = new LinkedHashMap<>();
        for (Element element : annotatedElements) {
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

    private List<Element> getCopilotToolAnnotatedElements(RoundEnvironment roundEnv) {
        TypeElement copilotToolType = processingEnv.getElementUtils()
                .getTypeElement("com.github.copilot.tool.CopilotTool");
        if (copilotToolType != null) {
            return new ArrayList<>(roundEnv.getElementsAnnotatedWith(copilotToolType));
        }
        return new ArrayList<>(roundEnv.getElementsAnnotatedWith(CopilotTool.class));
    }

    private void generateMetaClass(TypeElement classElement, List<ExecutableElement> methods) {
        String packageName = processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString();
        String simpleClassName = classElement.getSimpleName().toString();
        String metaClassName = simpleClassName + "$$CopilotToolMeta";
        String qualifiedMetaClassName = packageName.isEmpty() ? metaClassName : packageName + "." + metaClassName;

        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(qualifiedMetaClassName, classElement);
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                writeMetaClass(out, packageName, simpleClassName, metaClassName, methods);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate " + metaClassName + ": " + e.getMessage(), classElement);
        }
    }

    private void writeMetaClass(PrintWriter out, String packageName, String simpleClassName, String metaClassName,
            List<ExecutableElement> methods) {
        out.println("// GENERATED by CopilotToolProcessor — do not edit");

        if (!packageName.isEmpty()) {
            out.println("package " + packageName + ";");
            out.println();
        }

        out.println("import com.github.copilot.rpc.ToolDefinition;");
        out.println("import com.github.copilot.rpc.ToolDefer;");
        out.println("import com.github.copilot.tool.CopilotToolMetadataProvider;");
        out.println("import com.fasterxml.jackson.databind.ObjectMapper;");
        out.println("import java.util.*;");
        out.println("import java.util.concurrent.CompletableFuture;");
        out.println();

        out.println("public final class " + metaClassName + " implements CopilotToolMetadataProvider<" + simpleClassName
                + "> {");
        out.println();

        // Helper method for adding description/default to schema maps
        if (needsWithMetaHelper(methods)) {
            out.println(
                    "    private static Map<String, Object> withMeta(Map<String, Object> base, String description, Object defaultValue) {");
            out.println("        var result = new LinkedHashMap<String, Object>(base);");
            out.println("        if (description != null) result.put(\"description\", description);");
            out.println("        if (defaultValue != null) result.put(\"default\", defaultValue);");
            out.println("        return Collections.unmodifiableMap(result);");
            out.println("    }");
            out.println();
        }

        if (needsJsonSourceHelpers(methods)) {
            out.println("    private static Map<String, Object> mapOfNullable(Object... entries) {");
            out.println("        var result = new LinkedHashMap<String, Object>();");
            out.println("        for (int i = 0; i < entries.length; i += 2) {");
            out.println("            result.put((String) entries[i], entries[i + 1]);");
            out.println("        }");
            out.println("        return Collections.unmodifiableMap(result);");
            out.println("    }");
            out.println();
            out.println("    private static List<Object> listOfNullable(Object... items) {");
            out.println("        return Collections.unmodifiableList(Arrays.asList(items));");
            out.println("    }");
            out.println();
        }

        // definitions method
        out.println("    @Override");
        out.println("    @SuppressWarnings({\"unchecked\", \"rawtypes\"})");
        out.println(
                "    public List<ToolDefinition> definitions(" + simpleClassName + " instance, ObjectMapper mapper) {");
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
                CopilotToolParam paramAnnotation = param.getAnnotation(CopilotToolParam.class);
                if (paramAnnotation != null
                        && (!paramAnnotation.value().isEmpty() || !paramAnnotation.defaultValue().isEmpty())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean needsJsonSourceHelpers(List<ExecutableElement> methods) {
        for (ExecutableElement method : methods) {
            for (VariableElement param : method.getParameters()) {
                CopilotToolParam paramAnnotation = param.getAnnotation(CopilotToolParam.class);
                if (paramAnnotation != null && !paramAnnotation.schema().isEmpty()) {
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

        // Generate schema with @CopilotToolParam metadata (descriptions, names,
        // defaults)
        String schemaSource = generateSchemaWithParamMetadata(method.getParameters());

        // Generate invocation lambda
        String lambdaBody = generateLambdaBody(method);

        // Use the record constructor directly so all flags apply independently
        String overridesArg = overridesBuiltIn ? "Boolean.TRUE" : "null";
        String skipPermArg = skipPermission ? "Boolean.TRUE" : "null";
        String deferArg = defer != com.github.copilot.rpc.ToolDefer.NONE ? "ToolDefer." + defer.name() : "null";

        out.println("            new ToolDefinition(");
        out.println("                \"" + escapeJava(toolName) + "\",");
        out.println("                \"" + escapeJava(description) + "\",");
        out.println("                " + schemaSource + ",");
        out.println("                invocation -> {");
        out.println("                    " + lambdaBody);
        out.println("                },");
        out.println("                " + overridesArg + ",");
        out.println("                " + skipPermArg + ",");
        out.println("                " + deferArg + ",");
        out.println("                " + metadataSource(annotation));
        out.print("            )");
    }

    /**
     * Converts the {@code @CopilotTool(metadata = ...)} entries into a Java source
     * literal. Returns {@code "null"} when no metadata is present, otherwise a
     * {@code Map.<String, Object>of(...)} expression.
     */
    private String metadataSource(CopilotTool annotation) {
        CopilotTool.MetadataEntry[] entries = annotation.metadata();
        if (entries.length == 0) {
            return "null";
        }
        List<String> parts = new ArrayList<>();
        for (CopilotTool.MetadataEntry entry : entries) {
            parts.add("\"" + escapeJava(entry.key()) + "\", " + metadataValueSource(entry.value()));
        }
        return "Map.<String, Object>of(" + String.join(", ", parts) + ")";
    }

    /**
     * Converts a single {@link CopilotTool.MetadataValue} into a Java source
     * literal. A non-empty {@code flags} map takes precedence, then a non-empty
     * {@code str}, otherwise the {@code bool} scalar.
     */
    private String metadataValueSource(CopilotTool.MetadataValue value) {
        CopilotTool.MetadataFlag[] flags = value.flags();
        if (flags.length > 0) {
            List<String> flagParts = new ArrayList<>();
            for (CopilotTool.MetadataFlag flag : flags) {
                flagParts.add("\"" + escapeJava(flag.name()) + "\", " + flag.value());
            }
            return "Map.of(" + String.join(", ", flagParts) + ")";
        }
        if (!value.str().isEmpty()) {
            return "\"" + escapeJava(value.str()) + "\"";
        }
        return String.valueOf(value.bool());
    }

    private String generateSchemaWithParamMetadata(List<? extends VariableElement> parameters) {
        List<? extends VariableElement> schemaParameters = getSchemaParameters(parameters);

        if (schemaParameters.isEmpty()) {
            return "Map.of(\"type\", \"object\", \"properties\", Map.of(), \"required\", List.of())";
        }
        if (schemaParameters.size() == 1 && isRecord(schemaParameters.get(0).asType())) {
            return schemaGenerator.generateSchemaSource(schemaParameters.get(0).asType(), processingEnv.getTypeUtils(),
                    processingEnv.getElementUtils());
        }

        List<String> propertyEntries = new ArrayList<>();
        List<String> requiredNames = new ArrayList<>();

        for (VariableElement param : schemaParameters) {
            String paramName = getParamName(param);
            TypeMirror paramType = param.asType();
            CopilotToolParam paramAnnotation = param.getAnnotation(CopilotToolParam.class);

            // Generate the type schema for this parameter
            String typeSchema;
            if (paramAnnotation != null && !paramAnnotation.schema().isEmpty()) {
                try {
                    typeSchema = jsonToMapOfSource(paramAnnotation.schema());
                } catch (IllegalArgumentException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@CopilotToolParam schema is not valid JSON: " + e.getMessage(), param);
                    continue;
                }
            } else {
                typeSchema = schemaGenerator.generateSchemaSource(paramType, processingEnv.getTypeUtils(),
                        processingEnv.getElementUtils());
            }

            // Build property schema with description and default if present
            String propertySchema = buildPropertySchema(typeSchema, paramAnnotation, paramType);

            // Cast to Map<String, Object> via raw type for consistent Map.ofEntries typing
            propertyEntries.add("Map.entry(\"" + paramName + "\", (Map<String, Object>)(Map) " + propertySchema + ")");

            // Determine if required (Optional* types are never required)
            boolean isOptionalType = paramType.getKind() == TypeKind.DECLARED && Set
                    .of("java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong",
                            "java.util.OptionalDouble")
                    .contains(((TypeElement) ((DeclaredType) paramType).asElement()).getQualifiedName().toString());
            if (!isOptionalType && (paramAnnotation == null || paramAnnotation.required())) {
                requiredNames.add("\"" + paramName + "\"");
            }
        }

        String properties = "Map.ofEntries(" + String.join(", ", propertyEntries) + ")";
        String required = "List.of(" + String.join(", ", requiredNames) + ")";

        return "Map.of(\"type\", \"object\", \"properties\", " + properties + ", \"required\", " + required + ")";
    }

    private List<? extends VariableElement> getSchemaParameters(List<? extends VariableElement> parameters) {
        List<VariableElement> filtered = new ArrayList<>();
        for (VariableElement param : parameters) {
            if (!isToolInvocationType(param.asType())) {
                filtered.add(param);
            }
        }
        return filtered;
    }

    private boolean isToolInvocationType(TypeMirror type) {
        return TOOL_INVOCATION_TYPE.equals(processingEnv.getTypeUtils().erasure(type).toString());
    }

    private String buildPropertySchema(String typeSchema, CopilotToolParam paramAnnotation, TypeMirror paramType) {
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
        String defaultArg = hasDefault ? generateDefaultLiteral(paramType, defaultValue) : "null";

        return "withMeta(" + typeSchema + ", " + descArg + ", " + defaultArg + ")";
    }

    private String generateLambdaBody(ExecutableElement method) {
        List<? extends VariableElement> params = method.getParameters();
        List<? extends VariableElement> schemaParameters = getSchemaParameters(params);
        StringBuilder sb = new StringBuilder();

        // Generate argument extraction
        if (!schemaParameters.isEmpty()) {
            // Check if single-record-parameter shortcut applies
            if (schemaParameters.size() == 1 && isRecord(schemaParameters.get(0).asType())) {
                String typeName = getTypeString(schemaParameters.get(0).asType());
                String paramName = schemaParameters.get(0).getSimpleName().toString();
                sb.append("                    ").append(typeName).append(" ").append(paramName)
                        .append(" = mapper.convertValue(invocation.getArguments(), ").append(typeName)
                        .append(".class);\n");
            } else {
                sb.append("Map<String, Object> args = invocation.getArguments();\n");
                for (VariableElement param : schemaParameters) {
                    String paramName = getParamName(param);
                    String varName = param.getSimpleName().toString();
                    TypeMirror paramType = param.asType();

                    // Handle default values
                    CopilotToolParam paramAnnotation = param.getAnnotation(CopilotToolParam.class);
                    boolean hasDefault = paramAnnotation != null && !paramAnnotation.defaultValue().isEmpty();

                    if (hasDefault) {
                        String defaultValue = paramAnnotation.defaultValue();
                        sb.append("                    Object ").append(varName).append("Raw = args.containsKey(\"")
                                .append(paramName).append("\") ? args.get(\"").append(paramName).append("\") : ")
                                .append(generateDefaultLiteral(paramType, defaultValue)).append(";\n");
                        sb.append("                    ").append(getTypeString(paramType)).append(" ").append(varName)
                                .append(" = ").append(generateArgExtraction(varName + "Raw", paramType)).append(";\n");
                    } else if (isOptionalType(paramType)) {
                        generateOptionalExtraction(sb, paramName, varName, paramType);
                    } else {
                        sb.append("                    ").append(getTypeString(paramType)).append(" ").append(varName)
                                .append(" = ").append(generateArgExtractionFromMap(paramName, paramType)).append(";\n");
                    }
                }
            }
        }

        // Generate method invocation based on return type
        TypeMirror returnType = method.getReturnType();
        String callTarget = method.getModifiers().contains(Modifier.STATIC)
                ? ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString()
                : "instance";
        String methodCall = callTarget + "." + method.getSimpleName() + "(" + generateArgList(params) + ")";

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
            if (isToolInvocationType(params.get(i).asType())) {
                sb.append("invocation");
            } else {
                sb.append(params.get(i).getSimpleName().toString());
            }
        }
        return sb.toString();
    }

    private String generateArgExtractionFromMap(String paramName, TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return generatePrimitiveExtraction("args.get(\"" + paramName + "\")", type);
        }
        if (type.getKind() == TypeKind.ARRAY) {
            return generateGenericTypeReferenceConversion("args.get(\"" + paramName + "\")", type);
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
            if (hasTypeArguments(type)) {
                return generateGenericTypeReferenceConversion("args.get(\"" + paramName + "\")", type);
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
        if (type.getKind() == TypeKind.ARRAY) {
            return generateGenericTypeReferenceConversion(varExpr, type);
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
            if (hasTypeArguments(type)) {
                return generateGenericTypeReferenceConversion(varExpr, type);
            }
            return "mapper.convertValue(" + varExpr + ", " + qualifiedName + ".class)";
        }
        return "(Object) " + varExpr;
    }

    private boolean hasTypeArguments(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED && !((DeclaredType) type).getTypeArguments().isEmpty();
    }

    private String generateGenericTypeReferenceConversion(String expr, TypeMirror type) {
        return "mapper.convertValue(" + expr + ", new com.fasterxml.jackson.core.type.TypeReference<" + type
                + ">() {})";
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

    private boolean isOptionalType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        String name = typeElement.getQualifiedName().toString();
        return "java.util.Optional".equals(name) || "java.util.OptionalInt".equals(name)
                || "java.util.OptionalLong".equals(name) || "java.util.OptionalDouble".equals(name);
    }

    private void generateOptionalExtraction(StringBuilder sb, String paramName, String varName, TypeMirror paramType) {
        TypeElement typeElement = (TypeElement) ((DeclaredType) paramType).asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();

        sb.append("                    Object ").append(varName).append("Raw = args.get(\"").append(paramName)
                .append("\");\n");

        switch (qualifiedName) {
            case "java.util.OptionalInt" :
                sb.append("                    java.util.OptionalInt ").append(varName).append(" = ").append(varName)
                        .append("Raw != null ? java.util.OptionalInt.of(((Number) ").append(varName)
                        .append("Raw).intValue()) : java.util.OptionalInt.empty();\n");
                break;
            case "java.util.OptionalLong" :
                sb.append("                    java.util.OptionalLong ").append(varName).append(" = ").append(varName)
                        .append("Raw != null ? java.util.OptionalLong.of(((Number) ").append(varName)
                        .append("Raw).longValue()) : java.util.OptionalLong.empty();\n");
                break;
            case "java.util.OptionalDouble" :
                sb.append("                    java.util.OptionalDouble ").append(varName).append(" = ").append(varName)
                        .append("Raw != null ? java.util.OptionalDouble.of(((Number) ").append(varName)
                        .append("Raw).doubleValue()) : java.util.OptionalDouble.empty();\n");
                break;
            default :
                // java.util.Optional<T> — unwrap the type argument
                List<? extends TypeMirror> typeArgs = ((DeclaredType) paramType).getTypeArguments();
                if (!typeArgs.isEmpty()) {
                    TypeMirror innerType = typeArgs.get(0);
                    String innerExtraction = generateArgExtraction(varName + "Raw", innerType);
                    sb.append("                    java.util.Optional ").append(varName).append(" = ").append(varName)
                            .append("Raw != null ? java.util.Optional.of(").append(innerExtraction)
                            .append(") : java.util.Optional.empty();\n");
                } else {
                    sb.append("                    java.util.Optional ").append(varName).append(" = ").append(varName)
                            .append("Raw != null ? java.util.Optional.of(").append(varName)
                            .append("Raw) : java.util.Optional.empty();\n");
                }
                break;
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

    private String validateDefaultValueCompatibility(TypeMirror type, String defaultValue) {
        if (type.getKind().isPrimitive()) {
            return validatePrimitiveDefault(type.getKind(), defaultValue);
        }
        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
            String qualifiedName = typeElement.getQualifiedName().toString();
            if ("java.lang.String".equals(qualifiedName)) {
                return null;
            }
            if ("java.lang.Boolean".equals(qualifiedName)) {
                return validateBooleanDefault(defaultValue);
            }
            if ("java.lang.Character".equals(qualifiedName)) {
                return validateCharacterDefault(defaultValue);
            }
            if (isBoxedNumeric(qualifiedName)) {
                return validatePrimitiveDefault(boxedTypeKind(qualifiedName), defaultValue);
            }
        }
        return null;
    }

    private String validatePrimitiveDefault(TypeKind kind, String defaultValue) {
        try {
            switch (kind) {
                case INT :
                    Integer.parseInt(defaultValue);
                    return null;
                case LONG :
                    Long.parseLong(defaultValue);
                    return null;
                case SHORT :
                    Short.parseShort(defaultValue);
                    return null;
                case BYTE :
                    Byte.parseByte(defaultValue);
                    return null;
                case DOUBLE :
                    Double.parseDouble(defaultValue);
                    return null;
                case FLOAT :
                    Float.parseFloat(defaultValue);
                    return null;
                case BOOLEAN :
                    return validateBooleanDefault(defaultValue);
                case CHAR :
                    return validateCharacterDefault(defaultValue);
                default :
                    return null;
            }
        } catch (NumberFormatException ex) {
            return "@CopilotToolParam defaultValue '" + defaultValue + "' is not valid for " + kind.name().toLowerCase()
                    + " parameters";
        }
    }

    private String validateBooleanDefault(String defaultValue) {
        if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
            return null;
        }
        return "@CopilotToolParam defaultValue '" + defaultValue + "' is not valid for boolean parameters";
    }

    private String validateCharacterDefault(String defaultValue) {
        return defaultValue != null && defaultValue.length() == 1
                ? null
                : "@CopilotToolParam defaultValue '" + defaultValue + "' is not valid for char parameters";
    }

    private TypeKind boxedTypeKind(String qualifiedName) {
        switch (qualifiedName) {
            case "java.lang.Integer" :
                return TypeKind.INT;
            case "java.lang.Long" :
                return TypeKind.LONG;
            case "java.lang.Double" :
                return TypeKind.DOUBLE;
            case "java.lang.Float" :
                return TypeKind.FLOAT;
            case "java.lang.Short" :
                return TypeKind.SHORT;
            case "java.lang.Byte" :
                return TypeKind.BYTE;
            default :
                return TypeKind.NONE;
        }
    }

    private String getParamName(VariableElement param) {
        CopilotToolParam paramAnnotation = param.getAnnotation(CopilotToolParam.class);
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

    private boolean isRecord(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getKind() == ElementKind.RECORD;
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

    // ------------------------------------------------------------------
    // JSON-to-Java source code conversion
    // ------------------------------------------------------------------

    /**
     * Converts a JSON object string to a Java source expression. Supports nested
     * objects, arrays, strings, numbers, booleans, and null.
     */
    static String jsonToMapOfSource(String json) {
        JsonToSourceConverter converter = new JsonToSourceConverter(json);
        String result = converter.parseObject();
        converter.skipWhitespace();
        if (converter.pos < json.length()) {
            throw new IllegalArgumentException("Unexpected trailing content at position " + converter.pos + ": '"
                    + json.substring(converter.pos) + "'");
        }
        return result;
    }

    /**
     * Minimal recursive-descent JSON parser that produces helper calls and literal
     * Java source expressions from a JSON string. Only used at compile time by the
     * annotation processor.
     */
    private static final class JsonToSourceConverter {

        private final String input;
        private int pos;

        JsonToSourceConverter(String input) {
            this.input = input;
            this.pos = 0;
        }

        String parseObject() {
            skipWhitespace();
            expect('{');
            skipWhitespace();
            List<String> entries = new ArrayList<>();
            if (peek() != '}') {
                do {
                    skipWhitespace();
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    skipWhitespace();
                    String value = parseValue();
                    entries.add("\"" + escapeJava(key) + "\", " + value);
                    skipWhitespace();
                } while (tryConsume(','));
            }
            expect('}');
            return "mapOfNullable(" + String.join(", ", entries) + ")";
        }

        private String parseArray() {
            expect('[');
            skipWhitespace();
            List<String> items = new ArrayList<>();
            if (peek() != ']') {
                do {
                    skipWhitespace();
                    items.add(parseValue());
                    skipWhitespace();
                } while (tryConsume(','));
            }
            expect(']');
            return "listOfNullable(" + String.join(", ", items) + ")";
        }

        private String parseValue() {
            skipWhitespace();
            char c = peek();
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return "\"" + escapeJava(parseString()) + "\"";
            }
            if (c == 't' || c == 'f') {
                return parseBoolean();
            }
            if (c == 'n') {
                return parseNull();
            }
            return parseNumber();
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < input.length() && input.charAt(pos) != '"') {
                char current = input.charAt(pos++);
                if (current == '\\') {
                    sb.append(parseEscape());
                } else {
                    if (current < 0x20) {
                        throw new IllegalArgumentException("Unescaped control character at position " + (pos - 1));
                    }
                    sb.append(current);
                }
            }
            expect('"');
            return sb.toString();
        }

        private char parseEscape() {
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unterminated string escape at position " + pos);
            }
            char escaped = input.charAt(pos++);
            return switch (escaped) {
                case '"', '\\', '/' -> escaped;
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> parseUnicodeEscape();
                default -> throw new IllegalArgumentException(
                        "Invalid escape sequence \\" + escaped + " at position " + (pos - 2));
            };
        }

        private char parseUnicodeEscape() {
            if (pos + 4 > input.length()) {
                throw new IllegalArgumentException("Incomplete Unicode escape at position " + (pos - 2));
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                char hex = input.charAt(pos++);
                if (!isAsciiHexDigit(hex)) {
                    throw new IllegalArgumentException("Invalid Unicode escape at position " + (pos - 1));
                }
                int digit = Character.digit(hex, 16);
                value = (value << 4) | digit;
            }
            return (char) value;
        }

        private boolean isAsciiHexDigit(char c) {
            return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
        }

        private String parseBoolean() {
            if (input.startsWith("true", pos)) {
                pos += 4;
                return "true";
            }
            if (input.startsWith("false", pos)) {
                pos += 5;
                return "false";
            }
            throw new IllegalArgumentException("Expected boolean at position " + pos);
        }

        private String parseNull() {
            if (input.startsWith("null", pos)) {
                pos += 4;
                return "(Object) null";
            }
            throw new IllegalArgumentException("Expected null at position " + pos);
        }

        private String parseNumber() {
            int start = pos;
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
            }
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Expected number at position " + start);
            }
            if (input.charAt(pos) == '0') {
                pos++;
            } else if (isDigitOneToNine(input.charAt(pos))) {
                consumeDigits();
            } else {
                throw new IllegalArgumentException("Expected number at position " + pos);
            }
            if (pos < input.length() && input.charAt(pos) == '.') {
                pos++;
                requireDigit("fraction");
                consumeDigits();
            }
            if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                    pos++;
                }
                requireDigit("exponent");
                consumeDigits();
            }
            String number = input.substring(start, pos);
            try {
                new java.math.BigDecimal(number);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Number cannot be represented at position " + start + ": " + number,
                        e);
            }
            return "new java.math.BigDecimal(\"" + number + "\")";
        }

        private void requireDigit(String part) {
            if (pos >= input.length() || !isAsciiDigit(input.charAt(pos))) {
                throw new IllegalArgumentException("Expected digit in number " + part + " at position " + pos);
            }
        }

        private void consumeDigits() {
            while (pos < input.length() && isAsciiDigit(input.charAt(pos))) {
                pos++;
            }
        }

        private boolean isAsciiDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private boolean isDigitOneToNine(char c) {
            return c >= '1' && c <= '9';
        }

        private void skipWhitespace() {
            while (pos < input.length() && isJsonWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private boolean isJsonWhitespace(char c) {
            return c == ' ' || c == '\t' || c == '\r' || c == '\n';
        }

        private char peek() {
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            return input.charAt(pos);
        }

        private void expect(char c) {
            if (pos >= input.length() || input.charAt(pos) != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at position " + pos + " but got '"
                        + (pos < input.length() ? input.charAt(pos) : "EOF") + "'");
            }
            pos++;
        }

        private boolean tryConsume(char c) {
            if (pos < input.length() && input.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }
    }

    private static String escapeJava(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char current = s.charAt(i);
            switch (current) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (Character.isISOControl(current)) {
                        escaped.append(String.format("\\%03o", (int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
