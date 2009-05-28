/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
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

package com.sun.jersey.oauth.tests;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import junit.framework.Assert;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.oauth.server.OAuthServerRequest;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;
import com.sun.jersey.oauth.signature.OAuthSignature;
import com.sun.jersey.oauth.signature.OAuthSignatureException;

@Path("/photos")
public class PhotosResource extends Assert {

    @GET @Produces("text/plain")
    public String handle(
    @QueryParam("file") String file,
    @QueryParam("size") String size,
    @Context HttpContext hc) {

        OAuthServerRequest osr = new OAuthServerRequest(hc.getRequest());

        OAuthSecrets secrets = new OAuthSecrets().
         consumerSecret("kd94hf93k423kf44").tokenSecret("pfkkdhi9sl3r4s00");

        OAuthParameters params = new OAuthParameters().readRequest(osr);

        // ensure query parameters are as expected
        assertEquals(file, "vacation.jpg");
        assertEquals(size, "original");

        // ensure query parameters correctly parsed into OAuth parameters object
        assertEquals(params.getConsumerKey(), "dpf43f3p2l4k3l03");
        assertEquals(params.getToken(), "nnch734d00sl2jdk");
        assertEquals(params.getSignatureMethod(), "HMAC-SHA1");
        assertEquals(params.getTimestamp(), "1191242096");
        assertEquals(params.getNonce(), "kllo9940pd9333jh");
        assertEquals(params.getVersion(), "1.0");

        try {
            // verify the HMAC-SHA1 signature
            assertTrue(OAuthSignature.verify(osr, params, secrets));
        }
        catch (OAuthSignatureException ose) {
            fail(ose.getMessage());
        }

        return "PHOTO";
    }
}

