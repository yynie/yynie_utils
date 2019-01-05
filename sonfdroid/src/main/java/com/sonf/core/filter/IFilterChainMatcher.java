package com.sonf.core.filter;

public interface IFilterChainMatcher {
    boolean isMatched(String filterName);
}
