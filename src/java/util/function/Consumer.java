/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package java.util.function;

import java.util.Objects;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result. Unlike most other functional interfaces, {@code Consumer} is expected
 * to operate via side-effects.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(Object)}.
 *
 * @param <T> the type of the input to the operation
 *
 * @since 1.8
 */
/*
@Data
@Accessors(chain = true)
@AllArgsConstructor
public class Person {
    private Integer age;
    private String name;
}

List<Person> lisiList = new ArrayList<>();
Consumer<Person> consumer  = x -> {
    if (x.getName().equals("lisi")){
        lisiList.add(x);
    }
};
Stream.of(
        new Person(21,"zhangsan"),
        new Person(22,"lisi"),
        new Person(23,"wangwu"),
        new Person(24,"wangwu"),
        new Person(23,"lisi"),
        new Person(26,"lisi"),
        new Person(26,"zhangsan")
).forEach(consumer);

System.out.println(JSON.toJSONString(lisiList));
结果：[{"age":22,"name":"lisi"},{"age":23,"name":"lisi"},{"age":26,"name":"lisi"}]

 */
@FunctionalInterface
public interface Consumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t);

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default Consumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
}
