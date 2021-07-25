package com.github.kealdish.idvalue.view.builder;

import com.github.kealdish.idvalue.view.builder.context.BuildContext;

import static java.util.Collections.singleton;

public interface ViewBuilder<B extends BuildContext> {
    void buildMulti(Iterable<?> sources, B buildContext);

    default void buildSingle(Object one, B buildContext) {
        buildMulti(singleton(one), buildContext);
    }
}
