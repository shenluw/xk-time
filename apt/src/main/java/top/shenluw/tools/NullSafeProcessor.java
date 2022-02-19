package top.shenluw.tools;


import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author shenluw
 * @date 2022/2/19 15:12
 */
@SupportedAnnotationTypes(value = {"top.shenluw.tools.NullSafe"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class NullSafeProcessor extends AbstractProcessor {
    private static final String PREFIX = "NullSafe";

    private Elements elements;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elements = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        TypeElement nullSafe = annotations.iterator().next();

        messager.printMessage(Diagnostic.Kind.NOTE, annotations
                .stream().map(TypeElement::getSimpleName).collect(Collectors.joining(","))

        );

        for (Element element : roundEnv.getElementsAnnotatedWith(nullSafe)) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }
            generateNullSafeClass(element);
        }


        return true;
    }

    private void generateNullSafeClass(Element element) {
        Name packageName = elements.getPackageOf(element).getQualifiedName();
        String origin = packageName + "." + element.getSimpleName();


        TypeElement originType = elements.getTypeElement(origin);
        List<? extends Element> allMembers = elements.getAllMembers(originType);
        if (allMembers.isEmpty()) {
            return;
        }

        String newClass = PREFIX + element.getSimpleName();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(newClass)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        classBuilder.addJavadoc(elements.getDocComment(element));
        classBuilder.addJavadoc("@author $N", "shenluw");

        for (Element member : allMembers) {
            if (member.getKind() != ElementKind.METHOD) {
                continue;
            }

            MethodSpec methodSpec = generateNullSafeMethod(origin, ((ExecutableElement) member));
            if (methodSpec != null) {
                classBuilder.addMethod(methodSpec);
            }
        }
        writeTo(packageName.toString(), classBuilder.build());
    }

    private MethodSpec generateNullSafeMethod(String originClass, ExecutableElement element) {
        Set<Modifier> modifiers = element.getModifiers();
        if (!modifiers.contains(Modifier.STATIC) || !modifiers.contains(Modifier.PUBLIC)) {
            return null;

        }
        TypeMirror returnType = element.getReturnType();
        TypeKind kind = returnType.getKind();
        if (kind.isPrimitive() || kind == TypeKind.VOID) {
            return null;
        }

        List<? extends VariableElement> parameters = element.getParameters();
        if (parameters.isEmpty()) {
            return null;
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder(element.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.get(returnType));
        List<? extends TypeMirror> thrownTypes = element.getThrownTypes();
        if (!thrownTypes.isEmpty()) {
            for (TypeMirror thrownType : thrownTypes) {
                builder.addException(TypeName.get(thrownType));
            }
        }

        for (VariableElement parameter : parameters) {
            builder.addParameter(ParameterSpec.get(parameter));
        }
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        if (!annotationMirrors.isEmpty()) {
            for (AnnotationMirror annotationMirror : annotationMirrors) {
                builder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }
        }
        builder.addJavadoc(elements.getDocComment(element));
        builder.addCode(generateExecuteBody(originClass, element));
        return builder.build();
    }

    private String generateExecuteBody(String originClass, ExecutableElement element) {
        List<? extends VariableElement> parameters = element.getParameters();
        StringBuilder body = new StringBuilder();

        StringBuilder execute = new StringBuilder(String.format("return %s.%s(", originClass, element.getSimpleName()));
        for (VariableElement parameter : parameters) {
            TypeKind kind = parameter.asType().getKind();
            if (!kind.isPrimitive()) {
                body.append(String.format("if (%s == null) { return null; }\n", parameter.getSimpleName()));
            }
            execute.append(parameter.getSimpleName()).append(',');
        }
        execute.deleteCharAt(execute.length() - 1)
                .append(");\n");

        body.append(execute);
        return body.toString();
    }


    private void writeTo(String packageName, TypeSpec typeSpec) {
        try {
            JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "文件创建失败 " + typeSpec.name);
        }
    }
}
