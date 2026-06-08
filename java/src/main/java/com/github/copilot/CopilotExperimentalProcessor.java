/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Annotation processor that enforces compile-time gating of experimental APIs.
 *
 * <p>Any reference to a type or method annotated with {@link CopilotExperimental}
 * in consumer source code causes a compilation error unless the compiler option
 * {@code -Acopilot.experimental.allowed=true} is provided.
 *
 * @since 1.0.0
 */
@SupportedAnnotationTypes("*")
@SupportedOptions("copilot.experimental.allowed")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CopilotExperimentalProcessor extends AbstractProcessor {

    private Trees trees;
    private boolean allowed;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        String value = processingEnv.getOptions().get("copilot.experimental.allowed");
        this.allowed = "true".equals(value);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (allowed) {
            return false;
        }
        for (Element rootElement : roundEnv.getRootElements()) {
            TreePath path = trees.getPath(rootElement);
            if (path == null) continue;
            new ExperimentalUsageScanner().scan(path, null);
        }
        return false;
    }

    private class ExperimentalUsageScanner extends TreePathScanner<Void, Void> {

        @Override
        public Void visitIdentifier(IdentifierTree node, Void unused) {
            checkElement(node);
            return super.visitIdentifier(node, unused);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, Void unused) {
            checkElement(node);
            return super.visitMemberSelect(node, unused);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree node, Void unused) {
            checkElement(node);
            return super.visitMemberReference(node, unused);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void unused) {
            checkElement(node);
            return super.visitNewClass(node, unused);
        }

        private void checkElement(Object node) {
            TreePath currentPath = getCurrentPath();
            if (currentPath == null) return;
            Element element = trees.getElement(currentPath);
            if (element == null) return;
            if (isExperimental(element)) {
                reportError(element, currentPath);
            }
        }

        private boolean isExperimental(Element element) {
            if (element.getAnnotation(CopilotExperimental.class) != null) {
                return true;
            }
            Element enclosing = element.getEnclosingElement();
            if (enclosing != null && enclosing.getAnnotation(CopilotExperimental.class) != null) {
                return true;
            }
            return false;
        }

        private void reportError(Element element, TreePath path) {
            Messager messager = processingEnv.getMessager();
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Use of experimental API '" + element.getSimpleName()
                    + "' is not allowed. Add compiler option"
                    + " -Acopilot.experimental.allowed=true to opt in.",
                trees.getElement(path)
            );
        }
    }
}
