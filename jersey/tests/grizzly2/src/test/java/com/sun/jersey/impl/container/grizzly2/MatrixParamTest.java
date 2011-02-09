/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jersey.impl.container.grizzly2;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class MatrixParamTest extends AbstractGrizzlyServerTester {
    @Path("/test")
    public static class MatrixParamResource {
        @GET
        public String get(@MatrixParam("x") String x, @MatrixParam("y") String y) {
            return y;
        }
    }
        
    public MatrixParamTest(String testName) {
        super(testName);
    }
    
    public void testMatrixParam() {
        startServer(MatrixParamResource.class);

        UriBuilder base = getUri().path("test");
            
        WebResource r = Client.create().resource(base.clone().matrixParam("y", "1").build());
        assertEquals("1", r.get(String.class));
        r = Client.create().resource(base.clone().
                matrixParam("x", "1").matrixParam("y", "1%20%2B%202").build());
        assertEquals("1 + 2", r.get(String.class));
        r = Client.create().resource(base.clone().
                matrixParam("x", "1").matrixParam("y", "1%20%26%202").build());
        assertEquals("1 & 2", r.get(String.class));
        r = Client.create().resource(base.clone().
                matrixParam("x", "1").matrixParam("y", "1%20%7C%7C%202").build());
        assertEquals("1 || 2", r.get(String.class));
    }
}
