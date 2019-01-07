package com.sonf.nio;

import android.os.SystemClock;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A single instance cache for DNS resolve
 * @author <a href="mailto:yy_nie@hotmail.com">Yan.Nie</a>
 */
public class DNSCache {
    private static DNSCache sInstance;
    private ConcurrentHashMap<String, CachedInfo> map = new ConcurrentHashMap<String, CachedInfo>();

    /**
     * @return the single DNSCache instance
     */
    public static DNSCache getInstance() {
        synchronized (DNSCache.class){
            if(sInstance == null){
                sInstance = new DNSCache();
            }
            return sInstance;
        }
    }

    /**
     * Static method to check whether the specified string is IPV4 address or not
     *
     * @param check string to be check
     * @return true if the specified string is an IPV4 address, otherwise return false
     */
    public static boolean isIpV4(String check){
        String[] ipV4s = check.split("\\.");
        if(ipV4s.length != 4)
            return false;
        for(String s:ipV4s){
            try{
                Integer i = Integer.parseInt(s);
                if(i < 0 || i > 255){
                    return false;
                }
            }catch (NumberFormatException e){
                return false;
            }
        }
        return true;
    }

    /**
     * Get the cached IPV4 address for the specified host
     *
     * @param host the host to be resolved
     * @return the IPV4 address string if found in cache, or <tt>null</tt> otherwise
     */
    public final String get(String host){
        CachedInfo cachedInfo = map.get(host);
        return (cachedInfo == null ? null :cachedInfo.getIpAddress());
    }

    /**
     * Put a host-IPV4 pair into the cache
     *
     * @param host the host to be resolved
     * @param IP   the IPV4 address corresponding to the host
     * @param validMs  milliseconds how long to cache this host-IPV4 pair
     */
    public void put(String host, String IP, long validMs){
        map.put(host, new CachedInfo(IP, validMs));
    }

    private class CachedInfo{
        private String ipAddress;
        private long expiredAt;

        private CachedInfo(String IP, long validMs){
            ipAddress = IP;
            expiredAt = SystemClock.elapsedRealtime() + validMs;
        }

        private final String getIpAddress() {
            if(SystemClock.elapsedRealtime() < expiredAt) {
                return ipAddress;
            }
            return null;
        }
    }
}
