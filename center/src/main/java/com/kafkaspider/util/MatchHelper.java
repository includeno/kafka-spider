package com.kafkaspider.util;

import com.kafkaspider.service.MatchService;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


//辅助匹配url的工具
@Slf4j
public class MatchHelper {

    public final static List<Class> impls = getSubClassesFromInterface(MatchService.class);

    public static List<Class> getSubClassesFromInterface(Class<?> target) {
        return getSubclassesFromJar(target);
    }

    public static List<Class> getSubclassesFromJar(Class c) {

        Reflections reflections = new Reflections(
                ClasspathHelper.forPackage("com.kafkaspider.service"), Scanners.values());
        Set<Class> implementingTypes =
                reflections.getSubTypesOf(c);
        return implementingTypes.stream().collect(Collectors.toList());
    }
}
