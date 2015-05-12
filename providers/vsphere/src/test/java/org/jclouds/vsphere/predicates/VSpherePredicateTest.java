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

package org.jclouds.vsphere.predicates;

import com.vmware.vim25.mo.VirtualMachine;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * Date: 26/06/2014 9:18 AM
 * Package: org.jclouds.vsphere.predicates
 */
@Test(groups = "unit", testName = "VSpherePredicateTest")
public class VSpherePredicateTest {
   public void isTemplatePredicateTest() {
      Assert.assertTrue(VSpherePredicate.isTemplatePredicate.apply(new VirtualMachine(null, null)));
   }

   public void isInet4AddressTest() {
      Assert.assertTrue(VSpherePredicate.isInet4Address.apply("10.45.37.3"));
      Assert.assertTrue(!VSpherePredicate.isInet4Address.apply("fd7f:628d:cd1e:1:499e:4dbc:ca33:13cf"));
   }

   public void isInet6AddressTest() {
      Assert.assertTrue(!VSpherePredicate.isInet6Address.apply("10.45.37.3"));
      Assert.assertTrue(VSpherePredicate.isInet6Address.apply("fd7f:628d:cd1e:1:499e:4dbc:ca33:13cf"));
   }

   public void wait_for_nicTest() {
      long start = System.currentTimeMillis();
      Assert.assertTrue(!VSpherePredicate.WAIT_FOR_NIC(1000 * 10, TimeUnit.MILLISECONDS).apply(new VirtualMachine(null, null)));
      Assert.assertTrue(System.currentTimeMillis() - start > 1000 * 9);
   }

   public void wait_for_vmtoolsTest() {
      long start = System.currentTimeMillis();
      Assert.assertTrue(!VSpherePredicate.WAIT_FOR_VMTOOLS(1000 * 2, TimeUnit.MILLISECONDS).apply(new VirtualMachine(null, null)));
      Assert.assertTrue(System.currentTimeMillis() - start > 1000);
   }


}
