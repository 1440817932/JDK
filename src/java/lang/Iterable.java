/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.lang;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * Implementing this interface allows an object to be the target of
 * the "for-each loop" statement. See
 * <strong>
 * <a href="{@docRoot}/../technotes/guides/language/foreach.html">For-each Loop</a>
 * </strong>
 *
 * @param <T> the type of elements returned by the iterator
 *
 * @since 1.5
 * @jls 14.14.2 The enhanced for statement
 */

/**
 * #**default
 *      介绍：default是在java8中引入的关键字，也可称为Virtual
 * extension methods——虚拟扩展方法。是指，在接口内部包含了一些默认的方法实现（也就是接口中可以包含方法体，这打破了Java之前版本对接口的语法限制），
 * 从而使得接口在进行扩展的时候，不会破坏与接口相关的实现类代码。
 * ————————————————————————————————————————————————————————————————
 *      为什么要有这个特性：
 *      首先，之前的接口是个双刃剑，好处是面向抽象而不是面向具体编程，缺陷是，当需要修改接口时候，需要修改全部实现该接口的类，
 *      目前的java8之前的集合框架没有foreach方法，通常能想到的解决办法是在JDK里给相关的接口添加新的方法及实现。
 *      然而，对于已经发布的版本，是没法在给接口添加新方法的同时不影响已有的实现。所以引进的默认方法。
 *      他们的目的是为了解决接口的修改与现有的实现不兼容的问题。
 * ————————————————————————————————————————————————————————————————
 *     1） 同时继承两个接口
 *          1）创建接口Interface1,接口Interface1中定义了默认方法helloWorld()
 *          2）创建接口Interface2,接口Interface2中也定义了默认方法helloWorld()
 *          实现类MyImplement即实现了接口Interface1又实现了接口Interface2，恰巧两个接口中都定义可相同的默认方法。
 *          说白了就是编译器此时已经被干懵了，当我们在MyImplement类中调用方法时，
 *          它不知道该去调用Interface1的默认方法还是去调用Interface2的方法。解决方法就是在实现类中实现该方法。
 *          public class MyImplement implements Interface1,Interface2 {
 *               @Override
 *              public void helloWorld() {
 *                       System.out.println("hi i'm from MyImplement");
 *                       }
 *           public static void main(String[] args) {
 *               MyImplement myImplement = new MyImplement();
 *                       myImplement.helloWorld();
 *                       }
 *              }
 * ————————————————————————————————————————————————————————————————
 *      2）创建一个实现类MyImplement2，该实现类不仅继承了MyImplement并且实现了Interface2。
 *
 *      此时在实现类MyImplement2中调用helloWorld()方法，到底执行的是MyImplement中的方法还是执行Interface2中的方法？
 *      答:因为类优先于接口，所以将会执行MyImplement中的方法。
 * ————————————————————————————————————————————————————————————————
 */

public interface Iterable<T> {
    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    Iterator<T> iterator();

    /**
     * Performs the given action for each element of the {@code Iterable}
     * until all elements have been processed or the action throws an
     * exception.  Unless otherwise specified by the implementing class,
     * actions are performed in the order of iteration (if an iteration order
     * is specified).  Exceptions thrown by the action are relayed to the
     * caller.
     *
     * @implSpec
     * <p>The default implementation behaves as if:
     * <pre>{@code
     *     for (T t : this)
     *         action.accept(t);
     * }</pre>
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @since 1.8
     */
    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        for (T t : this) {
            action.accept(t);
        }
    }

    /**
     * Creates a {@link Spliterator} over the elements described by this
     * {@code Iterable}.
     *
     * @implSpec
     * The default implementation creates an
     * <em><a href="Spliterator.html#binding">early-binding</a></em>
     * spliterator from the iterable's {@code Iterator}.  The spliterator
     * inherits the <em>fail-fast</em> properties of the iterable's iterator.
     *
     * @implNote
     * The default implementation should usually be overridden.  The
     * spliterator returned by the default implementation has poor splitting
     * capabilities, is unsized, and does not report any spliterator
     * characteristics. Implementing classes can nearly always provide a
     * better implementation.
     *
     * @return a {@code Spliterator} over the elements described by this
     * {@code Iterable}.
     * @since 1.8
     */
    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }
}
