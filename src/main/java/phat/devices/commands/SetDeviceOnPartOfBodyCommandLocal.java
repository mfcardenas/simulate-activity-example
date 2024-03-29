/*
 * Copyright (C) 2014 Pablo Campillo-Sanchez <pabcampi@ucm.es>
 *
 * This software has been developed as part of the
 * SociAAL project directed by Jorge J. Gomez Sanz
 * (http://grasia.fdi.ucm.es/sociaal)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package phat.devices.commands;


import com.jme3.animation.SkeletonControl;
import com.jme3.app.Application;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import phat.agents.actors.ActorFactory;
import phat.body.BodiesAppState;
import phat.commands.PHATCommParam;
import phat.commands.PHATCommandAnn;
import phat.commands.PHATCommandListener;
import phat.devices.DevicesAppState;

import java.util.logging.Level;

/**
 * @author UCM
 */
@PHATCommandAnn(name = "SetDeviceOnBody", type = "device", debug = false)
public class SetDeviceOnPartOfBodyCommandLocal extends PHATDeviceCommand  {

    public enum PartOfBody {
        LeftHand, RightHand, LeftWrist, RightWrist, LeftUnkle, RightUnkle, Head, Chest, RightLeg, LeftLeg
    }
    String bodyId;
    String deviceId;
    PartOfBody partOfBody;

    public SetDeviceOnPartOfBodyCommandLocal() {
    }

    public SetDeviceOnPartOfBodyCommandLocal(String bodyId, String deviceId, PartOfBody partOfBody, PHATCommandListener listener) {
        super(listener);
        this.bodyId = bodyId;
        this.deviceId = deviceId;
        this.partOfBody = partOfBody;
        logger.log(Level.INFO, "New Command: {0}", new Object[]{this});
    }

    public SetDeviceOnPartOfBodyCommandLocal(String bodyId, String deviceId, PartOfBody partOfBody) {
        this(bodyId, deviceId, partOfBody, null);
    }

    @Override
    public void runCommand(Application app) {
        BodiesAppState bodiesAppState = app.getStateManager().getState(BodiesAppState.class);
        DevicesAppState devicesAppState = app.getStateManager().getState(DevicesAppState.class);

        Node body = bodiesAppState.getBody(bodyId);

        if (body != null && body.getParent() != null) {
            Spatial s = devicesAppState.getDevice(deviceId);
            if (s != null) {
                if (attachDevice(body, s)) {
                    setState(State.Success);
                    return;
                }
            }
        }
        setState(State.Fail);
    }

    @Override
    public void interruptCommand(Application app) {
        setState(State.Fail);
    }

    private boolean attachDevice(Spatial body, Spatial obj) {
        RigidBodyControl rbc = obj.getControl(RigidBodyControl.class);
        if (rbc != null) {
            rbc.setEnabled(false);
        }
        SkeletonControl sc = ActorFactory.findControl(body, SkeletonControl.class);
        Node attachmentsNode = null;
        if (partOfBody == PartOfBody.LeftHand) {
            obj.setLocalTranslation(0, 0.05f, 0.02f);
            Quaternion q = new Quaternion();
            q.fromAngles(0f, 0f, 90f);
            obj.setLocalRotation(q);
            attachmentsNode = sc.getAttachmentsNode("LThumb");
        } else if (partOfBody == PartOfBody.RightHand) {
            obj.setLocalTranslation(0, 0.05f, 0.02f);
            Quaternion q = new Quaternion();
            q.fromAngles(0f, 0f, 90f);
            obj.setLocalRotation(q);
            attachmentsNode = sc.getAttachmentsNode("RThumb");
        } else if (partOfBody == PartOfBody.RightWrist) {
            obj.setLocalTranslation(0, 0.0f, 0.02f);
            attachmentsNode = sc.getAttachmentsNode("RightHand");
        } else if (partOfBody == PartOfBody.LeftWrist) {
            obj.setLocalTranslation(0, 0.0f, 0.02f);
            attachmentsNode = sc.getAttachmentsNode("LeftHand");
        } else if (partOfBody == PartOfBody.LeftUnkle) {
            obj.setLocalRotation(new Quaternion().fromAngles(0f, FastMath.HALF_PI, -FastMath.HALF_PI));
            obj.setLocalTranslation(0.04f, 0f, -0.1f);
            attachmentsNode = sc.getAttachmentsNode("LeftFoot");
        } else if (partOfBody == PartOfBody.RightUnkle) {
            obj.setLocalRotation(new Quaternion().fromAngles(0f, FastMath.HALF_PI, -FastMath.HALF_PI));
            obj.setLocalTranslation(-0.04f, 0f, -0.1f);
            attachmentsNode = sc.getAttachmentsNode("RightFoot");
        } else if (partOfBody == PartOfBody.Chest) {
            obj.setLocalTranslation(0f, 0.1f, 0.2f);
            attachmentsNode = sc.getAttachmentsNode("Spine1");
        } else if (partOfBody == PartOfBody.RightLeg){
            obj.setLocalTranslation(0f, 0.1f, 0.3f);
            attachmentsNode = sc.getAttachmentsNode("RightLeg");
        } else if(partOfBody == PartOfBody.LeftLeg){
            obj.setLocalTranslation(0f, 0.1f, 0.3f);
            attachmentsNode = sc.getAttachmentsNode("LeftLeg");
        }

        if (attachmentsNode != null) {
            attachmentsNode.attachChild(obj);
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + bodyId + ", " + deviceId + ", " + partOfBody.name() + ")";
    }


    public String getBodyId() {
        return bodyId;
    }

    public String getEntityId() {
        return deviceId;
    }

    @PHATCommParam(mandatory = true, order = 1)
    public void setBodyId(String bodyId) {
        this.bodyId = bodyId;
    }

    @PHATCommParam(mandatory = true, order = 2)
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @PHATCommParam(mandatory = true, order = 3)
    public void setPartOfBody(PartOfBody partOfBody) {
        this.partOfBody = partOfBody;
    }
}
