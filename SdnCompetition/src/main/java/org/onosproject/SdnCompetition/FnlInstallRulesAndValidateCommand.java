package org.onosproject.SdnCompetition;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Created by janon on 4/11/16.
 */
@Command(scope = "onos", name = "fnl-rules",
        description = "Install the static flow entry and validate the path for the 2016 SDN Competition T4")
public class FnlInstallRulesAndValidateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "userOptions", description = "i for install rules and v for validate the ValidatedPath",
            required = true, multiValued = false)
    String userOption = null;

    private InstallRulesService service;

    @Override
    protected void execute(){
        service = get(InstallRulesService.class);
        if (userOption.equals("install")) {
            service.install();
            return;
        } else if (userOption.equals("validate")) {
//            Ip4Address srcIp4Address = Ip4Address.valueOf(srcIp);
//            Ip4Address dstIpAddress = Ip4Address.valueOf(dstIp);
            service.startValidatePath();
            return;
        } else if (userOption.equals("revalidate")) {
            service.restartValidatePath();
        } else if (userOption.equals("stopvalidate")) {
            service.stopValidatePath();
        }
        else {
            System.out.print("command input error\n");
            return;
        }
    }
}