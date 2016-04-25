/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.SdnCompetition;

import org.apache.felix.scr.annotations.*;
import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Janon Wang on 2016/4/11
 */
@Component(immediate = true)
@Service
public class InstallRules implements InstallRulesService {

    private static final int DEFAULT_PRIORITY = 11;
    private static final int HIGH_PRIORITY = 12;
    private static final int TABLE1 = 1;
    private static final int TABLE0 = 0;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private ApplicationId appId;
    private PacketProcessor validateProcess = new ValidateProcess();
    private List<DeviceId> deviceIdList = new ArrayList<>();

    //host mac
    private static final String h1_mac = "00:00:00:00:00:01";
    private static final String h2_mac = "00:00:00:00:00:02";
    private static final String d1_mac = "00:00:00:00:00:03";
    private static final String d2_mac = "00:00:00:00:00:04";

    //DeviceId
    private static final DeviceId s1_id = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId s2_id = DeviceId.deviceId("of:0000000000000002");
    private static final DeviceId s3_id = DeviceId.deviceId("of:0000000000000003");
    private static final DeviceId s4_id = DeviceId.deviceId("of:0000000000000004");

    //port-positive
    private static final long s1s2 = 2L;
    private static final long s1s3 = 4L;
    private static final long s2s4 = 2L;
    private static final long s3s4 = 2L;
    private static final long s4d1 = 2L;
    private static final long s4d2 = 4L;
    //port-opposite
    private static final long s4s2 = 1L;
    private static final long s2s1 = 1L;
    private static final long s1h1 = 1L;
    private static final long s4s3 = 3L;
    private static final long s3s1 = 1L;
    private static final long s1h2 = 3L;

    private int count;


    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.SdnCompetition");
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        flowRuleService.removeFlowRulesById(appId);
        log.info("Stopped");
    }

    /**
     * when input fnl-rules install command, this function will run.
     */
    public void install() {
        //install the flow rules to trans ipv4 packet to Table1
        installTable0Rules();
        //Next is the flow rules to install on Table1
        //H1--->D1-positive
        installOneRulesTable1(h1_mac, d1_mac, s1_id, s1s2);
        installOneRulesTable1(h1_mac, d1_mac, s2_id, s2s4);
        installOneRulesTable1(h1_mac, d1_mac, s4_id, s4d1);
        //H1--->D1-opposite
        installOneRulesTable1(d1_mac, h1_mac, s4_id, s4s2);
        installOneRulesTable1(d1_mac, h1_mac, s2_id, s2s1);
        installOneRulesTable1(d1_mac, h1_mac, s1_id, s1h1);

        //H2--->D1-positive
        installOneRulesTable1(h2_mac, d1_mac, s1_id, s1s3);
        installOneRulesTable1(h2_mac, d1_mac, s3_id, s3s4);
        installOneRulesTable1(h2_mac, d1_mac, s4_id, s4d1);
        //H2--->D1-opposite
        installOneRulesTable1(d1_mac, h2_mac, s4_id, s4s3);
        installOneRulesTable1(d1_mac, h2_mac, s3_id, s3s1);
        installOneRulesTable1(d1_mac, h2_mac, s1_id, s1h2);

        //H1---D2-positive
        installOneRulesTable1(h1_mac, d2_mac, s1_id, s1s3);
        installOneRulesTable1(h1_mac, d2_mac, s3_id, s3s4);
        installOneRulesTable1(h1_mac, d2_mac, s4_id, s4d2);
        //H1---D2-opposite
        installOneRulesTable1(d2_mac, h1_mac, s4_id, s4s3);
        installOneRulesTable1(d2_mac, h1_mac, s3_id, s3s1);
        installOneRulesTable1(d2_mac, h1_mac, s1_id, s1h1);

        //H2--->D2-positive
        installOneRulesTable1(h2_mac, d2_mac, s1_id, s1s2);
        installOneRulesTable1(h2_mac, d2_mac, s2_id, s2s4);
        installOneRulesTable1(h2_mac, d2_mac, s4_id, s4d2);
        //H2--->D2-opposite
        installOneRulesTable1(d2_mac, h2_mac, s4_id, s4s2);
        installOneRulesTable1(d2_mac, h2_mac, s2_id, s2s1);
        installOneRulesTable1(d2_mac, h2_mac, s1_id, s1h2);
    }

    /**
     * when input fnl-rules validate command, this function will run.
     */
    public void startValidatePath() {
        //init the count number
        count = 1;
        //init the deviceId list
        deviceIdList.clear();
        //install the flow rules to trans the ICMP packet to controller
        installValidatedRules();
        //add an process in the controller to receive the ICMP packet
        packetService.addProcessor(validateProcess, PacketProcessor.director(2));
    }

    /**
     * when input fnl-rules revalidate command, this function will run.
     */
    public void restartValidatePath() {
        //init the count number
        count = 1;
        //init the deviceId list
        deviceIdList.clear();
        //install the flow rules to trans the ICMP packet to controller
        installValidatedRules();
    }

    /**
     * when input fnl-rules stopvalidate command, this function will run.
     */
    public void stopValidatePath() {
        packetService.removeProcessor(validateProcess);
    }

    /**
     * install the flow rules on Table0 on all device to transmit the ipv4 packet to table1.
     */
    private void installTable0Rules() {
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder().
                matchEthType(Ethernet.TYPE_IPV4).
                build();
        TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder().
                        add(Instructions.transition(TABLE1)).
                        build();
        //every device table0 will be installed this flowRules
        FlowRule flowRule1 = buildFlowRule(trafficTreatment, trafficSelector, s1_id, TABLE0, DEFAULT_PRIORITY);
        FlowRule flowRule2 = buildFlowRule(trafficTreatment, trafficSelector, s2_id, TABLE0, DEFAULT_PRIORITY);
        FlowRule flowRule3 = buildFlowRule(trafficTreatment, trafficSelector, s3_id, TABLE0, DEFAULT_PRIORITY);
        FlowRule flowRule4 = buildFlowRule(trafficTreatment, trafficSelector, s4_id, TABLE0, DEFAULT_PRIORITY);
        flowRuleService.applyFlowRules(flowRule1, flowRule2, flowRule3, flowRule4);
    }

    /**
     * instal the flow rules that transmit the packet to the next device.
     * @param srcMac the source mac address
     * @param dstMac the destination mac address
     * @param deviceid the device to install the flow rules
     * @param portNumber the port number on the device to transmit the packet out
     */
    private void installOneRulesTable1(String srcMac, String dstMac,
                                 DeviceId deviceid, long portNumber){
        TrafficSelector trafficSelector = buildTrafficSelector(srcMac, dstMac);
        TrafficTreatment trafficTreatment = buildTrafficTreatment(PortNumber.portNumber(portNumber));
        //the forwarding rules will be install in the table1
        FlowRule flowRule = buildFlowRule(trafficTreatment, trafficSelector, deviceid, TABLE1, DEFAULT_PRIORITY);
        flowRuleService.applyFlowRules(flowRule);
    }

    /**
     * Build a traffic selector with the param.
     * @param srcMac the source mac address
     * @param dstMac the destination mac address
     * @return the constructed traffic selector
     */
    private TrafficSelector buildTrafficSelector (String srcMac, String dstMac) {
        MacAddress matchSrcMac = MacAddress.valueOf(srcMac);
        MacAddress matchDstMac = MacAddress.valueOf(dstMac);
        return DefaultTrafficSelector.builder()
                .matchEthSrc(matchSrcMac)
                .matchEthDst(matchDstMac)
                .build();
    }

    /**
     * Build a traffic treatment with the param.
     * @param portNumber the port number transmit the packet out
     * @return the constructed traffic treatment
     */
    private TrafficTreatment buildTrafficTreatment (PortNumber portNumber) {
        return DefaultTrafficTreatment.builder()
                .setOutput(portNumber).build();
    }

    /**
     * install the flow rules that transmit the ICMP pakcet the controller on all the device.
     */
    private void installValidatedRules() {
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder().
                 matchEthType(Ethernet.TYPE_IPV4).
                 matchIPProtocol(IPv4.PROTOCOL_ICMP).
                 build();
        //when the vs receive the packet, it will notice the controller
        TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder().
                add(Instructions.createOutput(PortNumber.CONTROLLER)).
                build();
        //every device table0 will be installed this flowRules with high priority
        FlowRule flowRule1 = buildFlowRule(trafficTreatment, trafficSelector, s1_id, TABLE0, HIGH_PRIORITY);
        FlowRule flowRule2 = buildFlowRule(trafficTreatment, trafficSelector, s2_id, TABLE0, HIGH_PRIORITY);
        FlowRule flowRule3 = buildFlowRule(trafficTreatment, trafficSelector, s3_id, TABLE0, HIGH_PRIORITY);
        FlowRule flowRule4 = buildFlowRule(trafficTreatment, trafficSelector, s4_id, TABLE0, HIGH_PRIORITY);
        flowRuleService.applyFlowRules(flowRule1, flowRule2, flowRule3, flowRule4);
    }

    /**
     * remove the flow rules that transmit the ICMP pakcet the controller on the specific device.
     * @param deviceId the device Id to install the flow rules
     */
    private void removeValidateRule(DeviceId deviceId) {
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder().
                matchEthType(Ethernet.TYPE_IPV4).
                matchIPProtocol(IPv4.PROTOCOL_ICMP).
                build();
        TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder().
                add(Instructions.createOutput(PortNumber.CONTROLLER)).
                build();
        FlowRule flowRule = buildFlowRule(trafficTreatment, trafficSelector, deviceId, TABLE0, HIGH_PRIORITY);
        flowRuleService.removeFlowRules(flowRule);
    }

    /**
     * Build a flow rules with the param.
     * @param trafficTreatment traffic treatment with action
     * @param trafficSelector traffic selector to match the packet
     * @param deviceId the deivce to install the flow rules
     * @param tableId the table on the device to install the flow rules
     * @param priority flow rules priority, set default to 11, abocve the fwd module
     * @return the constructed flow rules
     */
    private FlowRule buildFlowRule(TrafficTreatment trafficTreatment, TrafficSelector trafficSelector,
                                   DeviceId deviceId, int tableId, int priority) {
        return  DefaultFlowRule.builder().
                fromApp(appId).
                withPriority(priority).
                forDevice(deviceId).
                forTable(tableId).
                withSelector(trafficSelector).
                withTreatment(trafficTreatment).
                //all the flow rules will be permanent
                makePermanent().
                build();
    }

    /**
     * The process to receive the packet and handle the packet
     */
    private  class ValidateProcess implements PacketProcessor {
        public void process(PacketContext packetContext) {
            Ethernet ethernet = packetContext.inPacket().parsed();
            //filter the ICMP packet
            if(isIcmpRequest(ethernet)) {
                recordPath(packetContext);
                //delete the flow rules  which send the packet to controller on the specific device
                DeviceId deviceId = packetContext.inPacket().receivedFrom().deviceId();
                removeValidateRule(deviceId);
                //send the packet back to teh device
                TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder().
                        add(Instructions.createOutput(PortNumber.TABLE)).
                        build();
                DefaultOutboundPacket defaultOutboundPacket = new DefaultOutboundPacket(deviceId, trafficTreatment,
                        packetContext.inPacket().unparsed());
                //packet out to the table
                packetService.emit(defaultOutboundPacket);
            }
        }

        /**
         * record the deviceId.
         * @param packetContext the packet send to the controller
         */
        private void recordPath(PacketContext packetContext) {
            DeviceId deviceId = packetContext.inPacket().receivedFrom().deviceId();
            Ethernet ethernet = packetContext.inPacket().parsed();
            MacAddress srcMac = ethernet.getSourceMAC();
            MacAddress dstMac = ethernet.getDestinationMAC();
            //FIXME --- it will record the same deviceId muilt times without deviceIdList
            if(!deviceIdList.contains(deviceId)) {
                String logResult = "The NO."+count+" switch on the path between "+srcMac.toString()+" and "
                        +dstMac.toString()+" is "+deviceId.toString();
                log.info(logResult);
                deviceIdList.add(deviceId);
                count++;
            }
        }

        /**
         * judge if it is an ICMP packet.
         * @param ethnet the ethernet frame
         */
        private boolean isIcmpRequest(Ethernet ethnet) {
            return ethnet.getEtherType() == Ethernet.TYPE_IPV4 &&
                    ((IPv4)ethnet.getPayload()).getProtocol() == IPv4.PROTOCOL_ICMP;
        }
    }
}
