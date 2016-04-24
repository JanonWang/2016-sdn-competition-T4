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

import java.nio.ByteBuffer;

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
    //private static final short vlanId = 666;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected HostService hostService;

    private ApplicationId appId;
    private PacketProcessor validateProcess = new ValidateProcess();
    //private ValidatedPath validatedPath = new ValidatedPath();

    //host ip
//    private static final String h1_ip = "192.168.0.1/24";
//    private static final String h2_ip = "192.168.0.2/24";
//    private static final String d1_ip = "192.168.0.3/24";
//    private static final String d2_ip = "192.168.0.4/24";

    //host mac
    private static final String h1_mac = "00:00:00:00:00:01";
    private static final String h2_mac = "00:00:00:00:00:02";
    private static final String d1_mac = "00:00:00:00:00:03";
    private static final String d2_mac = "00:00:00:00:00:04";

    //host ip without mask
//    private static final String h1_ip_s = "192.168.0.1";
//    private static final String h2_ip_s = "192.168.0.2";
//    private static final String d1_ip_s = "192.168.0.3";
//    private static final String d2_ip_s = "192.168.0.4";

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
//        install();
//        installValidatedRules();
//        packetService.addProcessor(validateProcess, PacketProcessor.director(2));
        appId = coreService.registerApplication("org.onosproject.SdnCompetition");
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        flowRuleService.removeFlowRulesById(appId);
//        packetService.removeProcessor(validateProcess);
        log.info("Stopped");
    }
    //install the static flow rules
    public void install() {
        //install the rules to trans ICMP packet to Table1
        installTable0Rules();
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
        installOneRulesTable1(d2_mac, h1_mac, s1_id, s1h2);

        //H2--->D2-positive
        installOneRulesTable1(h2_mac, d2_mac, s1_id, s1s2);
        installOneRulesTable1(h2_mac, d2_mac, s2_id, s2s4);
        installOneRulesTable1(h2_mac, d2_mac, s4_id, s4d2);
        //H2--->D2-opposite
        installOneRulesTable1(d2_mac, h2_mac, s4_id, s4s2);
        installOneRulesTable1(d2_mac, h2_mac, s2_id, s2s1);
        installOneRulesTable1(d2_mac, h2_mac, s1_id, s1h2);
        return;
    }

    public void startValidatePath() {
        //init the count number
        count = 1;
        //install the ping packet to validate the path
        installValidatedRules();
        //validatedPath.clear();
//        srcIp4Address = srcIp;
//        dstIp4Address = dstIp;
        //add a process to handle the packet_in msg
        packetService.addProcessor(validateProcess, PacketProcessor.director(2));
    }

    public void restartValidatePath() {
        //init the count number
        count = 1;
        installValidatedRules();
    }

    public void stopValidatePath() {
        //cover the validatedRules to stop trans the ICMP packet to controller
        //installTable0Rules();
        packetService.removeProcessor(validateProcess);
    }



    private void installTable0Rules() {
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder().
                matchEthType(Ethernet.TYPE_IPV4).
                //matchIPProtocol(IPv4.PROTOCOL_ICMP).
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
        return;
    }

    private void installOneRulesTable1(String srcMac, String dstMac,
                                 DeviceId deviceid, long portNumber){
        TrafficSelector trafficSelector = buildTrafficSelector(srcMac, dstMac);
        TrafficTreatment trafficTreatment = buildTrafficTreatment(PortNumber.portNumber(portNumber));
        //the forwarding rules will be install in the table1
        FlowRule flowRule = buildFlowRule(trafficTreatment, trafficSelector, deviceid, TABLE1, DEFAULT_PRIORITY);
        flowRuleService.applyFlowRules(flowRule);
//        ForwardingObjective forwardingObjective = buildForwardingObjective(trafficTreatment, trafficSelector);
//        flowObjectiveService.forward(deviceid, forwardingObjective);
        return;
    }

    private TrafficSelector buildTrafficSelector (String srcMac, String dstMac) {
//        Ip4Prefix matchIp4SrcPrefix = Ip4Prefix.valueOf(srcIp);
//        Ip4Prefix matchIp4DstPrefix = Ip4Prefix.valueOf(dstIp);
        MacAddress matchSrcMac = MacAddress.valueOf(srcMac);
        MacAddress matchDstMac = MacAddress.valueOf(dstMac);
        TrafficSelector trafficelector = DefaultTrafficSelector.builder()
                .matchEthSrc(matchSrcMac)
                .matchEthDst(matchDstMac)
//                .matchIPSrc(matchIp4SrcPrefix)
//                .matchIPDst(matchIp4DstPrefix)
                .build();
        return trafficelector;
    }

    private TrafficTreatment buildTrafficTreatment (PortNumber portNumber) {
        TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber).build();
        return trafficTreatment;
    }

//    private ForwardingObjective buildForwardingObjective (TrafficTreatment trafficTreatment,
//                                                          TrafficSelector trafficSelector) {
//        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
//                .withPriority(DEFAULT_PRIORITY)
//                .withFlag(ForwardingObjective.Flag.VERSATILE)
//                .fromApp(appId)
//                .makePermanent()
//                .withSelector(trafficSelector)
//                .withTreatment(trafficTreatment)
//                .add();
//        return forwardingObjective;
//    }









    private void installValidatedRules() {
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder().
                 matchEthType(Ethernet.TYPE_IPV4).
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
        //every device table0 will be installed this flowRules with default priority
//        TrafficSelector trafficSelector1 = DefaultTrafficSelector.builder().
//                matchEthType(Ethernet.TYPE_IPV4).
//                build();
//        TrafficTreatment trafficTreatment1 = DefaultTrafficTreatment.builder().
//                immediate().
//                add(Instructions.popVlan()).
//                add(Instructions.transition(TABLE1)).
//                build();
//        FlowRule flowRule5 = buildFlowRule(trafficTreatment1, trafficSelector1, s1_id, TABLE0, DEFAULT_PRIORITY);
//        FlowRule flowRule6 = buildFlowRule(trafficTreatment1, trafficSelector1, s2_id, TABLE0, DEFAULT_PRIORITY);
//        FlowRule flowRule7 = buildFlowRule(trafficTreatment1, trafficSelector1, s3_id, TABLE0, DEFAULT_PRIORITY);
//        FlowRule flowRule8 = buildFlowRule(trafficTreatment1, trafficSelector1, s4_id, TABLE0, DEFAULT_PRIORITY);
        //apply the flow rules
        flowRuleService.applyFlowRules(flowRule1, flowRule2, flowRule3, flowRule4);
        return;
    }

    private void removeValidateRule(DeviceId deviceId) {
        TrafficSelector trafficSelector = DefaultTrafficSelector.builder().
                matchEthType(Ethernet.TYPE_IPV4).
                build();
        //when the vs receive the packet, it will notice the controller
        TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder().
                add(Instructions.createOutput(PortNumber.CONTROLLER)).
                build();
        FlowRule flowRule = buildFlowRule(trafficTreatment, trafficSelector, deviceId, TABLE0, HIGH_PRIORITY);
        flowRuleService.removeFlowRules(flowRule);
    }

    private FlowRule buildFlowRule(TrafficTreatment trafficTreatment, TrafficSelector trafficSelector,
                                   DeviceId deviceId, int tableId, int priority) {
        FlowRule flowRule = DefaultFlowRule.builder().
                fromApp(appId).
                withPriority(priority).
                forDevice(deviceId).
                forTable(tableId).
                withSelector(trafficSelector).
                withTreatment(trafficTreatment).
                //all the flow rules will be permanent
                makePermanent().
                build();
        return flowRule;
    }




    private  class ValidateProcess implements PacketProcessor {
        public void process(PacketContext packetContext) {
            Ethernet ethernet = packetContext.inPacket().parsed();
            if(isIcmpRequest(ethernet)) {//use ping packet to validate the path
                recordPath(packetContext);
                //delete the flow which send the packet to controller
                DeviceId deviceId = packetContext.inPacket().receivedFrom().deviceId();
                removeValidateRule(deviceId);
                //send the packet back
                TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder().
                        add(Instructions.createOutput(PortNumber.TABLE)).
                        build();
                //push an vlanId on the OutboundPacket to avoid matching the "to-controller" rules
                //Ethernet ethWithVlanId = packetContext.inPacket().parsed().setVlanID(vlanId);
                DefaultOutboundPacket defaultOutboundPacket = new DefaultOutboundPacket(deviceId, trafficTreatment,
                        packetContext.inPacket().unparsed());
                //packet out to the table
                packetService.emit(defaultOutboundPacket);
            } else {
                /**
                 * TODO--if the packet is not a ICMP packet,how to handle it.
                 * Now the normal packet will be block when pwd is closed and
                 * validatePath function is on
                 */
            }
        }
    }

    private boolean isIcmpRequest(Ethernet ethnet) {
        return ethnet.getEtherType() == Ethernet.TYPE_IPV4 &&
                ((IPv4)ethnet.getPayload()).getProtocol() == IPv4.PROTOCOL_ICMP;
    }

    private void recordPath(PacketContext packetContext) {
        DeviceId deviceId = packetContext.inPacket().receivedFrom().deviceId();
        Ethernet ethernet = packetContext.inPacket().parsed();
        MacAddress srcMac = ethernet.getSourceMAC();
        MacAddress dstMac = ethernet.getDestinationMAC();
//        Ip4Address srcIp = Ip4Address.valueOf(((IPv4)ethernet.getPayload()).getSourceAddress());
//        Ip4Address dstIp = Ip4Address.valueOf(((IPv4)ethernet.getPayload()).getDestinationAddress());

        String logResult = "The NO."+count+" switch on the path between "+srcMac.toString()+" and "
                    +dstMac.toString()+" is "+deviceId.toString();
        count++;
        log.info(logResult);
    }



























}