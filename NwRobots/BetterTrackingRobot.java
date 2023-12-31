import robocode.*;
import java.awt.*;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.util.Scanner;

//set J2D_D3D=false remeber for robocode
/* 
 * Improvement of tracking robot/tracker sample in robocode
 */

 public class BetterTrackingRobot extends AdvancedRobot {
    int moveDirection = 1; 

    

    public void run() {
        //Set colors 
        setBodyColor(new Color(128, 128, 50));
		setGunColor(new Color(50, 50, 20));
		setRadarColor(new Color(200, 200, 70));
		setScanColor(Color.white);
		setBulletColor(Color.blue);

        
        setAdjustGunForRobotTurn(true);
        //setAdjustRadarForGunTurn(true);
        setAdjustRadarForGunTurn(true);
        turnRadarRightRadians(Double.POSITIVE_INFINITY);
    }


    public void onScannedRobot (ScannedRobotEvent event) {
        double absoluteBearing = event.getBearingRadians()+getHeadingRadians();
        double lateralVelocity = event.getVelocity() * Math.sin(event.getHeadingRadians() - absoluteBearing); 
        double gunTurnAmount;

        setTurnRadarLeftRadians(getRadarTurnRemainingRadians());

        //Detect if the opponent may have fired 
        if (Math.random() > .9)
            {
                setMaxVelocity((12* Math.random()) + 12); //12+12=24?/22= 
            }
            if (event.getDistance() > 150) {
                gunTurnAmount = robocode.util.Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()+lateralVelocity / 22);
                setTurnGunRightRadians(gunTurnAmount);
                //It goes forward to the assigned position
                setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(absoluteBearing - getHeadingRadians() + lateralVelocity/getVelocity()));
                setAhead((event.getDistance() - 140) * moveDirection);
                setFire(3);
             }
             else {
                gunTurnAmount = robocode.util.Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()+lateralVelocity / 15);
                setTurnGunRightRadians(gunTurnAmount);
                //Turns perpendicular to the enemy (90 degrees minus get bearing to take away the angle the gun is already pointed)
                setTurnLeft(-90-event.getBearing()); 
                //It goes forward to the assigned position
                setAhead((event.getDistance() - 140) * moveDirection); //Go forward
                setFire(3);
             }
    }
    
    public void onHitRobot(HitRobotEvent event) {
        //gunTurnAmount = robocode.util.Utils.normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
        //turnGunRight(gunTurnAmount);
        back(50);
     }


     public void onHitWall(HitWallEvent event) {
        moveDirection = -moveDirection;
     }
 }