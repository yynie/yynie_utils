package com.sonf.core.session;


import java.io.Serializable;

/**
 * AttributeKey will be built from class name and specified key name.
 * It will be used as a map key to store data into a session's attribute map.
 *
 * For example:<br>
 *   <pre>
 *       //create a key from DefaultFilterChain
 *       AttributeKey key = new AttributeKey(DefaultFilterChain.class, "connectFuture");
 *
 *       //store
 *       String save = "store something in session";
 *       session.setAttribute(key, save);
 *
 *       //get and remove the stored data
 *       Object got = session.removeAttribute(key)
 *   </pre>
 */
public final class AttributeKey implements Serializable {
    private final String name;

    /***
     * Build a new instance
     *
     * @param clazz  the class this key belong to
     * @param name   key name
     */
    public AttributeKey(Class<?> clazz, String name){
        this.name = clazz.getName() + "." + name + "@" + Integer.toHexString(this.hashCode());
    }

    @Override
    public String toString(){
        return this.name;
    }

    @Override
    public int hashCode(){
        int h = 17 * 37 + ((name == null) ? 0 : name.hashCode());
        return h;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;

        if(!(obj instanceof  AttributeKey)) return false;

        AttributeKey other = (AttributeKey) obj;

        return this.name.equals(other.name);
    }

}
