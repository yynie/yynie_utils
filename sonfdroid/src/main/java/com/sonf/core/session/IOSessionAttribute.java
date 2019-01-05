package com.sonf.core.session;

import java.util.Set;

public interface IOSessionAttribute {
    /**
     * @return the value associated with the specified key, null if no such attribute,
     *
     * @param key The key we are looking for
     */
    Object get(Object key);

    /**
     * Sets a user-defined attribute.
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  null if it is new.
     */
    Object set( Object key, Object value);

    /**
     * Sets a user defined attribute if the attribute with the specified key
     * is not set yet.
     *
     * @param key The key we are looking for
     * @param value The value to inject
     * @return The previous attribute
     */
    Object setIfAbsent(Object key, Object value);

    /**
     * Removes a user-defined attribute with the specified key.
     *
     * @return The old value of the attribute.
     * @param key The key we are looking for
     */
    Object remove(Object key);

    /**
     * @return <tt>true</tt> if this session contains the attribute with
     * the specified <tt>key</tt>.
     *
     * @param key The key we are looking for
     */
    boolean contains(Object key);

    /**
     * @return the set of keys of all user-defined attributes.
     *
     */
    Set<Object> getKeys();

}
