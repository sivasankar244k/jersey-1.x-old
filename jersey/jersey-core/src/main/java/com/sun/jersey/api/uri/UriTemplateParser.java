/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package com.sun.jersey.api.uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A URI template parser that parses JAX-RS specifide URI templates.
 * 
 * @author Paul.Sandoz@Sun.Com
 */
public class UriTemplateParser {
    private static Set<Character> RESERVED_REGEX_CHARACTERS = createReserved();

    private static Set<Character> createReserved() {
        // TODO need to escape all regex characters present
        char[] reserved = {
            '.',
            '?',
            '(', 
            ')'};

        Set<Character> s = new HashSet<Character>(reserved.length);
        for (char c : reserved) s.add(c);
        return s;
    }        

    private static final Pattern TEMPLATE_VALUE_PATTERN = Pattern.compile("[^/]+?");

    private interface CharacterIterator {
        boolean hasNext();
        char next();
        char peek();
        int pos();
    }

    private static final class StringCharacterIterator implements CharacterIterator {
        int pos;
        String s;

        public StringCharacterIterator(String s) {
            this.s = s;
        }

        public boolean hasNext() {
            return pos < s.length();
        }

        public char next() {
            if (!hasNext())
                throw new NoSuchElementException();
            return s.charAt(pos++);
        }

        public char peek() {
            if (!hasNext())
                throw new NoSuchElementException();

            return s.charAt(pos++);
        }

        public int pos() {
            if (pos == 0) return 0;
            return pos - 1;
        }

    }

    private final String template;
    
    private final StringBuffer regex = new StringBuffer();;

    private final StringBuffer normalizedTemplate = new StringBuffer();;

    private final StringBuffer literalCharactersBuffer = new StringBuffer();;

    private int literalCharacters;

    private final Pattern pattern;

    private final List<String> names = new ArrayList<String>();

    private final Map<String, Pattern> nameToPattern = new HashMap<String, Pattern>();

    public UriTemplateParser(String template) {
        if (template == null || template.length() == 0)
            throw new IllegalArgumentException();

        this.template = template;
        parse(new StringCharacterIterator(template));
        try {
            pattern = Pattern.compile(regex.toString());
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid syntax for the template expression '" + 
                    regex + "'", 
                    ex);            
        }
    }

    public final String getTemplate() {
        return template;
    }

    public final Pattern getPattern() {
        return pattern;
    }

    public final String getNormalizedTemplate() {
        return normalizedTemplate.toString();
    }

    public final Map<String, Pattern> getNameToPattern() {
        return nameToPattern;
    }

    public final List<String> getNames() {
        return names;
    }

    public final int getNumberOfLiteralCharacters() {
        return literalCharacters;
    }

    /**
     * Encode literal characters of a template.
     * 
     * @param literalCharacters the literal characters
     * @return the encoded literal characters.
     */
    protected String encodeLiteralCharacters(String literalCharacters) {
        return literalCharacters;
    }
    
    private void parse(CharacterIterator ci) {
        while (ci.hasNext()) {
            char c = ci.next();
            if (c == '{') {
                processLiteralCharacters();
                parseName(ci);
            } else {
                literalCharactersBuffer.append(c);
            }
        }
        processLiteralCharacters();
    }

    private void processLiteralCharacters() {
        if (literalCharactersBuffer.length() > 0) {
            literalCharacters += literalCharactersBuffer.length();

            String s = encodeLiteralCharacters(literalCharactersBuffer.toString());

            normalizedTemplate.append(s);

            // Escape if reserved regex character
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (RESERVED_REGEX_CHARACTERS.contains(c))
                    regex.append("\\");
                regex.append(c);
            }

            literalCharactersBuffer.setLength(0);
        }
    }

    private void parseName(CharacterIterator ci) {
        char c = consumeWhiteSpace(ci);

        StringBuffer nameBuffer = new StringBuffer();        
        if (Character.isLetterOrDigit(c) || c == '_') {
            // Template name character
            nameBuffer.append(c);
        } else {
            throw new IllegalArgumentException("Illegal character '" + c + 
                    "' at position " + ci.pos() + " is not as the start of a name");
        }

        String nameRegexString = "";
        while(true) {
            c = ci.next();
            // "\\{(\\w[-\\w\\.]*)
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                // Template name character             
                nameBuffer.append(c);
            } else if (c == ':') {
                nameRegexString = parseRegex(ci);
                break;
            } else if (c == '}') {
                break;
            } else if (c == ' ') {
                c = consumeWhiteSpace(ci);

                if (c == ':') {
                    nameRegexString = parseRegex(ci);
                    break;
                } else if (c == '}') {
                    break;
                } else {
                    // Error
                    throw new IllegalArgumentException("Illegal character '" + c + 
                            "' at position " + ci.pos() + " is not allowed after a name");
                }
            } else {
                throw new IllegalArgumentException("Illegal character '" + c + 
                        "' at position " + ci.pos() + " is not allowed as part of a name");
            }
        }        
        String name = nameBuffer.toString();
        names.add(name);

        try {
            Pattern namePattern = (nameRegexString.length() == 0) 
                    ? TEMPLATE_VALUE_PATTERN : Pattern.compile(nameRegexString);
            if (nameToPattern.containsKey(name)) {
                if (!nameToPattern.get(name).equals(namePattern)) {
                    throw new IllegalArgumentException("The name '" + name + 
                            "' is declared " +
                            "more than once with different regular expressions");
                }
            } else {
                nameToPattern.put(name, namePattern);            
            }

            regex.append('(').
                    append(namePattern).
                    append(')');
            normalizedTemplate.append('{').
                    append(name).
                    append('}');
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid syntax for the expression '" + nameRegexString + 
                    "' associated with the name '" + name + "'", 
                    ex);
        }
    }

    private String parseRegex(CharacterIterator ci) {
        StringBuffer regexBuffer = new StringBuffer();

        int braceCount = 1;
        while (true) {
            char c = ci.next();
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0)
                    break;
            }            
            regexBuffer.append(c);
        }

        return regexBuffer.toString().trim();
    }

    private char consumeWhiteSpace(CharacterIterator ci) {
        char c = ci.next();
        // Consume white space;
        // TODO use correct c
        while (c == ' ') c = ci.next();

        return c;
    }
}