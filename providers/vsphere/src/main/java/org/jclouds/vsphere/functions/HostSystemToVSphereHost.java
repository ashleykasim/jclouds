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

package org.jclouds.vsphere.functions;

import com.google.common.base.Function;
import com.vmware.vim25.mo.HostSystem;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.vsphere.domain.VSphereHost;
import org.jclouds.vsphere.domain.VSphereServiceInstance;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HostSystemToVSphereHost implements Function<HostSystem, VSphereHost> {


   @Inject
   public HostSystemToVSphereHost() {
   }

   @Override
   public VSphereHost apply(@Nullable HostSystem from) {
      if (from == null)
         return null;
      return new VSphereHost(from, new VSphereServiceInstance(from.getServerConnection().getServiceInstance()));
   }

}
