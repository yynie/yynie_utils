package com.sonf.core.filter;

import com.sonf.core.filter.IFilterChain.Entry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FilterChainBuilder {

    private final List<Entry> entries;

    /**
     * Creates a new instance with an empty filter list.
     */
    public FilterChainBuilder() {
        entries = new CopyOnWriteArrayList<Entry>();
    }

    public void buildChain(IFilterChainMatcher matcher, IFilterChain chain){
        for (Entry e : entries) {
            if((matcher == null)
                || (matcher.isMatched(e.getName()))){
                chain.addLast(e.getName(), e.getFilter());
            }
        }
    }

    public synchronized void add(String name, IFilter filter) {
        register(entries.size(), new EntryImpl(name, filter));
    }

    public boolean contains(String name) {
        return getEntry(name) != null;
    }

    public Entry getEntry(String name) {
        for (Entry e : entries) {
            if (e.getName().equals(name)) {
                return e;
            }
        }

        return null;
    }

    private void register(int index, Entry e) {
        if (contains(e.getName())) {
            throw new IllegalArgumentException("Other filter is using the same name: " + e.getName());
        }

        entries.add(index, e);
    }

    public synchronized void clear() {
        entries.clear();
    }

    private final class EntryImpl implements Entry {
        private final String name;
        private volatile IFilter filter;

        private EntryImpl(String name, IFilter filter) {
            if (name == null || name.equals("head") || name.equals("tail")) {
                throw new IllegalArgumentException("name");
            }
            if (filter == null) {
                throw new IllegalArgumentException("filter");
            }

            this.name = name;
            this.filter = filter;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public IFilter getFilter() {
            return filter;
        }

        @Override
        public Entry getNextEntry() {
            return null;
        }

        @Override
        public Entry getPrevEntry() {
            return null;
        }
    }
}
