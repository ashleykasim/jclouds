/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.jclouds.lifecycle;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import com.google.common.collect.Lists;

/**
 * This will close objects in the reverse order that they were added.
 * 
 * @author Adrian Cole
 */
@Singleton
public class Closer implements Closeable {
   // guice is single threaded. no need to lock this
   List<Closeable> methodsToClose = Lists.<Closeable> newArrayList();

   public void addToClose(Closeable toClose) {
      methodsToClose.add(toClose);
   }

   public void close() throws IOException {
      Collections.reverse(methodsToClose);
      for (Closeable toClose : methodsToClose) {
         toClose.close();
      }
   }
}
