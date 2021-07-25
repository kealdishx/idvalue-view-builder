package com.github.kealdish.idvalue.view.builder.context;

import java.util.Map;

/**
 * @author kealdish
 */
public interface BuildContext {

    default <K, V> Map<K, V> getData(Class<V> type) {
        return getData((Object) type);
    }

    <K, V> Map<K, V> getData(Object namespace);

    void merge(BuildContext buildContext);
}
