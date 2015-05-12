/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.vsphere.loaders;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheLoader;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.logging.Logger;
import org.jclouds.vsphere.domain.VSphereServiceInstance;
import org.jclouds.vsphere.predicates.VSpherePredicate;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class VSphereTemplateLoader extends CacheLoader<String, Optional<VirtualMachine>> {
   @Resource
   protected Logger logger = Logger.NULL;

   private final Supplier<VSphereServiceInstance> serviceInstance;

   @Inject
   VSphereTemplateLoader(Supplier<VSphereServiceInstance> serviceInstance) {
      this.serviceInstance = checkNotNull(serviceInstance, "serviceInstance");
   }

   @Override
   public Optional<VirtualMachine> load(String vmName) {
      Optional<VirtualMachine> results = Optional.absent();
      try (VSphereServiceInstance instance = serviceInstance.get();) {
         VirtualMachine vm = (VirtualMachine) new InventoryNavigator(instance.getInstance().getRootFolder()).searchManagedEntity("VirtualMachine", vmName);
         if (VSpherePredicate.isTemplatePredicate.apply(vm)) {
            results = Optional.of(vm);
         }
      } catch (Exception e) {
         logger.error("Can't find template " + vmName, e);
         Throwables.propagateIfPossible(e);

      }

      return results;
   }
}
