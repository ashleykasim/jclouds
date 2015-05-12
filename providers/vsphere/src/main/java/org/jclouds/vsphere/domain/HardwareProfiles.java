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

import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.HardwareBuilder;
import org.jclouds.compute.domain.Processor;
import org.jclouds.compute.domain.Volume;
import org.jclouds.compute.domain.VolumeBuilder;

/**
 */
public enum HardwareProfiles {
   C1_M1_D10(InstanceType.C1_M1_D10, "vSphere", InstanceType.C1_M1_D10, new Processor(1, 1.0), 1024, new VolumeBuilder().size(20f).type(Volume.Type.LOCAL).build()),
   C2_M2_D30(InstanceType.C2_M2_D30, "vSphere", InstanceType.C2_M2_D30, new Processor(2, 1.0), 2 * 1024, new VolumeBuilder().size(30f).type(Volume.Type.LOCAL).build()),
   C2_M2_D50(InstanceType.C2_M2_D50, "vSphere", InstanceType.C2_M2_D50, new Processor(2, 1.0), 2 * 1024, new VolumeBuilder().size(50f).type(Volume.Type.LOCAL).build()),
   C2_M4_D50(InstanceType.C2_M4_D50, "vSphere", InstanceType.C2_M4_D50, new Processor(2, 2.0), 4 * 1024, new VolumeBuilder().size(50f).type(Volume.Type.LOCAL).build()),
   C2_M10_D80(InstanceType.C2_M10_D80, "vSphere", InstanceType.C2_M10_D80, new Processor(2, 2.0), 10 * 1024, new VolumeBuilder().size(80f).type(Volume.Type.LOCAL).build()),
   C3_M10_D80(InstanceType.C3_M10_D80, "vSphere", InstanceType.C3_M10_D80, new Processor(3, 2.0), 10 * 1024, new VolumeBuilder().size(80f).type(Volume.Type.LOCAL).build()),
   C4_M4_D20(InstanceType.C4_M4_D20, "vSphere", InstanceType.C4_M4_D20, new Processor(4, 2.0), 4 * 1024, new VolumeBuilder().size(20f).type(Volume.Type.LOCAL).build()),
   C2_M6_D40(InstanceType.C2_M6_D40, "vSphere", InstanceType.C2_M6_D40, new Processor(2, 2.0), 6 * 1024, new VolumeBuilder().size(40f).type(Volume.Type.LOCAL).build()),
   C8_M16_D30(InstanceType.C8_M16_D30, "vSphere", InstanceType.C8_M16_D30, new Processor(8, 2.0), 16 * 1024, new VolumeBuilder().size(30f).type(Volume.Type.LOCAL).build()),
   C8_M16_D80(InstanceType.C8_M16_D80, "vSphere", InstanceType.C8_M16_D80, new Processor(8, 2.0), 16 * 1024, new VolumeBuilder().size(80f).type(Volume.Type.LOCAL).build());

   private final Hardware hardware;

   public Hardware getHardware() {
      return hardware;
   }

   private HardwareProfiles(String ids, String hypervisor, String name, Processor processor, int ram, Volume volume) {
      hardware = new HardwareBuilder().ids(ids).hypervisor(hypervisor).name(name)
              .processor(processor)
              .ram(ram)
              .volume(volume)
              .build();
   }
}
