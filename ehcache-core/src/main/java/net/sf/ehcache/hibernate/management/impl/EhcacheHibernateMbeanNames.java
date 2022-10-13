/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.hibernate.management.impl;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Utility class used for getting {@link ObjectName}'s for ehcache hibernate MBeans
 *
 * <p>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 *
 */
public abstract class EhcacheHibernateMbeanNames {

    /**
     * Filter out invalid ObjectName characters from s.
     *
     * @param s
     * @return A valid JMX ObjectName attribute value.
     */
    public static String mbeanSafe(String s) {
        return s == null ? "" : s.replaceAll(",|:|=|\n", ".");
      }
}
