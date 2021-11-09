package com.fldy;

/**
 * 容器监听
 */
public interface UniverseProcessor {

    Object before(String name, Object o);

    Object after(String name, Object o);

}
