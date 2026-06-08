/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Annotation processor that enforces compile-time gating of experimental APIs.
 *
 * <p>
 * Any declaration-level reference to a type or method annotated with
 * {@link CopilotExperimental} in consumer source code causes a compilation
 * error unless the compiler option {@code -Acopilot.experimental.allowed=true}
 * is provided.
 *
 * <p>
 * This processor uses only standard JSR 269 APIs ({@code javax.lang.model.*})
 * and works with any Java compiler (javac, ECJ, etc.). It checks declarations
 * (field types, method parameters, return types, supertypes, thrown types) but
 * does not inspect method body expressions.
 *
 * @since 1.0.0
 */
@SupportedAnnotationTypes("*")
@SupportedOptions("copilot.experimental.allowed")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CopilotExperimentalProcessor extends AbstractProcessor {

    private boolean allowed;
    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        String value = processingEnv.getOptions().get("copilot.experimental.allowed");
        this.allowed = "true".equals(value);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (allowed) {
            return false;
        }
        for (Element rootElement : roundEnv.getRootElements()) {
            checkElement(rootElement);
        }
        return false;
    }

    private void checkElement(Element element) {
        // Skip elements that are themselves annotated @CopilotExperimental
        // (they are the definitions, not consumers)
        if (isExperimental(element)) {
            return;
        }

        switch (element.getKind()) {
            case CLASS, INTERFACE, ENUM, RECORD -> checkTypeElement((TypeElement) element);
            case METHOD, CONSTRUCTOR -> checkExecutable((ExecutableElement) element);
            case FIELD, ENUM_CONSTANT -> checkField((VariableElement) element);
            default -> {
            }
        }

        // Recurse into enclosed elements
        for (Element enclosed : element.getEnclosedElements()) {
            checkElement(enclosed);
        }
    }

    private void checkTypeElement(TypeElement typeElement) {
        // Check superclass
        TypeMirror superclass = typeElement.getSuperclass();
        checkTypeMirror(superclass, typeElement, "extends");

        // Check implemented interfaces
        for (TypeMirror iface : typeElement.getInterfaces()) {
            checkTypeMirror(iface, typeElement, "implements");
        }
    }

    private void checkExecutable(ExecutableElement method) {
        // Check return type
        checkTypeMirror(method.getReturnType(), method, "return type");

        // Check parameter types
        for (VariableElement param : method.getParameters()) {
            checkTypeMirror(param.asType(), method, "parameter '" + param.getSimpleName() + "'");
        }

        // Check thrown types
        for (TypeMirror thrown : method.getThrownTypes()) {
            checkTypeMirror(thrown, method, "throws");
        }
    }

    private void checkField(VariableElement field) {
        checkTypeMirror(field.asType(), field, "field type");
    }

    private void checkTypeMirror(TypeMirror typeMirror, Element usageSite, String context) {
        if (typeMirror == null) {
            return;
        }
        if (typeMirror instanceof DeclaredType declaredType) {
            Element typeElement = declaredType.asElement();
            if (isExperimental(typeElement)) {
                reportError(typeElement, usageSite, context);
            }
            // Check type arguments (generics)
            for (TypeMirror typeArg : declaredType.getTypeArguments()) {
                checkTypeMirror(typeArg, usageSite, context);
            }
        }
    }

    private boolean isExperimental(Element element) {
        if (element == null) {
            return false;
        }
        if (element.getAnnotation(CopilotExperimental.class) != null) {
            return true;
        }
        // If the enclosing type is experimental, members are implicitly experimental
        Element enclosing = element.getEnclosingElement();
        return enclosing != null && enclosing.getAnnotation(CopilotExperimental.class) != null;
    }

    private void reportError(Element experimentalElement, Element usageSite, String context) {
        Messager messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.ERROR,
                "Use of experimental API '" + experimentalElement.getSimpleName() + "' in " + context
                        + " is not allowed. Add compiler option -Acopilot.experimental.allowed=true to opt in.",
                usageSite);
    }
}
