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
package phat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import com.jme3.system.AppSettings;
import phat.app.PHATApplication;
import phat.app.PHATInitAppListener;
import phat.body.BodiesAppState;
import phat.body.commands.*;
import phat.devices.DevicesAppState;
import phat.devices.commands.CreateAccelerometerSensorCommand;
import phat.devices.commands.CreateSmartphoneCommand;
import phat.devices.commands.SetDeviceOnPartOfBodyCommandLocal;
import phat.mobile.servicemanager.server.ServiceManagerServer;
import phat.mobile.servicemanager.services.Service;
import phat.sensors.accelerometer.AccelerationData;
import phat.sensors.accelerometer.AccelerometerControl;
import phat.sensors.accelerometer.XYAccelerationsChart;
import phat.server.PHATServerManager;
import phat.server.ServerAppState;
import phat.server.commands.ActivateAccelerometerServerCommand;
import phat.structures.houses.TestHouse;
import phat.util.Debug;
import phat.util.SpatialFactory;
import phat.world.WorldAppState;
import sim.android.hardware.service.SimSensorEvent;

import javax.swing.*;

/**
 * Activity Monitoring Demo.
 * @author UCM
 */
public class ActivityMonitoringDemo implements PHATInitAppListener {

    private static final Logger logger = Logger.getLogger(TestHouse.class.getName());
    private BodiesAppState bodiesAppState;
    private ServerAppState serverAppState;
    private DevicesAppState devicesAppState;
    private WorldAppState worldAppState;

    private static Properties config;

    public static void main(String[] args) {
        ActivityMonitoringDemo test = new ActivityMonitoringDemo();
        PHATApplication phat = new PHATApplication(test);
        phat.setDisplayFps(true);
        phat.setDisplayStatView(false);
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Activity Monitoring Demo");
        settings.setWidth(640);
        settings.setHeight(480);
        phat.setSettings(settings);
        phat.start();
    }

    @Override
    public void init(SimpleApplication app) {
        SpatialFactory.init(app.getAssetManager(), app.getRootNode());

        AppStateManager stateManager = app.getStateManager();

        app.getFlyByCamera().setMoveSpeed(10f);

        app.getCamera().setLocation(new Vector3f(0.2599395f, 2.7232018f, 3.373138f));
        app.getCamera().setRotation(new Quaternion(-0.0035931943f, 0.9672268f, -0.25351822f, -0.013704466f));

        BulletAppState bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setAccuracy(1 / 60f);

        worldAppState = new WorldAppState();
        worldAppState.setLandType(WorldAppState.LandType.Grass);
        app.getStateManager().attach(worldAppState);
        worldAppState.setCalendar(2019, 9, 15, 15, 40, 40);

        Debug.enableDebugGrid(10, app.getAssetManager(), app.getRootNode());
        bodiesAppState = new BodiesAppState();
        stateManager.attach(bodiesAppState);

        bodiesAppState.createBody(BodiesAppState.BodyType.Elder, "Patient");
        bodiesAppState.runCommand(new SetBodyInCoordenatesCommand("Patient", Vector3f.ZERO));

        bodiesAppState.runCommand(new SetSpeedDisplacemenetCommand("Patient", 0.5f));
        bodiesAppState.runCommand(new SetStoopedBodyCommand("Patient", true));

        SetCameraToBodyCommand camCommand = new SetCameraToBodyCommand("Patient");
        camCommand.setDistance(3);
        camCommand.setFront(true);
        bodiesAppState.runCommand(camCommand);

        devicesAppState = new DevicesAppState();
        stateManager.attach(devicesAppState);


        devicesAppState.runCommand(new CreateAccelerometerSensorCommand("Sensor1"));
        devicesAppState.runCommand(new SetDeviceOnPartOfBodyCommandLocal("Patient", "Sensor1",
                SetDeviceOnPartOfBodyCommandLocal.PartOfBody.Chest));

        devicesAppState.runCommand(new CreateAccelerometerSensorCommand("Sensor2"));
        devicesAppState.runCommand(new SetDeviceOnPartOfBodyCommandLocal("Patient", "Sensor2",
                SetDeviceOnPartOfBodyCommandLocal.PartOfBody.RightLeg));

        devicesAppState.runCommand(new CreateAccelerometerSensorCommand("Sensor3"));
        devicesAppState.runCommand(new SetDeviceOnPartOfBodyCommandLocal("Patient", "Sensor3",
                SetDeviceOnPartOfBodyCommandLocal.PartOfBody.LeftHand));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread() { public void run() {
            launchRemoteXYChart(PHATServerManager.getAddress(),"Remote Chest"
                    ,"Sensor1");
            launchRemoteXYChart(PHATServerManager.getAddress(),"Remote Right Leg"
                    ,"Sensor2");
            launchRemoteXYChart(PHATServerManager.getAddress(),"Remote Left Hand"
                    ,"Sensor3");
        } }.start();

        serverAppState = new ServerAppState();
        stateManager.attach(serverAppState);

        serverAppState.runCommand(new ActivateAccelerometerServerCommand("PatientBodyAccel", "Sensor1"));
        serverAppState.runCommand(new ActivateAccelerometerServerCommand("PatientBodyAccel", "Sensor2"));
        serverAppState.runCommand(new ActivateAccelerometerServerCommand("PatientBodyAccel", "Sensor3"));

        stateManager.attach(new AbstractAppState() {
            PHATApplication app;

            @Override
            public void initialize(AppStateManager asm, Application aplctn) {
                app = (PHATApplication) aplctn;
            }

            float cont = 0f;
            boolean fall = false;
            float timeToChange = 10f;
            boolean init = false;

            @Override
            public void update(float f) {
                if (!init) {
                    AccelerometerControl ac1 = devicesAppState.getDevice("Sensor1")
                            .getControl(AccelerometerControl.class);
                    ac1.setMode(AccelerometerControl.AMode.GRAVITY_MODE);

                    AccelerometerControl ac2 = devicesAppState.getDevice("Sensor2")
                            .getControl(AccelerometerControl.class);
                    ac2.setMode(AccelerometerControl.AMode.GRAVITY_MODE);

                    AccelerometerControl ac3 = devicesAppState.getDevice("Sensor3")
                            .getControl(AccelerometerControl.class);
                    ac3.setMode(AccelerometerControl.AMode.GRAVITY_MODE);

                    init = true;

                }

                System.out.println("Change to Running:::" + String.valueOf(cont) + "-" + String.valueOf(f));
                bodiesAppState.runCommand(new PlayBodyAnimationCommand("Patient", "RunForward"));

//                cont += f;
//                if (cont > timeToChange && cont < timeToChange + 10f && !fall) {
//                    System.out.println("Change to Running:::" + String.valueOf(cont) + "-" + String.valueOf(f));
//                    bodiesAppState.runCommand(new PlayBodyAnimationCommand("Patient", "RunForward"));
//                    /*
//                     * Gesture
//                     * SpinSpindle: 	abrir puerta con dificultad
//                     * Hands2Hips: 		llevar manos a la cadera, (dolor de espalda)
//                     * Hand2Belly: 		llevar la mano al vientre, (dolor de vientre)
//                     * Wave: 			pedir ayuda o llamar atención
//                     * ScratchArm: 		rascar el codo
//                     * LeverPole: 		molestias en el movimiento y pedir ayuda
//                     *
//                     */
//                    fall = true;
//                } else {
//                    if (fall && cont > timeToChange + 17f) {
//                        System.out.println("Change to Walking:::" + String.valueOf(cont) + "-" + String.valueOf(f));
//                        bodiesAppState.runCommand(new PlayBodyAnimationCommand("Patient", "WalkForward"));
//                        fall = false;
//                        cont = 0;
//                    }
//                }
            }
        });

    }

    /**
     * launchRemoteXYChart.
     * @param host host
     * @param title title
     * @param sensor sensor
     */
    public static void launchRemoteXYChart(final InetAddress host, final String title, final String sensor) {
        Service taccelService = ServiceManagerServer.getInstance().getServiceManager().getService("PatientBodyAccel", sensor);
        while (taccelService == null) {
            // not ready
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            taccelService = ServiceManagerServer.getInstance().getServiceManager().getService("PatientBodyAccel", sensor);
        }
        final Service accelService = taccelService;
        new Thread() {
            public void run() {
                Socket s = null;
                try {
                    final XYAccelerationsChart chart = new XYAccelerationsChart(title,
                            sensor + ": " + title + " accelerations", "m/s2", "x,y,z");
                    chart.showWindow();
                    for (int k = 0; k < 5 && s == null; k++)
                        try {
                            s = new Socket(host, accelService.getPort());
                        } catch (IOException e) {
                            e.printStackTrace();
                            try {
                                Thread.currentThread().sleep(500);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }

                        }
                    BufferedReader is = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String objRead = null;
                    Long lastRead = new Date().getTime();
                    do {
                        objRead = is.readLine();
                        final long interval = new Date().getTime() - lastRead;
                        lastRead = new Date().getTime();
                        if (objRead != null && !objRead.isEmpty()) {
                            final SimSensorEvent sse = SimSensorEvent.fromString(objRead);
                            if (sse != null) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        AccelerationData ad = new AccelerationData(interval, sse.getValues()[0],
                                                sse.getValues()[1], sse.getValues()[2]);
                                        chart.update(null, ad);
                                        chart.repaint();
                                    }
                                });
                            }
                        }
                    } while (objRead != null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}