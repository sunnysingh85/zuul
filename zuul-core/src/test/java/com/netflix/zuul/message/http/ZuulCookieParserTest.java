/*
 * Copyright 2019 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package com.netflix.zuul.message.http;

import com.netflix.zuul.message.Headers;
import io.netty.handler.codec.http.Cookie;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ZuulCookieParserTest {

    private ZuulCookieParser parser;

    @Before
    public void setup() {
        parser = new ZuulCookieParser();
    }

    @Test
    public void testParseHappyPath() {
        Headers headers = new Headers();
        headers.add(HttpHeaderNames.COOKIE, "Id=;Id2=");

        Cookies cookies = parser.parseCookies(headers);
        List<Cookie> id2Cookies = cookies.get("Id2");

        assertEquals(1, id2Cookies.size());
        Cookie cookie = id2Cookies.iterator().next();
        assertEquals("", cookie.value());
    }

    @Test
    public void testParseMultipleCookies() {
        Headers headers = new Headers();
        headers.add(HttpHeaderNames.COOKIE, "Id=1; Id=2; Id=3");

        Cookies parsedCookies = parser.parseCookies(headers);
        List<Cookie> cookies = parsedCookies.getAll();

        assertEquals(3, cookies.size());
        Iterator<Cookie> it = cookies.iterator();

        Cookie cookie = it.next();
        assertNotNull(cookie);
        assertEquals("1", cookie.value());
        cookie = it.next();
        assertNotNull(cookie);
        assertEquals("2", cookie.value());
        cookie = it.next();
        assertNotNull(cookie);
        assertEquals("3", cookie.value());
    }

    @Test
    public void testParseCookieNotFound() {
        Headers headers = new Headers();
        headers.add(HttpHeaderNames.COOKIE, "Id=;Id2=");

        Cookies parsedCookies = parser.parseCookies(headers);
        List<Cookie> cookies = parsedCookies.get("Id3");

        assertNull(cookies);
    }
}
