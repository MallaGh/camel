/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.converter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter from String syntax to milli seconds.
 * Code is copied to org.apache.camel.catalog.TimePatternConverter in camel-catalog
 */
@Converter
public final class TimePatternConverter {   
    private static final Logger LOG = LoggerFactory.getLogger(TimePatternConverter.class);
    private static final String NUMBERS_ONLY_STRING_PATTERN = "^[-]?(\\d)+$";
    private static final String HOUR_REGEX_PATTERN = "((\\d)*(\\d))h(our(s)?)?";
    private static final String MINUTES_REGEX_PATTERN = "((\\d)*(\\d))m(in(ute(s)?)?)?";
    private static final String SECONDS_REGEX_PATTERN = "((\\d)*(\\d))s(ec(ond)?(s)?)?";

    /**
     * Utility classes should not have a public constructor.
     */
    private TimePatternConverter() {
    }
    
    @Converter
    public static long toMilliSeconds(String source) throws IllegalArgumentException {
        long milliseconds = 0;
        boolean foundFlag = false;

        checkCorrectnessOfPattern(source);
        Matcher matcher;

        matcher = createMatcher(NUMBERS_ONLY_STRING_PATTERN, source);
        if (matcher.find()) {
            // Note: This will also be used for regular numeric strings. 
            //       This String -> long converter will be used for all strings.
            milliseconds = Long.valueOf(source);
        } else {            
            matcher = createMatcher(HOUR_REGEX_PATTERN, source);
            if (matcher.find()) {
                milliseconds = milliseconds + (3600000 * Long.valueOf(matcher.group(1)));
                foundFlag = true;
            }
            
            matcher = createMatcher(MINUTES_REGEX_PATTERN, source);
            if (matcher.find()) {
                long minutes = Long.valueOf(matcher.group(1));
                if ((minutes > 59) && foundFlag) {
                    throw new IllegalArgumentException("Minutes should contain a valid value between 0 and 59: " + source);
                }
                foundFlag = true;
                milliseconds = milliseconds + (60000 * minutes);
            }
               
            matcher = createMatcher(SECONDS_REGEX_PATTERN, source);
            if (matcher.find()) {
                long seconds = Long.valueOf(matcher.group(1));
                if ((seconds > 59) && foundFlag) {
                    throw new IllegalArgumentException("Seconds should contain a valid value between 0 and 59: " + source);
                }
                foundFlag = true;
                milliseconds = milliseconds + (1000 * seconds);
            }      
            
            // No pattern matched... initiating fallback check and conversion (if required). 
            // The source at this point may contain illegal values or special characters 
            if (!foundFlag) {
                milliseconds = Long.valueOf(source);
            }
        }       
        
        LOG.trace("source: {} milliseconds: ", source, milliseconds);
        
        return milliseconds;
    }

    private static void checkCorrectnessOfPattern(String source) {
        //replace only numbers once
        Matcher matcher = createMatcher(NUMBERS_ONLY_STRING_PATTERN, source);
        String replaceSource = matcher.replaceFirst("");

        //replace hour string once
        matcher = createMatcher(HOUR_REGEX_PATTERN, replaceSource);
        if (matcher.find() && matcher.find()) {
            throw new IllegalArgumentException("Hours should not be specified more then once: " + source);
        }
        replaceSource = matcher.replaceFirst("");

        //replace minutes once
        matcher = createMatcher(MINUTES_REGEX_PATTERN, replaceSource);
        if (matcher.find() && matcher.find()) {
            throw new IllegalArgumentException("Minutes should not be specified more then once: " + source);
        }
        replaceSource = matcher.replaceFirst("");

        //replace seconds once
        matcher = createMatcher(SECONDS_REGEX_PATTERN, replaceSource);
        if (matcher.find() && matcher.find()) {
            throw new IllegalArgumentException("Seconds should not be specified more then once: " + source);
        }
        replaceSource = matcher.replaceFirst("");

        if (replaceSource.length() > 0) {
            throw new IllegalArgumentException("Illegal characters: " + source);
        }
    }

    private static Matcher createMatcher(String regexPattern, String source) {
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(source);        
    }    
}
