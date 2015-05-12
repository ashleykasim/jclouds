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

package org.jclouds.vsphere.domain;

/**
 * The type of the instance.
 * C = cpu
 * M = memory (G)
 * D = disk (G)
 */
public class InstanceType {

   public static final String C1_M1_D10 = "C1_M1_D10";

   public static final String C2_M2_D30 = "C2_M2_D30";

   public static final String C2_M2_D50 = "C2_M2_D50";

   public static final String C2_M4_D50 = "C2_M4_D50";

   public static final String C2_M10_D80 = "C2_M10_D80";

   public static final String C3_M10_D80 = "C3_M10_D80";

   public static final String C4_M4_D20 = "C4.M4.D20";

   public static final String C2_M6_D40 = "C2.M6.D40";

   public static final String C8_M16_D30 = "C8_M16_D30";

   public static final String C8_M16_D80 = "C8_M16_D80";
}
