package com.sonf.nio;

import android.os.SystemClock;

import java.util.concurrent.ConcurrentHashMap;

public class DNSCache {
    private static DNSCache sInstance;
    private ConcurrentHashMap<String, CachedInfo> map = new ConcurrentHashMap<String, CachedInfo>();

    public static DNSCache getInstance() {
        synchronized (DNSCache.class){
            if(sInstance == null){
                sInstance = new DNSCache();
            }
            return sInstance;
        }
    }

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

    public final String get(String host){
        CachedInfo cachedInfo = map.get(host);
        return (cachedInfo == null ? null :cachedInfo.getIpAddress());
    }

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
