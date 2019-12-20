package com.netflix.zuul.message.http;

import com.netflix.zuul.message.Headers;

public interface CookieParser {

    /**
     * Parse into {@link Cookies} from request headers.
     */
    Cookies parseCookies(Headers requestHeaders);
}
