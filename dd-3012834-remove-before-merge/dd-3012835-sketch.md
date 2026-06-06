Subject: Using Java annotation processor to make it so one must pass `-Aexperimental=true` when compiling, otherwise usages of classes annotated with `@CopilotExperimental` will fail to compile.


This looks doable:  
  
Yes, via a \*\*pluggable annotation processor\*\* (JSR 269). The mechanism:  
  
1. Define `@CopilotExperimental` as before  
2. Write an annotation processor that scans the AST for \*usages\* of types annotated `@CopilotExperimental`  
3. The processor calls `Messager.printMessage(Kind.ERROR, ...)` unless a javac `-A` option is present  
4. Compile-time error, no runtime cost  
  
Sketch:  
  
```java  
@SupportedAnnotationTypes("\*")  
@SupportedOptions("allowExperimental")  
public class ExperimentalCheckProcessor extends AbstractProcessor {  
  
    @Override  
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {  
        if (processingEnv.getOptions().containsKey("allowExperimental")) {  
            return false; // flag passed, allow everything  
        }  
  
        // Scan all elements for references to @CopilotExperimental types  
        for (Element element : roundEnv.getRootElements()) {  
            checkElement(element);  
        }  
        return false;  
    }  
  
    private void checkElement(Element element) {  
        // Check supertypes, field types, method param/return types, etc.  
        // If any referenced type has @CopilotExperimental, emit an error:  
        processingEnv.getMessager().printMessage(  
            Diagnostic.Kind.ERROR,  
            "Use of @CopilotExperimental type requires -AallowExperimental",  
            element  
        );  
    }  
}  
```  
  
Then at compile time:  
  
```bash  
# Fails if @CopilotExperimental types are used  
javac -processor com.example.ExperimentalCheckProcessor MyCode.java  
  
# Passes  
javac -processor com.example.ExperimentalCheckProcessor -AallowExperimental MyCode.java  
```  
  
