/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License. 
 * 
 * You can obtain a copy of the License at:
 *     https://jersey.dev.java.net/license.txt
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at:
 *     https://jersey.dev.java.net/license.txt
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyrighted [year] [name of copyright owner]"
 */

package com.sun.ws.rest.api.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Methods describing the set of resources in a Web application. It is anticipated
 * that an implementation of this interface will be generated by tools rather
 * than directly implemented by developers.
 */
public interface ResourceConfig {
    /**
     * URI should be normalized as specified by java.net.URI.normalize() method javadoc
     */
    public static final String NORMALIZE_URI = "com.sun.ws.rest.config.feature.NormalizeURI";
    
    /**
     * URI path should be canonicalized by removing contiguous slashes. (i.e. all /+ should be replaced by /)
     */
    public static final String CANONICALIZE_URI_PATH = "com.sun.ws.rest.config.feature.CanonicalizeURIPath";
    
    /**
     * If REDIRECT is set and either of NORMALIZE_URI and CANONICALIZE_URI_PATH is set and the appropriate operation results
     * in different URI, the client is (temporarily) redirected to the new URI. Otherwise the request is silently forwarded
     * to the new URI/resource
     */
    public static final String REDIRECT = "com.sun.ws.rest.config.feature.Redirect";
    
    /**
     * Matric params (/in/uri/like/this/par1=a;par2=b) should be ignored
     */
    public static final String IGNORE_MATRIX_PARAMS = "com.sun.ws.rest.config.feature.IgnoreMatrixParams";
   
 
    /**
     * Get the set of resource classes that should be deployed. A resource class
     * is a class with a <code>UriTemplate</code> annotation.
     * @return the set of resource classes.
     */
    Set<Class> getResourceClasses();
    
    /**
     * Get the unmodifiable map containing set of features associated with the WebApplication 
     * @return the unmodifiable map of features.
     */
    Map<String, Boolean> getFeatures();
}
