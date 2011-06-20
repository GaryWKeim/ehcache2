/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.pool.sizeof;

import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;

/**
 * @author Alex Snaps
 */
final class ObjectGraphWalker {

  // Todo this is probably not what we want...
  private final ConcurrentMap<String, SoftReference<Collection<Field>>> fieldCache = new ConcurrentHashMap<String, SoftReference<Collection<Field>>>();

  private final SizeOfFilter sizeOfFilter;

  private final Visitor visitor;

  static interface Visitor {
    public long visit(Object object);
  }


  ObjectGraphWalker(Visitor visitor, SizeOfFilter filter) {
    this.visitor    = visitor;
    this.sizeOfFilter = filter;
  }

  long walk(Object... root) {

    long result = 0;

    Stack<Object>                   toVisit = new Stack<Object>();
    IdentityHashMap<Object, Object> visited = new IdentityHashMap<Object, Object>();

    if (root != null) {
      for (Object object : root) {
        nullSafeAdd(toVisit, object);
      }
    }

    while (!toVisit.isEmpty()) {

      Object ref = toVisit.pop();

      if (visited.containsKey(ref)) {
        continue;
      }

      Class<?> refClass = ref.getClass();
      if (sizeOfFilter.filterClass(refClass)) {
        if (refClass.isArray() && !refClass.getComponentType().isPrimitive()) {
          for (int i = 0; i < Array.getLength(ref); i++) {
            nullSafeAdd(toVisit, Array.get(ref, i));
          }
        } else {
          for (Field field : getFilteredFields(refClass)) {
            try {
              Object o = field.get(ref);
              if (!field.getType().isPrimitive()) {
                nullSafeAdd(toVisit, o);
              }
            } catch (IllegalAccessException ex) {
              throw new RuntimeException(ex);
            }
          }
        }

        result += visitor.visit(ref);
      }
      visited.put(ref, null);
    }

    return result;
  }

  Collection<Field> getFilteredFields(Class<?> refClass) {
    SoftReference<Collection<Field>> ref = fieldCache.get(refClass.getName());
    Collection<Field> fieldList = ref != null ? ref.get() : null;
    if (fieldList != null) {
      return fieldList;
    } else {
      Collection<Field> result = Collections.unmodifiableCollection(sizeOfFilter.filterFields(refClass, getAllFields(refClass)));
      fieldCache.put(refClass.getName(), new SoftReference<Collection<Field>>(result));
      return result;
    }
  }

  private void nullSafeAdd(final Stack<Object> toVisit, final Object o) {
    if (o != null) {
      toVisit.add(o);
    }
  }

  static Collection<Field> getAllFields(Class<?> refClass) {
    Collection<Field> fields = new ArrayList<Field>();
    for (Class<?> klazz = refClass; klazz != null; klazz = klazz.getSuperclass()) {
      for (Field field : klazz.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          field.setAccessible(true);
          fields.add(field);
        }
      }
    }
    return Collections.unmodifiableCollection(fields);
  }
}
