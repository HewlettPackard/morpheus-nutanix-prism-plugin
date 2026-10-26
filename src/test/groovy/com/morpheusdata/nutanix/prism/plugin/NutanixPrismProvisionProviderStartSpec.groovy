package com.morpheusdata.nutanix.prism.plugin

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.model.Cloud
import com.morpheusdata.model.ComputeServer
import com.morpheusdata.model.ComputeServerType
import com.morpheusdata.model.Workload
import com.morpheusdata.nutanix.prism.plugin.utils.NutanixPrismComputeUtility
import com.morpheusdata.response.ServiceResponse
import spock.lang.Specification

class NutanixPrismProvisionProviderStartSpec extends Specification {

	NutanixPrismProvisionProvider provider
	NutanixPrismPlugin plugin

	void setup() {
		plugin = Mock(NutanixPrismPlugin)
		plugin.getAuthConfig(_) >> [apiUrl: 'https://prism']
		provider = new NutanixPrismProvisionProvider(plugin, Mock(MorpheusContext))
	}

	private ComputeServer managedServer() {
		new ComputeServer(id: 1L, externalId: 'vm-uuid', managed: true, cloud: new Cloud(id: 2L),
			computeServerType: new ComputeServerType(controlPower: true))
	}

	void "startServer does not send power on to a VM that is already ON"() {
		given:
		GroovySpy(NutanixPrismComputeUtility, global: true)
		NutanixPrismComputeUtility.waitForPowerState(*_) >> [success: true, powerState: 'ON', data: [spec: [resources: [power_state: 'ON']]]]

		when:
		def response = provider.startServer(managedServer())

		then:
		0 * NutanixPrismComputeUtility.startVm(*_)
		response.success
	}

	void "startServer sends power on to a VM that is OFF"() {
		given:
		GroovySpy(NutanixPrismComputeUtility, global: true)
		NutanixPrismComputeUtility.waitForPowerState(*_) >> [success: true, powerState: 'OFF', data: [spec: [resources: [power_state: 'OFF']]]]

		when:
		def response = provider.startServer(managedServer())

		then:
		1 * NutanixPrismComputeUtility.startVm(*_) >> ServiceResponse.success([status: [execution_context: [task_uuid: 't1']]])
		1 * NutanixPrismComputeUtility.checkTaskReady(*_) >> ServiceResponse.success()
		response.success
	}

	void "startWorkload does not send power on to a VM that is already ON"() {
		given:
		GroovySpy(NutanixPrismComputeUtility, global: true)
		NutanixPrismComputeUtility.waitForPowerState(*_) >> [success: true, powerState: 'ON', data: [spec: [resources: [power_state: 'ON']]]]

		when:
		def response = provider.startWorkload(new Workload(server: managedServer()))

		then:
		0 * NutanixPrismComputeUtility.startVm(*_)
		response.success
	}
}
