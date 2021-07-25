package com.github.kealdish.idvalue.view.builder.context.impl;

import com.github.kealdish.idvalue.view.builder.context.BuildContext;
import com.github.kealdish.idvalue.view.builder.util.MergeUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class SimpleBuildContext implements BuildContext {

    private final ConcurrentMap<Object, Map<Object, Object>> dataMap;
    private final ConcurrentMap<Object, Map<Object, Object>> lazyDataMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Object, Function<BuildContext, Map<Object, Object>>> lazyBuilders = new ConcurrentHashMap<>();

    public SimpleBuildContext() {
        this(new ConcurrentHashMap<>());
    }

    // for test case
    public SimpleBuildContext(ConcurrentMap<Object, Map<Object, Object>> dataMap) {
        this.dataMap = dataMap;
    }

    @Override
    public <K, V> Map<K, V> getData(Object namespace) {
        Function<BuildContext, Map<Object, Object>> lazyBuilder = lazyBuilders.get(namespace);
        if (lazyBuilder != null) {
            return computeIfAbsent(lazyDataMap, namespace, ns -> lazyBuilder.apply(this));
        } else {
            return computeIfAbsent(dataMap, namespace, ns -> new ConcurrentHashMap<>());
        }
    }

    /**
     * Workaround to fix ConcurrentHashMap stuck bug when call {@link ConcurrentHashMap#computeIfAbsent} recursively.
     * see https://bugs.openjdk.java.net/browse/JDK-8062841.
     */
    private static <K, V> Map<K, V> computeIfAbsent(ConcurrentMap<Object, Map<Object, Object>> map, Object key,
                                                    Function<Object, Map<Object, Object>> function) {
        Map<Object, Object> value = map.get(key);
        if (value == null) {
            value = function.apply(key);
            map.put(key, value);
        }
        return (Map<K, V>) value;
    }

    public void setupLazyNodeData(Object namespace,
                                  Function<BuildContext, Map<Object, Object>> lazyBuildFunction) {
        lazyBuilders.put(namespace, lazyBuildFunction);
    }

    @Override
    public void merge(BuildContext buildContext) {
        if (buildContext instanceof SimpleBuildContext) {
            SimpleBuildContext other = (SimpleBuildContext) buildContext;
            other.dataMap.forEach(
                    (namespace, values) -> dataMap.merge(namespace, values, MergeUtils::merge));
            other.lazyBuilders.forEach(lazyBuilders::putIfAbsent);
            lazyBuilders.keySet().forEach(key -> {
                dataMap.remove(key);
                lazyDataMap.remove(key);
            });
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        return "SimpleBuildContext{" +
                "dataMap=" + dataMap +
                ", lazyDataMap=" + lazyDataMap +
                ", lazyBuilders=" + lazyBuilders +
                '}';
    }
}
