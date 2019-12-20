package com.netflix.zuul.message.http;

import com.netflix.zuul.message.Headers;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A plain vanilla cookie parser.
 */
@Singleton
public class ZuulCookieParser implements CookieParser {

    @Override
    public Cookies parseCookies(Headers requestHeaders) {
        List<String> cookieHeaders = requestHeaders.get(HttpHeaderNames.COOKIE);
        return parseFromHeaders(cookieHeaders);
    }

    private Cookies parseFromHeaders(List<String> requestHeaders) {
        List<Cookie> cookies = collectCookies(requestHeaders);
        return toZuulCookies(cookies);
    }

    /**
     * Sanitizes and collects cookies from headers into a list of {@link Cookie}s.
     * Note there could be multiple cookies in the header with same name.
     */
    private List<Cookie> collectCookies(List<String> headers) {
        return headers.stream()
                .flatMap(headerValues -> Arrays.stream(headerValues.split(";")))
                .map(String::trim)
                .map(this::toCookie)
                .collect(Collectors.toList());
    }

    private DefaultCookie toCookie(String header) {

        // Why not use CookieDecoder::decode?

        // Currently CookieDecoder::decode will NOT return more than one cookie per-name, because CookieDecoder uses
        // a TreeSet to back it, which uses io.netty.handler.codec.http.cookie.DefaultCookie, who's
        // equals/hashCode/compareTo only look at the same.
        // Filed https://github.com/netty/netty/issues/7210

        int equalsIdx = header.indexOf("=");
        String key = (equalsIdx != -1) ? header.substring(0, equalsIdx) : header;
        String value = (equalsIdx != -1) ? header.substring(equalsIdx + 1) : "";
        return new DefaultCookie(key, value);
    }

    private Cookies toZuulCookies(List<Cookie> cookies) {
        Cookies zuulCookies = new Cookies();
        cookies.forEach(zuulCookies::add);
        return zuulCookies;
    }
}
