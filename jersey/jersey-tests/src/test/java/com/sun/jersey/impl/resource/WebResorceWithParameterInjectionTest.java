/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.jersey.impl.resource;

import com.sun.jersey.impl.AbstractResourceTester;
import java.net.URI;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class WebResorceWithParameterInjectionTest extends AbstractResourceTester {
    
    public WebResorceWithParameterInjectionTest(String testName) {
        super(testName);
    }
        
    @Path("/{arg1}/{arg2}")
    public static class TestParameterInjectedUriInfo {
        @GET
        public String doGet(@Context UriInfo uriInfo) {
            URI baseUri = uriInfo.getBaseUri();
            URI uri = uriInfo.getAbsolutePath();
            assertEquals(BASE_URI, baseUri);
            assertEquals(UriBuilder.fromUri(BASE_URI).path("a/b").build(), uri);

            return "GET";
        }        
    }
    
    @Path("/{arg1}/{arg2}")
    public static class TestParameterInjectedHttpHeaders {
        @GET
        public String doGet(@Context HttpHeaders httpHeaders) {
            String value = httpHeaders.getRequestHeaders().getFirst("X-TEST");
            assertEquals("TEST", value);
            return "GET";
        }        
    }
        
    @Path("/{arg1}/{arg2}")
    public static class TestParameterInjectedUriInfoHttpHeaders {
        @GET
        public String doGet(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders) {
            String value = httpHeaders.getRequestHeaders().getFirst("X-TEST");
            assertEquals("TEST", value);
            
            URI baseUri = uriInfo.getBaseUri();
            URI uri = uriInfo.getAbsolutePath();
            assertEquals(BASE_URI, baseUri);
            assertEquals(UriBuilder.fromUri(BASE_URI).path("a/b").build(), uri);
            return "GET";
        }        
    }
    
    public void testParameterInjectedUriInfo() {
        initiateWebApplication(TestParameterInjectedUriInfo.class);
        
        assertEquals("GET", resource("a/b").get(String.class));
    }
    
    public void testParameterInjectedHttpHeaders() {
        initiateWebApplication(TestParameterInjectedHttpHeaders.class);
        
        assertEquals("GET", resource("a/b").
                header("X-TEST", "TEST").get(String.class));
    }
    
    public void testParameterInjectedUriInfoHttpHeaders() {
        initiateWebApplication(TestParameterInjectedUriInfoHttpHeaders.class);
        
        assertEquals("GET", resource("a/b").
                header("X-TEST", "TEST").get(String.class));
    }
}
