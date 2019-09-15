package org.usfirst.team1885.localizer.comms;

import com.google.ar.core.Pose;

/**
 * Created by Stephen Welch on 10/25/2017.
 */

public class SendablePose implements Sendable {

    private Pose pose;

    public SendablePose(Pose pose) {
        this.pose = pose;
    }

    @Override
    public String toJson() {
        return null;
    }

    public Pose getPose() {
        return pose;
    }
}
