/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.aws.deploy.ops

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing as AmazonElasticLoadBalancingV2
import com.netflix.spinnaker.clouddriver.aws.TestCredential
import com.netflix.spinnaker.clouddriver.aws.deploy.description.EnableDisableAsgDescription
import com.netflix.spinnaker.clouddriver.aws.deploy.ops.discovery.AwsEurekaSupport
import com.netflix.spinnaker.clouddriver.eureka.api.Eureka
import com.netflix.spinnaker.clouddriver.aws.services.AsgService
import com.netflix.spinnaker.clouddriver.aws.services.RegionScopedProviderFactory
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import spock.lang.Shared
import spock.lang.Specification

abstract class EnableDisableAtomicOperationUnitSpecSupport extends Specification {
  @Shared
  def description = new EnableDisableAsgDescription([
      asgs       : [[
        serverGroupName: "kato-main-v000",
        region         : "us-west-1"
      ]],
      credentials: TestCredential.named('foo')
  ])

  @Shared
  AbstractEnableDisableAtomicOperation op

  @Shared
  Task task

  @Shared
  AsgService asgService

  @Shared
  Eureka eureka

  @Shared
  AmazonElasticLoadBalancing loadBalancing

  @Shared
  AmazonElasticLoadBalancingV2 loadBalancingV2

  def setup() {
    task = Mock(Task)
    TaskRepository.threadLocalTask.set(task)
    eureka = Mock(Eureka)
    asgService = Mock(AsgService)
    loadBalancing = Mock(AmazonElasticLoadBalancing)
    loadBalancingV2 = Mock(AmazonElasticLoadBalancingV2)
    wireOpMocks(op)
  }

  def wireOpMocks(AbstractEnableDisableAtomicOperation op) {
    def regionScopedProviderFactory = Stub(RegionScopedProviderFactory) {
      forRegion(_, _) >> {
        return Stub(RegionScopedProviderFactory.RegionScopedProvider) {
          getAsgService() >> asgService
          getAmazonElasticLoadBalancing() >> loadBalancing
          getAmazonElasticLoadBalancingV2() >> loadBalancingV2
          getEureka() >> eureka
        }
      }
    }

    op.discoverySupport = new AwsEurekaSupport(
      regionScopedProviderFactory: regionScopedProviderFactory
    )
    op.discoverySupport.metaClass.verifyInstanceAndAsgExist = {
      AmazonEC2 amazonEC2, AsgService asgService, String instanceId, String asgName -> true
    }

    op.regionScopedProviderFactory = regionScopedProviderFactory
  }
}
