package org.onosproject.SdnCompetition;

/**
 * Created by janon on 4/19/16.
 */
public interface InstallRulesService {
    void install();
    void startValidatePath();
    void stopValidatePath();
    void restartValidatePath();
}