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

package com.sun.ws.rest.impl.modelapi.annotation;

import com.sun.ws.rest.api.model.AbstractResource;
import com.sun.ws.rest.api.model.AbstractResourceMethod;
import com.sun.ws.rest.api.model.AbstractWebAppModel;
import com.sun.ws.rest.api.model.AbstractSubResourceLocator;
import com.sun.ws.rest.api.model.AbstractSubResourceMethod;
import com.sun.ws.rest.api.model.Parameter;
import com.sun.ws.rest.api.model.Parameterized;
import com.sun.ws.rest.api.model.UriTemplateValue;
import com.sun.ws.rest.impl.model.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.QueryParam;
import javax.ws.rs.UriParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpContext;

/**
 *
 * @author japod
 */
public class IntrospectionModeller {
    
    private static final Logger LOGGER = Logger.getLogger(IntrospectionModeller.class.getName());
    
    // TODO: just leave the code in static methods ?
    public static final AbstractWebAppModel createModel(Set<Class> resourceClasses) {
        
        AbstractWebAppModel model = new AbstractWebAppModel();
        
        // validation is done by resource model validation method
        // so returning an empty resource model is ok
        if (null == resourceClasses) {
            return model;
        }
        
        for (Class<?> resourceClass : resourceClasses) {
            final AbstractResource resource = createResource(resourceClass);
            if (resource.isRootResource()) {
                model.getRootResources().add(resource);
            } else {
                model.getSubResources().add(resource);
            }
        }
        // TODO: what about views ?
        return model;
    }
    
    public static final AbstractResource createResource(Class<?> resourceClass) {
        final Path rPathAnnotation = resourceClass.getAnnotation(Path.class);
        final boolean isRootResourceClass = (null != rPathAnnotation);
        
        final boolean isEncodedAnotOnClass = (null != resourceClass.getAnnotation(Encoded.class));
        
        AbstractResource resource;
        
        if (isRootResourceClass) {
            resource = new AbstractResource(resourceClass,
                    new UriTemplateValue(rPathAnnotation.value(), rPathAnnotation.encode(), rPathAnnotation.limited()));
        } else { // just a subresource class
            resource = new AbstractResource(resourceClass);
        }
        
        if (null != resource.getConstructor()) {
            processParameters(resource.getConstructor(), resource.getConstructor().getCtor(), isEncodedAnotOnClass);
        }
        
        final MethodList methodList = new MethodList(resourceClass);
        
        final ConsumeMime classScopeConsumeMimeAnnotation = resourceClass.getAnnotation(ConsumeMime.class);
        final ProduceMime classScopeProduceMimeAnnotation = resourceClass.getAnnotation(ProduceMime.class);
        workOutResourceMethodsList(resource, methodList, isEncodedAnotOnClass, classScopeConsumeMimeAnnotation, classScopeProduceMimeAnnotation);
        workOutSubResourceMethodsList(resource, methodList, isEncodedAnotOnClass, classScopeConsumeMimeAnnotation, classScopeProduceMimeAnnotation);
        workOutSubResourceLocatorsList(resource, methodList, isEncodedAnotOnClass);
        
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("A new abstract resource created by IntrospectionModeler: \n" + resource.toString());
        }
        
        return resource;
    }
    
    private static final void findOutConsumeMimeTypes(
            AbstractResourceMethod resourceMethod, ConsumeMime classScopeConsumeMimeAnnotation) {
        
        if (resourceMethod.getMethod().isAnnotationPresent(ConsumeMime.class)){
            final ConsumeMime consumeMimeAnnotation = resourceMethod.getMethod().getAnnotation(ConsumeMime.class);
            resourceMethod.getSupportedInputTypes().addAll(MimeHelper.createMediaTypes(consumeMimeAnnotation));
        } else { // have to use the annotation from class
            resourceMethod.getSupportedInputTypes().addAll(MimeHelper.createMediaTypes(classScopeConsumeMimeAnnotation));
        }
    }
    
    private static final void findOutProduceMimeTypes(
            AbstractResourceMethod resourceMethod, ProduceMime classScopeProduceMimeAnnotation) {
        
        if (resourceMethod.getMethod().isAnnotationPresent(ProduceMime.class)){
            final ProduceMime produceMimeAnnotation = resourceMethod.getMethod().getAnnotation(ProduceMime.class);
            resourceMethod.getSupportedOutputTypes().addAll(MimeHelper.createMediaTypes(produceMimeAnnotation));
        } else { // have to use the annotation from class
            resourceMethod.getSupportedOutputTypes().addAll(MimeHelper.createMediaTypes(classScopeProduceMimeAnnotation));
        }
    }
    
    
    private static final void workOutResourceMethodsList(
            AbstractResource resource, MethodList methodList, boolean isEncoded,
            ConsumeMime classScopeConsumeMimeAnnotation, ProduceMime classScopeProduceMimeAnnotation) {
        
        // TODO what to do about multiple meta HttpMethod annotations?
        // Should the method be added more than once to the model and
        // then validation could reject?
        for (Method method : methodList.hasMetaAnnotation(HttpMethod.class).hasNotAnnotation(Path.class)) {
            final AbstractResourceMethod resourceMethod = new AbstractResourceMethod(method, 
                    getMetaAnnotations(method, HttpMethod.class).get(0).value());
            
            findOutConsumeMimeTypes(resourceMethod, classScopeConsumeMimeAnnotation);
            findOutProduceMimeTypes(resourceMethod, classScopeProduceMimeAnnotation);
            processParameters(resourceMethod, resourceMethod.getMethod(), isEncoded);
            
            resource.getResourceMethods().add(resourceMethod);
        }
    }
    
    private static final void workOutSubResourceMethodsList(
            AbstractResource resource, MethodList methodList, boolean isEncoded,
            ConsumeMime classScopeConsumeMimeAnnotation, ProduceMime classScopeProduceMimeAnnotation) {
        
        // TODO what to do about multiple meta HttpMethod annotations?
        // Should the method be added more than once to the model and
        // then validation could reject?
        for (Method method : methodList.hasMetaAnnotation(HttpMethod.class).hasAnnotation(Path.class)) {
            final Path mPathAnnotation = method.getAnnotation(Path.class);
            final AbstractSubResourceMethod subResourceMethod = new AbstractSubResourceMethod(
                        method,
                        new UriTemplateValue(mPathAnnotation.value(), mPathAnnotation.encode(), mPathAnnotation.limited()),
                        getMetaAnnotations(method, HttpMethod.class).get(0).value());
       
            findOutConsumeMimeTypes(subResourceMethod, classScopeConsumeMimeAnnotation);
            findOutProduceMimeTypes(subResourceMethod, classScopeProduceMimeAnnotation);
            processParameters(subResourceMethod, subResourceMethod.getMethod(), isEncoded);
            
            resource.getSubResourceMethods().add(subResourceMethod);
        }
    }
    
    /**
     * Return a list of specific meta-annotations decared on annotations of
     * a method.
     */
    private static final <T extends Annotation> List<T> getMetaAnnotations(
            Method m, Class<T> annotation) {
        List <T> ma = new ArrayList<T>();
        for (Annotation a : m.getAnnotations()) {
            if (a.annotationType().getAnnotation(annotation) != null) {
                ma.add(a.annotationType().getAnnotation(annotation));
            }
        }
        
        return ma;
    }
    
    
    
    private static final void workOutSubResourceLocatorsList(AbstractResource resource, MethodList methodList, boolean isEncoded) {
        
        for (Method method : methodList.hasNotMetaAnnotation(HttpMethod.class).hasAnnotation(Path.class)) {
            final Path mPathAnnotation = method.getAnnotation(Path.class);
            final AbstractSubResourceLocator subResourceLocator = new AbstractSubResourceLocator(
                    method,
                    new UriTemplateValue(mPathAnnotation.value(), mPathAnnotation.encode(), mPathAnnotation.limited()));
            
            processParameters(subResourceLocator, subResourceLocator.getMethod(), isEncoded);
            
            resource.getSubResourceLocators().add(subResourceLocator);
        }
    }

    private static final void processParameters(Parameterized parametrized, Constructor ctor, boolean isEncoded) {
        processParameters(
                parametrized, 
                ((null != ctor.getAnnotation(Encoded.class)) || isEncoded),
                ctor.getParameterTypes(), ctor.getGenericParameterTypes(), ctor.getParameterAnnotations());
    }
    
    private static final void processParameters(Parameterized parametrized, Method method, boolean isEncoded) {
        processParameters(
                parametrized, 
                ((null != method.getAnnotation(Encoded.class)) || isEncoded),
                method.getParameterTypes(), method.getGenericParameterTypes(), method.getParameterAnnotations());
    }
    
    private static final void processParameters(
        Parameterized parametrized,
        boolean isEncoded,
        Class[] parameterTypes,
        Type[] genericParameterTypes,
        Annotation[][] parameterAnnotations) {
        
        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameter = createParameter(
                    isEncoded, parameterTypes[i], genericParameterTypes[i], parameterAnnotations[i]);
            if (null != parameter) {
                parametrized.getParameters().add(parameter);
            } else {
                // clean up the parameters
                parametrized.getParameters().removeAll(parametrized.getParameters());
                break;
            }
        }
    }
    
    
    private static interface ParamAnnotationHelper {
        public String getValueOf(Annotation a);
        public Parameter.Source getSource();
    }
    
    private static Map<Class, ParamAnnotationHelper> createParamAnotHelperMap() {
        Map<Class, ParamAnnotationHelper> m = new WeakHashMap<Class, ParamAnnotationHelper>();
        m.put(HttpContext.class, new ParamAnnotationHelper() {
            public String getValueOf(Annotation a){ return null;}
            public Parameter.Source getSource(){ return Parameter.Source.CONTEXT;}
        });
        m.put(HeaderParam.class, new ParamAnnotationHelper() {
            public String getValueOf(Annotation a){ return ((HeaderParam)a).value();}
            public Parameter.Source getSource(){ return Parameter.Source.HEADER;}
        });
        m.put(MatrixParam.class, new ParamAnnotationHelper() {
            public String getValueOf(Annotation a){ return ((MatrixParam)a).value();}
            public Parameter.Source getSource(){ return Parameter.Source.MATRIX;}
        });
        m.put(QueryParam.class, new ParamAnnotationHelper() {
            public String getValueOf(Annotation a){ return ((QueryParam)a).value();}
            public Parameter.Source getSource(){ return Parameter.Source.QUERY;}
        });
        m.put(UriParam.class, new ParamAnnotationHelper() {
            public String getValueOf(Annotation a){ return ((UriParam)a).value();}
            public Parameter.Source getSource(){ return Parameter.Source.URI;}
        });
        return Collections.unmodifiableMap(m);
    }
    
    private final static Map<Class, ParamAnnotationHelper> ANOT_HELPER_MAP = createParamAnotHelperMap();
    
    
    private static final Parameter createParameter(
            boolean isEncoded, Class<?> paramClass, Type paramType, Annotation[] annotations) {
        
        if (null == annotations) {
            return null;
        }
        
        Parameter.Source paramSource = null;
        String paramName = null;
        boolean paramEncoded = isEncoded;
        
        String paramDefault = null;
        
        for (Annotation annotation : annotations) {
            if (ANOT_HELPER_MAP.containsKey(annotation.annotationType())) {
                ParamAnnotationHelper helper = ANOT_HELPER_MAP.get(annotation.annotationType());
                if (null != paramSource) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning("Annotation for " + helper.getSource() + " parameter found " +
                                "where another source (" + paramSource + ") has been already used.");
                    }
                    // TODO: throw an exception ?
                }
                paramSource = helper.getSource();
                paramName = helper.getValueOf(annotation);
            } else if (Encoded.class == annotation.annotationType()) {
                paramEncoded = true;
            } else if (DefaultValue.class == annotation.annotationType()) {
                paramDefault = ((DefaultValue)annotation).value();
            }// TODO: should we really ignore unknown annotations ?
        }
        
        if (paramSource == null) {
            paramSource = Parameter.Source.ENTITY;
        }
        
        return new Parameter(paramSource, paramName, paramType, paramClass, paramEncoded, paramDefault);
    }
    
}
