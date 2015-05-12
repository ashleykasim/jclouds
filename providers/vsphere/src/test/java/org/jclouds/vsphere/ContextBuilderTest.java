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
package org.jclouds.vsphere;


import com.google.common.collect.ImmutableSet;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.jclouds.vsphere.compute.options.VSphereTemplateOptions;

import java.util.Properties;
import java.util.Set;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 2/23/14
 * Time: 12:58 PM
 * To change this template use File | Settings | File Templates.
 */

//@Test(groups = "unit", testName = "ContextBuilderTest")
public class ContextBuilderTest {
   public void testVSphereContext() throws RunNodesException {
      ImmutableSet modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule());
      ComputeServiceContext context = ContextBuilder.newBuilder("vsphere")
//                .credentials("root", "master1234")
//                .endpoint("https://10.56.161.100/sdk")
              .credentials("root", "vmware")
              .endpoint("https://10.45.7.70/sdk")
              .modules(modules)
              .buildView(ComputeServiceContext.class);

      TemplateBuilder b = context.getComputeService().templateBuilder();
      TemplateOptions o = context.getComputeService().templateOptions();
      ((VSphereTemplateOptions) o).isoFileName("ISO/UCSInstall_UCOS_3.1.0.0-9914.iso");
      ((VSphereTemplateOptions) o).flpFileName("ISO/image.flp");
      ((VSphereTemplateOptions) o).postConfiguration(false);
      o.tags(ImmutableSet.of("from UnitTest"))
              .nodeNames(ImmutableSet.of("first-vm12"))
              .networks("VLAN537");
//        b.imageId("Cisco Centos 6.5").smallest();
//        b.imageId("Cisco Centos 6.5.0").smallest().options(o);
      //b.imageId("Cisco Centos 6.5").locationId("default").smallest().options(o);
      b.imageId("conductor-mgt").locationId("default").minRam(6000).options(o);

      // Set images = context.getComputeService().listNodesByIds(ImmutableSet.of("junit-test-9b7"));
      Set<? extends NodeMetadata> nodes = context.getComputeService().createNodesInGroup("junit-test", 1, b.build());

      System.out.print("");
   }


   public void testOpenVmTools() throws Exception {
      ImmutableSet modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule());
      Properties p = new Properties();
      p.put("jclouds.vsphere.vm.password", "Nat0d12");
      ComputeServiceContext context = ContextBuilder.newBuilder("vsphere")
              .credentials("root", "vmware")
              .endpoint("https://10.63.120.120/sdk")

              .modules(modules)
              .overrides(p)
              .buildView(ComputeServiceContext.class);

      NodeMetadata nodeMetadata = context.getComputeService().getNodeMetadata("zenit-ccp0");
      System.out.print(nodeMetadata);
   }
   public void testPhenixVsphere() throws Exception {
      ImmutableSet modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule());
      Properties p = new Properties();
      p.put("jclouds.vsphere.vm.password", "Nat0d12");
      ComputeServiceContext context = ContextBuilder.newBuilder("vsphere")
              .credentials("root", "vmware")
              .endpoint("https://10.63.120.120/sdk")
//              .credentials("ci-scope.gen@prime", "c1-Scope")
//              .endpoint("https://vcte-vcenter.cisco.com/sdk")
              .modules(modules)
              .overrides(p)
              .buildView(ComputeServiceContext.class);

      TemplateBuilder b = context.getComputeService().templateBuilder();
      TemplateOptions o = context.getComputeService().templateOptions();
      ((VSphereTemplateOptions) o).postConfiguration(true);
      ((VSphereTemplateOptions) o).distributedVirtualSwitch(true);
      ((VSphereTemplateOptions) o).vmFolder("campnou");
      ((VSphereTemplateOptions) o).datacenterName("PHOENIX");

      o.tags(ImmutableSet.of("from UnitTest"))
              .nodeNames(ImmutableSet.of("first-vm12"))
              .networks("BVLAN_293");
//        b.imageId("Cisco Centos 6.5").smallest();
//        b.imageId("Cisco Centos 6.5.0").smallest().options(o);
      //b.imageId("Cisco Centos 6.5").locationId("default").smallest().options(o);
      b.imageId("SCOPE-TMP-DB").locationId("PHOENIX").minRam(10000).options(o);

      // Set images = context.getComputeService().listNodesByIds(ImmutableSet.of("junit-test-9b7"));
      Set<? extends NodeMetadata> nodes = context.getComputeService().createNodesInGroup("junit-test", 1, b.build());

//      ServiceInstance serviceInstance = new ServiceInstance(new URL("https://10.63.120.120/sdk"), "root", "vmware", true);
//
//      ManagedEntity entity = new InventoryNavigator(serviceInstance.getRootFolder()).searchManagedEntity("DistributedVirtualPortgroup", "VLAN_283");
//      ManagedEntity entity1 = new InventoryNavigator(serviceInstance.getRootFolder()).searchManagedEntity("Folder", "GADWALL");

      System.out.print("");
   }


//    public void  testVSphereApi() throws RunNodesException, IOException {
//        ImmutableSet modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule());
//        Injector injector = ContextBuilder.newBuilder("vsphere")
//                .credentials("root", "vmware")
//                .endpoint("https://10.45.7.70/sdk")
//                .modules(modules)
//                .buildInjector();
//
//        FileManagerApi fileManagerApi = injector.getInstance(FileManagerApi.class);
//        fileManagerApi.uploadFile("C:/Users/igreenfi/Desktop/image.flp", "test5/image.flp");
//
////        VSphereRestClient client = new VSphereRestClient("https://10.45.7.70","root","vmware");
////
////        client.putFile("test3/image.flp","CH","chesx3-datastore", new File("C:/Users/igreenfi/Desktop/image.flp"));
//
//        System.out.print("");
//    }
}
