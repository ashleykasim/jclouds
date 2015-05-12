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
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.vmware.vim25.mo.ServiceInstance;
import org.jclouds.compute.callables.RunScriptOnNode.Factory;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.Credentials;
import org.jclouds.location.Provider;
import org.jclouds.logging.Logger;
import org.jclouds.vsphere.domain.VSphereServiceInstance;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Singleton
public class CreateAndConnectVSphereClient implements Supplier<VSphereServiceInstance> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final Supplier<URI> providerSupplier;
   private transient Supplier<Credentials> credentials;

   @Inject
   public CreateAndConnectVSphereClient(Function<Supplier<NodeMetadata>, ServiceInstance> providerContextToCloud,
                                        Factory runScriptOnNodeFactory,
                                        @Provider Supplier<URI> providerSupplier,
                                        @Provider Supplier<Credentials> credentials) {

      this.credentials = checkNotNull(credentials, "credentials is needed");
      this.providerSupplier = checkNotNull(providerSupplier, "endpoint to vSphere node or vCenter server is needed");
   }

   public synchronized ServiceInstance start() {
      URI provider = providerSupplier.get();
      try {
         return new ServiceInstance(new URL(provider.toASCIIString()), credentials.get().identity, credentials.get().credential, true);
      } catch (RemoteException e) {
         throw Throwables.propagate(e);
      } catch (MalformedURLException e) {
         throw Throwables.propagate(e);
      }
   }

   @Override
   public VSphereServiceInstance get() {
      ServiceInstance client = start();
      checkState(client != null, "(CreateAndConnectVSphereClient) start not called");
      return new VSphereServiceInstance(client);
   }

}
