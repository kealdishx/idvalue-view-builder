package com.github.kealdish.idvalue.view.builder;

import java.util.function.BiFunction;

public interface Lazy {
    Object sourceNamespace();

    Object targetNamespace();

    BiFunction<?, ?, ?> builder();
}
