package ac;
import robocode.*;import java.awt.Color;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import robocode.ScannedRobotEvent;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html

/**
 * Aditya - a robot by (your name here)
 */
public class Aditya extends TeamRobot
{
	/**
	 * run: Aditya's default behavior
	 */
	
	int gunTurnAmt = 10;
	int count = 0;
	String target;
	
	
	public void run() {
		// Initialization of the robot should be put here

		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:
		
		// Lets Gun, Radar, and Body Move on Its Own
		setAdjustGunForRobotTurn(true);
		setColors(Color.blue,Color.orange,Color.green); // body,gun,radar

		// Robot main loop
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			turnGunRight(gunTurnAmt);
			// Keep track of how many times we have turned
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		
		target = e.getName();
		System.out.println(target);
		
		
		double absoluteAngle = (getHeading() + e.getBearing());
		double relativeAngle = robocode.util.Utils.normalRelativeAngleDegrees(absoluteAngle - getGunHeading());
		
		turnGunRight(relativeAngle);
			
		if (target.equals("sampe.Tracker")) {
			// turnGunRight(150);
			turnRight(20);
		}
		else {
			setFire(600 / e.getDistance());
		}
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		// Replace the next line with any behavior you would like
		ahead(20);
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
		// Replace the next line with any behavior you would like
		back(20);
	}	
}
