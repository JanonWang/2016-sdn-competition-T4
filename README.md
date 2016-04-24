# 2016-sdn-competition-T4
SdnCompetition-T4-validatePath
Install static flow rules and validate the path. Using flowRulesService to apply rules in ONO0. Add a process to filter the ICMP packet, 
when the device recive an ICMP packet, it will packet in to the controller. Controller will read the deviceId from the packet and record 
it. Then delete the flow rules whose action is packet in, and then packet out it to the device table. 
