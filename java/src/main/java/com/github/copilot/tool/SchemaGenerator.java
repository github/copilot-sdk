/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Compile-time utility that maps {@code javax.lang.model} types to JSON Schema
 * represented as Java source code literals ({@code Map.of(...)} expressions).
 *
 * <p>
 * This class is invoked by the annotation processor and operates exclusively
 * with the {@code javax.lang.model} API. It does NOT use
 * {@code java.lang.reflect}.
 *
 * @since 1.0.2
 */
public class SchemaGenerator {

    /**
     * Given a {@link TypeMirror} from the annotation processing environment,
     * returns a {@code String} containing Java source code for a {@code Map}
     * literal representing the JSON Schema of that type.
     *
     * @param type
     *            the type to generate schema for
     * @param typeUtils
     *            the {@link Types} utility from the processing environment
     * @param elementUtils
     *            the {@link Elements} utility from the processing environment
     * @return a Java source code string representing the JSON Schema
     */
    public String generateSchemaSource(TypeMirror type, Types typeUtils, Elements elementUtils) {
        return generateSchema(type, typeUtils, elementUtils);
    }

    /**
     * Generates the full "parameters" schema source for a method's parameters.
     * Produces a
     * {@code Map.of("type", "object", "properties", Map.of(...), "required", List.of(...))}.
     *
     * @param parameters
     *            the method parameters to generate schema for
     * @param typeUtils
     *            the {@link Types} utility from the processing environment
     * @param elementUtils
     *            the {@link Elements} utility from the processing environment
     * @return a Java source code string representing the parameters JSON Schema
     */
    public String generateParametersSchemaSource(List<? extends VariableElement> parameters, Types typeUtils,
            Elements elementUtils) {
        if (parameters.isEmpty()) {
            return "Map.of(\"type\", \"object\", \"properties\", Map.of(), \"required\", List.of())";
        }

        List<String> propertyEntries = new ArrayList<>();
        List<String> requiredNames = new ArrayList<>();

        for (VariableElement param : parameters) {
            String paramName = param.getSimpleName().toString();
            TypeMirror paramType = param.asType();

            boolean isOptional = isOptionalType(paramType, typeUtils, elementUtils);
            String schema;
            if (isOptional) {
                schema = generateSchema(unwrapOptional(paramType, typeUtils, elementUtils), typeUtils, elementUtils);
            } else {
                schema = generateSchema(paramType, typeUtils, elementUtils);
            }

            propertyEntries.add("\"" + paramName + "\", " + schema);

            if (!isOptional) {
                Param paramAnnotation = param.getAnnotation(Param.class);
                if (paramAnnotation == null || paramAnnotation.required()) {
                    requiredNames.add("\"" + paramName + "\"");
                }
            }
        }

        String properties = "Map.of(" + String.join(", ", propertyEntries) + ")";
        String required = "List.of(" + String.join(", ", requiredNames) + ")";

        return "Map.of(\"type\", \"object\", \"properties\", " + properties + ", \"required\", " + required + ")";
    }

    private String generateSchema(TypeMirror type, Types typeUtils, Elements elementUtils) {
        // Handle primitive types
        if (type.getKind().isPrimitive()) {
            return generatePrimitiveSchema(type.getKind());
        }

        // Handle array types
        if (type.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) type;
            TypeMirror componentType = arrayType.getComponentType();
            String itemsSchema = generateSchema(componentType, typeUtils, elementUtils);
            return "Map.of(\"type\", \"array\", \"items\", " + itemsSchema + ")";
        }

        // Handle declared types (classes, interfaces, enums, records)
        if (type.getKind() == TypeKind.DECLARED) {
            return generateDeclaredTypeSchema((DeclaredType) type, typeUtils, elementUtils);
        }

        // Fallback: any
        return "Map.of()";
    }

    private String generatePrimitiveSchema(TypeKind kind) {
        switch (kind) {
            case INT :
            case LONG :
            case BYTE :
            case SHORT :
                return "Map.of(\"type\", \"integer\")";
            case DOUBLE :
            case FLOAT :
                return "Map.of(\"type\", \"number\")";
            case BOOLEAN :
                return "Map.of(\"type\", \"boolean\")";
            case CHAR :
                return "Map.of(\"type\", \"string\")";
            default :
                return "Map.of()";
        }
    }

    private String generateDeclaredTypeSchema(DeclaredType type, Types typeUtils, Elements elementUtils) {
        TypeElement typeElement = (TypeElement) type.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();

        // String
        if ("java.lang.String".equals(qualifiedName)) {
            return "Map.of(\"type\", \"string\")";
        }

        // Boxed primitives
        if ("java.lang.Integer".equals(qualifiedName) || "java.lang.Long".equals(qualifiedName)) {
            return "Map.of(\"type\", \"integer\")";
        }
        if ("java.lang.Double".equals(qualifiedName) || "java.lang.Float".equals(qualifiedName)) {
            return "Map.of(\"type\", \"number\")";
        }
        if ("java.lang.Boolean".equals(qualifiedName)) {
            return "Map.of(\"type\", \"boolean\")";
        }

        // UUID
        if ("java.util.UUID".equals(qualifiedName)) {
            return "Map.of(\"type\", \"string\", \"format\", \"uuid\")";
        }

        // OffsetDateTime
        if ("java.time.OffsetDateTime".equals(qualifiedName)) {
            return "Map.of(\"type\", \"string\", \"format\", \"date-time\")";
        }

        // JsonNode (any)
        if ("com.fasterxml.jackson.databind.JsonNode".equals(qualifiedName)) {
            return "Map.of()";
        }

        // Object (any)
        if ("java.lang.Object".equals(qualifiedName)) {
            return "Map.of()";
        }

        // Optional types
        if ("java.util.Optional".equals(qualifiedName)) {
            List<? extends TypeMirror> typeArgs = type.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                return generateSchema(typeArgs.get(0), typeUtils, elementUtils);
            }
            return "Map.of()";
        }
        if ("java.util.OptionalInt".equals(qualifiedName)) {
            return "Map.of(\"type\", \"integer\")";
        }
        if ("java.util.OptionalDouble".equals(qualifiedName)) {
            return "Map.of(\"type\", \"number\")";
        }

        // List / Collection
        if (isCollectionType(qualifiedName)) {
            List<? extends TypeMirror> typeArgs = type.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                String itemsSchema = generateSchema(typeArgs.get(0), typeUtils, elementUtils);
                return "Map.of(\"type\", \"array\", \"items\", " + itemsSchema + ")";
            }
            return "Map.of(\"type\", \"array\")";
        }

        // Map<String, V>
        if (isMapType(qualifiedName)) {
            List<? extends TypeMirror> typeArgs = type.getTypeArguments();
            if (typeArgs.size() == 2) {
                TypeMirror valueType = typeArgs.get(1);
                if (valueType.getKind() == TypeKind.DECLARED) {
                    TypeElement valueElement = (TypeElement) ((DeclaredType) valueType).asElement();
                    String valueQName = valueElement.getQualifiedName().toString();
                    if ("java.lang.Object".equals(valueQName)) {
                        return "Map.of(\"type\", \"object\")";
                    }
                }
                String valueSchema = generateSchema(valueType, typeUtils, elementUtils);
                return "Map.of(\"type\", \"object\", \"additionalProperties\", " + valueSchema + ")";
            }
            return "Map.of(\"type\", \"object\")";
        }

        // Enum types
        if (typeElement.getKind() == ElementKind.ENUM) {
            List<String> constants = typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
                    .map(e -> "\"" + e.getSimpleName().toString() + "\"").collect(Collectors.toList());
            return "Map.of(\"type\", \"string\", \"enum\", List.of(" + String.join(", ", constants) + "))";
        }

        // Record types
        if (typeElement.getKind() == ElementKind.RECORD) {
            return generateRecordSchema(typeElement, typeUtils, elementUtils);
        }

        // POJO / class types — treat as object with fields
        if (typeElement.getKind() == ElementKind.CLASS) {
            return generateClassSchema(typeElement, typeUtils, elementUtils);
        }

        // Sealed interfaces with @JsonSubTypes — oneOf
        if (typeElement.getKind() == ElementKind.INTERFACE) {
            return generateSealedSchema(typeElement, typeUtils, elementUtils);
        }

        return "Map.of()";
    }

    private String generateRecordSchema(TypeElement typeElement, Types typeUtils, Elements elementUtils) {
        List<String> propertyEntries = new ArrayList<>();
        List<String> requiredNames = new ArrayList<>();

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.RECORD_COMPONENT) {
                RecordComponentElement component = (RecordComponentElement) enclosed;
                String name = component.getSimpleName().toString();
                TypeMirror componentType = component.asType();

                boolean isOptional = isOptionalType(componentType, typeUtils, elementUtils);
                String schema;
                if (isOptional) {
                    schema = generateSchema(unwrapOptional(componentType, typeUtils, elementUtils), typeUtils,
                            elementUtils);
                } else {
                    schema = generateSchema(componentType, typeUtils, elementUtils);
                    requiredNames.add("\"" + name + "\"");
                }

                propertyEntries.add("\"" + name + "\", " + schema);
            }
        }

        String properties = "Map.of(" + String.join(", ", propertyEntries) + ")";
        String required = "List.of(" + String.join(", ", requiredNames) + ")";

        return "Map.of(\"type\", \"object\", \"properties\", " + properties + ", \"required\", " + required + ")";
    }

    private String generateClassSchema(TypeElement typeElement, Types typeUtils, Elements elementUtils) {
        List<String> propertyEntries = new ArrayList<>();
        List<String> requiredNames = new ArrayList<>();

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                // Skip static fields
                if (field.getModifiers().contains(javax.lang.model.element.Modifier.STATIC)) {
                    continue;
                }
                String name = field.getSimpleName().toString();
                TypeMirror fieldType = field.asType();

                boolean isOptional = isOptionalType(fieldType, typeUtils, elementUtils);
                String schema;
                if (isOptional) {
                    schema = generateSchema(unwrapOptional(fieldType, typeUtils, elementUtils), typeUtils,
                            elementUtils);
                } else {
                    schema = generateSchema(fieldType, typeUtils, elementUtils);
                    requiredNames.add("\"" + name + "\"");
                }

                propertyEntries.add("\"" + name + "\", " + schema);
            }
        }

        if (propertyEntries.isEmpty()) {
            return "Map.of(\"type\", \"object\")";
        }

        String properties = "Map.of(" + String.join(", ", propertyEntries) + ")";
        String required = "List.of(" + String.join(", ", requiredNames) + ")";

        return "Map.of(\"type\", \"object\", \"properties\", " + properties + ", \"required\", " + required + ")";
    }

    private String generateSealedSchema(TypeElement typeElement, Types typeUtils, Elements elementUtils) {
        List<? extends TypeMirror> permittedSubclasses = typeElement.getPermittedSubclasses();
        if (permittedSubclasses != null && !permittedSubclasses.isEmpty()) {
            List<String> schemas = permittedSubclasses.stream().map(sub -> generateSchema(sub, typeUtils, elementUtils))
                    .collect(Collectors.toList());
            return "Map.of(\"oneOf\", List.of(" + String.join(", ", schemas) + "))";
        }
        return "Map.of(\"type\", \"object\")";
    }

    private boolean isOptionalType(TypeMirror type, Types typeUtils, Elements elementUtils) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) type;
        TypeElement element = (TypeElement) declaredType.asElement();
        String name = element.getQualifiedName().toString();
        return "java.util.Optional".equals(name) || "java.util.OptionalInt".equals(name)
                || "java.util.OptionalDouble".equals(name) || "java.util.OptionalLong".equals(name);
    }

    private TypeMirror unwrapOptional(TypeMirror type, Types typeUtils, Elements elementUtils) {
        if (type.getKind() != TypeKind.DECLARED) {
            return type;
        }
        DeclaredType declaredType = (DeclaredType) type;
        TypeElement element = (TypeElement) declaredType.asElement();
        String name = element.getQualifiedName().toString();

        if ("java.util.Optional".equals(name)) {
            List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                return typeArgs.get(0);
            }
        }
        if ("java.util.OptionalInt".equals(name)) {
            return typeUtils.getPrimitiveType(TypeKind.INT);
        }
        if ("java.util.OptionalDouble".equals(name)) {
            return typeUtils.getPrimitiveType(TypeKind.DOUBLE);
        }
        if ("java.util.OptionalLong".equals(name)) {
            return typeUtils.getPrimitiveType(TypeKind.LONG);
        }
        return type;
    }

    private boolean isCollectionType(String qualifiedName) {
        return "java.util.List".equals(qualifiedName) || "java.util.Collection".equals(qualifiedName)
                || "java.util.Set".equals(qualifiedName);
    }

    private boolean isMapType(String qualifiedName) {
        return "java.util.Map".equals(qualifiedName);
    }
}
