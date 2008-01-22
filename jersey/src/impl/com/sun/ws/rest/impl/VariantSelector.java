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

package com.sun.ws.rest.impl;

import com.sun.ws.rest.api.core.HttpRequestContext;
import com.sun.ws.rest.impl.http.header.AcceptableLanguageTag;
import com.sun.ws.rest.impl.http.header.AcceptableMediaType;
import com.sun.ws.rest.impl.http.header.AcceptableToken;
import com.sun.ws.rest.impl.http.header.QualityFactor;
import com.sun.ws.rest.impl.model.HttpHelper;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public final class VariantSelector {
    
    private VariantSelector() { }
    
    private static final class ListEntry<T> {
        private T t;
        private ListEntry<T> prev;
        private ListEntry<T> next;
        
        public ListEntry() { }
        
        public ListEntry(ListEntry<T> prev, T t) {
            this.t = t;
            
            prev.next = this;
            this.prev = prev;
        }
        
        public T value() {
            return t;
        }
        
        public ListEntry<T> next() {
            return next;
        }

        public void removeTail() {
            if (next != null) {
                next.prev = null;
                next = null;
            }
        }
        
        public void remove() {
            if (prev != null) {
                prev.next = next;
            }
            if (next != null) {
                next.prev = prev;
            }            
            prev = next = null;
        }
        
        public void insertAfter(ListEntry<T> e) {
            if (this.next == e)
                return;
            
            e.remove();
            e.prev = this;
            e.next = this.next;
            if (e.next != null)
                e.next.prev = e;
            this.next = e;
        }
        
        public static <T> ListEntry<T> create(Collection<T> l) {
            ListEntry<T> head = new ListEntry<T>();
            ListEntry<T> e = head;
            for (T t: l) {
                e = new ListEntry<T>(e, t);
            }
            
            return head;
        }
    }
    
    /**
     * Interface to get a dimension value from a variant and check if an
     * acceptable dimension value is compatable with a dimension value.
     * 
     * @param T the acceptable dimension value type
     * @param U the dimension value type
     */
    private static interface DimensionChecker<T, U> {
        /**
         * Get the dimension value from the variant
         * 
         * @param v the variant
         * @return the dimension value
         */
        U getDimension(Variant v);
        
        /**
         * Ascertain if the acceptable dimension value is compatible with
         * the dimension value
         * 
         * @param t the acceptable dimension value
         * @param u the dimension value
         * @return true if the acceptable dimension value is compatible with
         *         the dimension value
         */
        boolean isCompatible(T t, U u);
    }
    
    private static final DimensionChecker<AcceptableMediaType, MediaType> MEDIA_TYPE_DC = 
            new DimensionChecker<AcceptableMediaType, MediaType>() {
        public MediaType getDimension(Variant v) {
            return v.getMediaType();
        }

        public boolean isCompatible(AcceptableMediaType t, MediaType u) {
            return t.isCompatible(u);
        }        
    };
    
    private static final DimensionChecker<AcceptableLanguageTag, String> LANGUAGE_TAG_DC = 
            new DimensionChecker<AcceptableLanguageTag, String>() {
        public String getDimension(Variant v) {
            return v.getLanguage();
        }

        public boolean isCompatible(AcceptableLanguageTag t, String u) {
            return t.isCompatible(u);
        }        
    };
    
    private static final DimensionChecker<AcceptableToken, String> CHARSET_DC = 
            new DimensionChecker<AcceptableToken, String>() {
        public String getDimension(Variant v) {
            MediaType m = v.getMediaType();
            return (m != null) ? m.getParameters().get("charset") : null;
        }

        public boolean isCompatible(AcceptableToken t, String u) {
            return t.isCompatible(u);
        }        
    };
    
    private static final DimensionChecker<AcceptableToken, String> ENCODING_DC = 
            new DimensionChecker<AcceptableToken, String>() {
        public String getDimension(Variant v) {
            return v.getEncoding();
        }

        public boolean isCompatible(AcceptableToken t, String u) {
            return t.isCompatible(u);
        }        
    };
    
    /**
     * Select variants for a given dimension. 
     * 
     * @param head the head of the list of variants. The list will be modified
     *        so that it only contains variants that are the most acceptable.
     *        Hence any variants that are not acceptable or are less acceptable 
     *        are removed from the list.
     * @param as the list of acceptable dimension values, ordered by the quality
     *        parameter, with the highest quality dimension value occuring first.
     * @param dc the dimension checker
     */
    private static <T extends QualityFactor, U> void selectVariants(ListEntry<Variant> head, 
            List<T> as, DimensionChecker<T, U> dc) {
        int q = QualityFactor.MINUMUM_QUALITY;
        // Iterate over the acceptable entries
        // This assumes the the entries are ordered by the quality
        for (T a : as) {
            // If entries of the higest quality factor have already been
            // selected no need to continue further
            if (a.getQuality() < q)
                break;
            
            // Iterate over the variants
            ListEntry<Variant> i = head.next();
            while (i != null) {
                final ListEntry<Variant> i_next = i.next();
                // Get the dimension value of the varient to check
                final U d = dc.getDimension(i.value());
                
                if (d != null) {
                    // Check if the acceptable entry is compatable with
                    // the dimension value
                    if (dc.isCompatible(a, d)) {
                        // Update the quality factor
                        // assert(q >= a.getQuality())
                        q = a.getQuality();
                        // Move the compatible varient entry to occur after
                        // the head
                        head.insertAfter(i);
                        // Set the head to the compatible varient entry
                        // This ensures that compatible entries are only
                        // checked once
                        head = i;
                    }
                }
                i = i_next;
            }
        }
        // Order entries with null dimension values after those 
        // with acceptable dimension values
        ListEntry<Variant> i = head.next();
        while (i != null) {
            final ListEntry<Variant> i_next = i.next();
            if (dc.getDimension(i.value()) == null) {
                head.insertAfter(i);
                head = i;                
            }
            i = i_next;
        }        
        // Remove the unacceptable entries
        // Any such entries will always occur after the head entry
        head.removeTail();
    }
    
    public static Variant selectVariant(HttpRequestContext r, List<Variant> variants) {
        ListEntry<Variant> vs = ListEntry.create(variants);
        
        selectVariants(vs, HttpHelper.getAccept(r), MEDIA_TYPE_DC);
        selectVariants(vs, HttpHelper.getAcceptLangauge(r), LANGUAGE_TAG_DC);
        selectVariants(vs, HttpHelper.getAcceptCharset(r), CHARSET_DC);
        selectVariants(vs, HttpHelper.getAcceptEncoding(r), ENCODING_DC);
                
        return vs.next() != null ? vs.next().value() : null;        
    }
}
