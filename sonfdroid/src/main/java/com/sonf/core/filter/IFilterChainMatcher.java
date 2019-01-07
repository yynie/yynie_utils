package com.sonf.core.filter;

import com.sonf.core.session.IOSession;

/** Matcher to determine whether a Filter should be added into the session'chain */

public interface IFilterChainMatcher {
    /**
     * @param session
     * @param filterName
     * @return whether a Filter should be added into the session'chain
     */
    boolean isMatched(IOSession session, String filterName);
}
