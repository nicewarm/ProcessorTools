package com.lzh.processor.compiler;

import com.lzh.processor.annoapi.FieldType;
import com.lzh.processor.data.FieldData;
import com.lzh.processor.reflect.Reflect;
import com.lzh.processor.util.StringUtils;
import com.lzh.processor.util.UtilMgr;
import com.lzh.processor.util.javapoet.FieldSpec;
import com.lzh.processor.util.javapoet.JavaFile;
import com.lzh.processor.util.javapoet.MethodSpec;
import com.lzh.processor.util.javapoet.TypeName;
import com.lzh.processor.util.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * @author Administrator
 */
public abstract class FileFactory {
    final static String REQUEST_DATA_CLASS = "RequestData";
    final static String REQUEST_DATA_FIELD_NAME = "data";
    final static String PARENT_CLASS_FIELD_NAME = "parent";
    final static String TAG_FIELD = "TAG";
    final static String CREATE_METHOD = "create";

    String pkgName;
    ElementParser parser;
    TypeName generateClassName = null;
    TypeName generateParentClassName = null;

    public FileFactory(ElementParser parser) {
        this.parser = parser;
        pkgName = getPkgName();
        generateClassName = getTypeName(parser.getClzName() + getSuffix());
        if (parser.getParentElement() != null) {
            generateParentClassName = getTypeName(parser.getParentElement().getQualifiedName().toString() + getSuffix());
        }
    }

    abstract String getSuffix();

    abstract void generateCode() throws IOException;

    TypeName getTypeName(TypeElement element) {
        return TypeName.get(element.asType());
    }

    TypeName getTypeName (String clzName) {
        return Reflect.on(TypeName.class).create(clzName).get();
    }

    String getName (String clzSimpleName) {
        return StringUtils.isEmpty(pkgName)? clzSimpleName:pkgName + "." + clzSimpleName;
    }

    TypeName getTypeName(FieldType parent,TypeName child) {
        if (parent.equals(FieldType.array)) {
            return getTypeName(child.toString() + "[]");
        } else if (parent.equals(FieldType.list)) {
            return getTypeName(List.class.getCanonicalName() + "<" + child.toString() + ">");
        } else if (parent.equals(FieldType.set)) {
            return getTypeName(Set.class.getCanonicalName() + "<" + child.toString() + ">");
        } else {
            return child;
        }
    }

    public String getPkgName() {
        PackageElement pkgElement = UtilMgr.getMgr().getElementUtils().getPackageOf(parser.getElement());
        return pkgElement.isUnnamed() ? "":pkgElement.getQualifiedName().toString();
    }

    /**
     * create inner class RequestData,contains all of the field define by annotation @Field
     */
    TypeSpec generateRequestData() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(REQUEST_DATA_CLASS)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addJavadoc("inner class RequestData,contains all of the field define by annotation @Field")
                .addSuperinterface(TypeName.get(Serializable.class));
        List<FieldData> fieldList = parser.getFieldList();

        for (int i = 0; i < fieldList.size(); i++) {
            FieldData data = fieldList.get(i);
            builder.addField(createField(data));
            builder.addMethod(createSetRequestBuilder(data));
            builder.addMethod(createGetRequestBuilder(data));
        }

        return builder.build();
    }

    MethodSpec createGetRequestBuilder(FieldData data) {
        String getMethodName = StringUtils.getGetMethodName(data.getName());
        return MethodSpec.methodBuilder(getMethodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(getTypeName(data.getFieldType(), data.getType()))
                .addStatement("return this.$L", data.getName())
                .addJavadoc(data.getDoc())
                .build();
    }

    FieldSpec createField(FieldData data) {
        FieldSpec.Builder builder = FieldSpec.builder(getTypeName(data.getFieldType(), data.getType()), data.getName(), Modifier.PRIVATE)
                .addJavadoc(data.getDoc());
        if (!StringUtils.isEmpty(data.getDefValue())) {
            builder.initializer(
                    (data.getType().equals(TypeName.get(String.class)) ? "$S" : "$L"),
                    data.getDefValue());
        }

        return builder.build();
    }

    MethodSpec createSetRequestBuilder(FieldData data) {
        String setMethodName = StringUtils.getSetMethodName(data.getName());
        return MethodSpec.methodBuilder(setMethodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(getTypeName(data.getFieldType(), data.getType()), data.getName())
                .returns(getTypeName(REQUEST_DATA_CLASS))
                .addStatement("this.$L = $L", data.getName(), data.getName())
                .addStatement("return this")
                .addJavadoc(data.getDoc())
                .build();

    }

    void build(TypeSpec.Builder typeBuilder) throws IOException {
        JavaFile.Builder javaBuilder = JavaFile.builder(pkgName, typeBuilder.build());
        javaBuilder.addFileComment("The file is auto-generate by processorTool,do not modify!");
        javaBuilder.build().writeTo(UtilMgr.getMgr().getFiler());
    }

    /**
     * create private constructor method
     */
    MethodSpec createPrivateConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
        if (generateParentClassName != null) {
            builder.addStatement("$L = $L.$L()", PARENT_CLASS_FIELD_NAME, generateParentClassName, CREATE_METHOD);
        }

        return builder.build();
    }

    /**
     * generate static create method
     */
    MethodSpec createMethod() {
        return MethodSpec.methodBuilder(CREATE_METHOD)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("$T instance = new $T()", generateClassName, generateClassName)
                .addStatement("instance.$L = new $L()", REQUEST_DATA_FIELD_NAME, REQUEST_DATA_CLASS)
                .addStatement("return instance")
                .returns(generateClassName)
                .build();
    }

    void addParamsSetMethod(TypeSpec.Builder typeBuilder) {
        List<FieldData> fieldList = parser.getFieldList();
        for (int i = 0; i < fieldList.size(); i++) {
            FieldData data = fieldList.get(i);
            typeBuilder.addMethod(createSetMethod(data));
        }

        if (generateParentClassName == null) {
            return;
        }
        List<FieldData> parentFieldList = parser.getParentFieldList();
        for (int i = 0; i < parentFieldList.size(); i++) {
            FieldData data = parentFieldList.get(i);
            typeBuilder.addMethod(createParentSetMethod(data));
        }
    }

    MethodSpec createParentSetMethod(FieldData data) {
        String setMethodName = StringUtils.getSetMethodName(data.getName());
        return MethodSpec.methodBuilder(setMethodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(getTypeName(data.getFieldType(),data.getType()),data.getName())
                .returns(generateClassName)
                .addStatement("$L.$L($L)",PARENT_CLASS_FIELD_NAME,setMethodName,data.getName())
                .addStatement("return this")
                .addJavadoc(data.getDoc())
                .build();
    }

    MethodSpec createSetMethod(FieldData data) {
        String setMethodName = StringUtils.getSetMethodName(data.getName());
        return MethodSpec.methodBuilder(setMethodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(generateClassName)
                .addParameter(getTypeName(data.getFieldType(), data.getType()), data.getName())
                .addStatement("this.$L.$L($L)",REQUEST_DATA_FIELD_NAME,setMethodName,data.getName())
                .addStatement("return this")
                .addJavadoc(data.getDoc())
                .build();
    }
}
