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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.netflix.zuul.context.SessionContext;
import com.netflix.zuul.message.Headers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HttpRequestMessageImplTest {

    HttpRequestMessageImpl request;

    @Test
    public void testOriginalRequestInfo() {
        HttpQueryParams queryParams = new HttpQueryParams();
        queryParams.add("flag", "5");
        Headers headers = new Headers();
        headers.add("Host", "blah.netflix.com");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");

        request.storeInboundRequest();
        HttpRequestInfo originalRequest = request.getInboundRequest();

        Assert.assertEquals(request.getPort(), originalRequest.getPort());
        Assert.assertEquals(request.getPath(), originalRequest.getPath());
        Assert.assertEquals(request.getQueryParams().getFirst("flag"), originalRequest.getQueryParams().getFirst("flag"));
        Assert.assertEquals(request.getHeaders().getFirst("Host"), originalRequest.getHeaders().getFirst("Host"));

        request.setPort(8080);
        request.setPath("/another/place");
        request.getQueryParams().set("flag", "20");
        request.getHeaders().set("Host", "wah.netflix.com");

        Assert.assertEquals(7002, originalRequest.getPort());
        Assert.assertEquals("/some/where", originalRequest.getPath());
        Assert.assertEquals("5", originalRequest.getQueryParams().getFirst("flag"));
        Assert.assertEquals("blah.netflix.com", originalRequest.getHeaders().getFirst("Host"));
    }

    @Test
    public void testReconstructURI() {
        HttpQueryParams queryParams = new HttpQueryParams();
        queryParams.add("flag", "5");
        Headers headers = new Headers();
        headers.add("Host", "blah.netflix.com");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals("https://blah.netflix.com:7002/some/where?flag=5", request.reconstructURI());

        queryParams = new HttpQueryParams();
        headers = new Headers();
        headers.add("X-Forwarded-Host", "place.netflix.com");
        headers.add("X-Forwarded-Port", "80");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "http", 7002, "localhost");
        Assert.assertEquals("http://place.netflix.com/some/where", request.reconstructURI());

        queryParams = new HttpQueryParams();
        headers = new Headers();
        headers.add("X-Forwarded-Host", "place.netflix.com");
        headers.add("X-Forwarded-Proto", "https");
        headers.add("X-Forwarded-Port", "443");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "http", 7002, "localhost");
        Assert.assertEquals("https://place.netflix.com/some/where", request.reconstructURI());

        queryParams = new HttpQueryParams();
        headers = new Headers();
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "http", 7002, "localhost");
        Assert.assertEquals("http://localhost:7002/some/where", request.reconstructURI());

        queryParams = new HttpQueryParams();
        queryParams.add("flag", "5");
        queryParams.add("flag B", "9");
        headers = new Headers();
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some%20where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals("https://localhost:7002/some%20where?flag=5&flag+B=9", request.reconstructURI());
    }

    @Test
    public void testReconstructURI_immutable() {
        HttpQueryParams queryParams = new HttpQueryParams();
        queryParams.add("flag", "5");
        Headers headers = new Headers();
        headers.add("Host", "blah.netflix.com");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost", true);

        // Check it's the same value 2nd time.
        Assert.assertEquals("https://blah.netflix.com:7002/some/where?flag=5", request.reconstructURI());
        Assert.assertEquals("https://blah.netflix.com:7002/some/where?flag=5", request.reconstructURI());

        // Check that cached on 1st usage.
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost", true);
        request = spy(request);
        when(request._reconstructURI()).thenReturn("http://testhost/blah");
        verify(request, times(1))._reconstructURI();
        Assert.assertEquals("http://testhost/blah", request.reconstructURI());
        Assert.assertEquals("http://testhost/blah", request.reconstructURI());

        // Check that throws exception if we try to mutate it.
        try {
            request.setPath("/new-path");
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testPathAndQuery() {
        HttpQueryParams queryParams = new HttpQueryParams();
        queryParams.add("flag", "5");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, new Headers(),
            "192.168.0.2", "https", 7002, "localhost");

        // Check that value changes.
        Assert.assertEquals("/some/where?flag=5", request.getPathAndQuery());
        request.getQueryParams().add("k", "v");
        Assert.assertEquals("/some/where?flag=5&k=v", request.getPathAndQuery());
        request.setPath("/other");
        Assert.assertEquals("/other?flag=5&k=v", request.getPathAndQuery());
    }

    @Test
    public void testPathAndQuery_immutable() {
        HttpQueryParams queryParams = new HttpQueryParams();
        queryParams.add("flag", "5");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, new Headers(),
            "192.168.0.2", "https", 7002, "localhost", true);

        // Check it's the same value 2nd time.
        Assert.assertEquals("/some/where?flag=5", request.getPathAndQuery());
        Assert.assertEquals("/some/where?flag=5", request.getPathAndQuery());

        // Check that cached on 1st usage.
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, new Headers(),
            "192.168.0.2", "https", 7002, "localhost", true);
        request = spy(request);
        when(request.generatePathAndQuery()).thenReturn("/blah");
        verify(request, times(1)).generatePathAndQuery();
        Assert.assertEquals("/blah", request.getPathAndQuery());
        Assert.assertEquals("/blah", request.getPathAndQuery());
    }

    @Test
    public void testGetOriginalHost() {
        HttpQueryParams queryParams = new HttpQueryParams();
        Headers headers = new Headers();
        headers.add("Host", "blah.netflix.com");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals("blah.netflix.com", request.getOriginalHost());

        headers = new Headers();
        headers.add("Host", "blah.netflix.com");
        headers.add("X-Forwarded-Host", "foo.netflix.com");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals("foo.netflix.com", request.getOriginalHost());

        headers = new Headers();
        headers.add("X-Forwarded-Host", "foo.netflix.com");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals("foo.netflix.com", request.getOriginalHost());

        headers = new Headers();
        headers.add("Host", "blah.netflix.com:8080");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals("blah.netflix.com", request.getOriginalHost());
    }

    @Test
    public void testGetOriginalPort() {
        HttpQueryParams queryParams = new HttpQueryParams();
        Headers headers = new Headers();
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals(7002, request.getOriginalPort());

        headers = new Headers();
        headers.add("Host", "blah.netflix.com");
        headers.add("X-Forwarded-Port", "443");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals(443, request.getOriginalPort());

        headers = new Headers();
        headers.add("Host", "blah.netflix.com:443");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals(443, request.getOriginalPort());

        headers = new Headers();
        headers.add("Host", "blah.netflix.com:443");
        headers.add("X-Forwarded-Port", "7005");
        request = new HttpRequestMessageImpl(new SessionContext(), "HTTP/1.1", "POST", "/some/where", queryParams, headers,
            "192.168.0.2", "https", 7002, "localhost");
        Assert.assertEquals(7005, request.getOriginalPort());
    }

    @Test
    public void testCleanCookieHeaders() {
        assertEquals("BlahId=12345; something=67890;", HttpRequestMessageImpl.cleanCookieHeader("BlahId=12345; Secure, something=67890;"));
        assertEquals("BlahId=12345; something=67890;", HttpRequestMessageImpl.cleanCookieHeader("BlahId=12345; something=67890;"));
        assertEquals(" BlahId=12345; something=67890;", HttpRequestMessageImpl.cleanCookieHeader(" Secure, BlahId=12345; Secure, something=67890;"));
        assertEquals("", HttpRequestMessageImpl.cleanCookieHeader(""));
    }

    @Test
    public void testCookieParserIsInvoked() {
        // setup:
        CookieParser cookieParser = mock(CookieParser.class);
        Headers headers = new Headers();
        request = new HttpRequestMessageImpl(new SessionContext(), null, null, null, null, headers,
                "192.168.0.2", "https", 7002, "localhost", cookieParser);

        // when:
        request.parseCookies();

        // then:
        verify(cookieParser, times(1)).parseCookies(headers);
    }
}
