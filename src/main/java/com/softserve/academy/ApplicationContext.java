package com.softserve.academy;

import com.softserve.academy.annotations.Component;
import com.softserve.academy.annotations.Inject;
import com.softserve.academy.annotations.Value;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Field;
import java.util.*;

public class ApplicationContext {

    private Map<String, Object> container;

    public ApplicationContext(String packageForScan) throws IllegalAccessException, InstantiationException {
        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner()).setUrls(ClasspathHelper
                        .forClassLoader(classLoadersList.toArray(new ClassLoader[0]))).filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageForScan))));

        container = new HashMap<>();

        Set<Class<?>> allTypesAnnotatedWithComponent = reflections.getTypesAnnotatedWith(Component.class);

        if (!allTypesAnnotatedWithComponent.isEmpty()) {
            for (Class<?> component : allTypesAnnotatedWithComponent) {
                Object object = component.newInstance();



                container.put(component.getName(), object);
            }
            for (Object object : container.values()) {
                setValues(object);
                injectValues(object);
            }
        }
    }

    public Object getComponent(Class<?> component) {
        if (container.containsKey(component.getName())) {
            return container.get(component.getName());
        }
        throw new RuntimeException("Component with name " + component.getName() + " not allowed!");
    }

    private void setValues(Object object) throws IllegalAccessException {
        Field fields[] = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Value.class)) {
                Value fieldValue = field.getAnnotation(Value.class);

                String values[] = fieldValue.value();

                if (field.getType().isArray()) {
                    field.set(object, values);
                } else {
                    field.set(object, values[0]);
                }
            }
        }
    }

    private void injectValues(Object object) throws IllegalAccessException {
        Field fields[] = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Inject.class)) {

                if (container.containsKey(field.getType().getName())) {
                    field.set(object, container.get(field.getType().getName()));
                }
            }
        }
    }
}