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

package org.jclouds.vsphere.util;

import com.google.common.collect.Lists;
import org.jclouds.vsphere.compute.util.ListsUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 */
@Test(groups = "unit", testName = "VSpherePredicateTest")
public class ComputerNameValidatorTest {
   @Test(expectedExceptions = IllegalArgumentException.class)
   public void validateTooShortTest() {
      ComputerNameValidator.INSTANCE.validate("a");
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void validateTooLongTest() {
      ComputerNameValidator.INSTANCE.validate("asdfghjklzxcvbnmasdfghjkqwertyh");
   }

   public void validateOKTest() {
      ComputerNameValidator.INSTANCE.validate("good-name");
   }

   public void listToArrayTest() {
      List<List<String>> list = new ArrayList<List<String>>();

      list.add(Lists.newArrayList("fff", "ddd"));
      list.add(Lists.newArrayList("fff", "ddd1"));

      String[][] expected = new String[][]{ {"fff", "ddd"}, {"fff", "ddd1"}};


      Object[] strings = ListsUtils.ListToArray(list);

      Assert.assertEquals(strings, expected, "ListsUtils.ListToArray does not work.");

      //System.out.println(transformList.toArray() );
   }
}
