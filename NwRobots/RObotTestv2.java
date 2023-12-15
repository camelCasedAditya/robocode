//Robot for the java program "robocode"
//Right now trying to get it to dodge bullets effectivly
//The main movement is based off a method called surf, but advanced (or even basic sometimes) surfs are too complicated so I tried to sort of emulate it (to varrying degrees of success)
//It basically takes information on a bullet pattern (or at least tries) that the enemy fired and uses that to try and dodge the bullets
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*; //For Point2D (a location on the x,y coordinates (useful for robocode))
import java.util.ArrayList;
import java.awt.Color;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html




/**
 * RObotTest - a robot by (your name here)
 */
public class RObotTest extends Robot
{
	
	public Point2D.Double myLocation;	//Robot's location
	public Point2D.Double enemyLocation; //Enemy location

	public ArrayList enemyBulletPattern;
	public ArrayList SafePositions;

	public static double enemyEnergy = 100.0;

	//Add wall funcitonality here??


	class BulletPattern {
		Point2D.Double fireLocation;
		long timeFired;
		double bulletVelocity, directAngle, distanceTraveled;
		int bulletDirection;
	}
	//Function for the bullet Velocity (so it's easier to calculate) given by the api (a bullet of full power travels at 17units/second)
	public static double bulletVelocity(double power) {
        return (20.0 - (3.0*power));
    }
	//Finding the absolute angle between two objects (like each robot or a robot and a bullet)(with help)
	public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }
	//Projects a point from a source location along an angle and distance (with helpo)
	public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) { 
		return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length, sourceLocation.y + Math.cos(angle) * length); }

	/**
	 * run: RObotTest's default behavior
	 */
	public void run() {
		// Initialization of the robot 
		enemyBulletPattern = new ArrayList();
		SafePositions = new ArrayList();

		/* setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		*/
		
		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:

		// setColors(Color.red,Color.blue,Color.green); // body,gun,radar

		// Robot main loop
		do {
			//Turns the radar right 
			turnRadarRight(Double.POSITIVE_INFINITY);
		} while (true);
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent event) {
		//Get my location
		myLocation = new Point2D.Double(getX(),getY());
		
		//Heading is the absolute angle in degrees with 0 facing up the screen, positive clockwise. 0 <= heading < 360. (from api)
		//Bearing is the relative angle to some object from the robot's heading, positive clockwise. -180 < bearing <= 180 (from api)
		//Find robot bearings
		double bearing = event.getBearing() + getHeading();
		//Find the bearing stats of the enemy?
		//horizontalVelocity = getVelocity()*Math

		//Detect the bullet power of the enemy 
		double bulletPower = enemyEnergy - event.getEnergy();
		//Detect if enemy actually fired a bullet (1 to 3 are the power levels of bullets)
		if (bulletPower > 3.01 && bulletPower > 0.9 /* && SafePositions.size() > 2 */) {
			BulletPattern pattern = new BulletPattern();
			pattern.timeFired = getTime();
			pattern.bulletVelocity = bulletVelocity(bulletPower);
			pattern.distanceTraveled = bulletVelocity(bulletPower);
			//pattern.bulletDirection = ((Integer)SafePositions.get(2)).intValue();
			//pattern.bulletAngle = ((Double)bearing.get(2)).intValue();
			pattern.fireLocation = (Point2D.Double)enemyLocation.clone();

			enemyBulletPattern.add(pattern);
		}
		//Get the enemies energy
		enemyEnergy = event.getEnergy();

		//Predict the enemy's location
		enemyLocation = project(myLocation, bearing, event.getDistance()); 
			
		updateEnemyBulletPattern();
		dodge();
		
	}

	public void updateEnemyBulletPattern() {
		//Go through the bullet pattern array
	for (int i = 0; i < enemyBulletPattern.size(); i++) {
			BulletPattern pattern = (BulletPattern)enemyBulletPattern.get(i);
			//we can get the distance traveled by finding the time the bullet was fired (also account for how fast the bullet was (how much power))
			//Update the distance of the bullets/enemy pattern
			pattern.distanceTraveled = (getTime() - pattern.timeFired)*pattern.bulletVelocity;
			//If the distance between the robot and the last known enemy fire location....
			if (pattern.distanceTraveled > myLocation.distance(pattern.fireLocation) + 40);
			//Recorrect the array if the bullet has passed the robot's location //FIX IN CASE THE ROBOT RUNS INTO THE BULLET BACKWARD (add like 40 to the robot location?)
			enemyBulletPattern.remove(i);
			//go back one index in the pattern array
			i--;
		}
	}
	//Find a path through the bullets that the robot get go through
	public BulletPattern findCloseDodgePath() {
		double closestDistance = 40000; 
		BulletPattern closestPattern = null;

		//Loops through the bullet pattern array to find the closest bullet pattern to the robot's location (closestBullet)
		for (int i = 0; i < enemyBulletPattern.size(); i++) {
				BulletPattern pattern = (BulletPattern)enemyBulletPattern.get(i);
				//Getting the distance between the robot and the firing pattern location
				double distance = myLocation.distance(pattern.fireLocation);

				//if the distance between the firing pattern is > than the velocity...(has the bullet traveled far enough)(wait till the bullet passes the robot's center)
				//And if the distance the bullet has traveled is < the closest recorded distance to the target...(checks for if the bullet is still the closest one to the target/it's not going off screen)
				if (distance > pattern.bulletVelocity && pattern.distanceTraveled < closestDistance) {
					//update the closest distance and the bullet pattern
					closestDistance = distance;
					closestPattern = pattern;
				}
		}	
		return closestPattern;
}
	//using guessFacotr https://robowiki.net/wiki/GuessFactor_Targeting_(traditional) 
	//im a bit confused here
	//Gonna not do this
	/* public int GuessFactors(BulletPattern pattern, Point2D.Double targetLocation) {

	} */
	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent event) {
		//If the colleciton of patterns is empty...(it didn't detect one?)
		if (!enemyBulletPattern.isEmpty()) {
			//Make a point to represent the target location (the robot's location)
			Point2D.Double targetLocation = new Point2D.Double(getX(),getY());
			BulletPattern hitPattern = null;

			//Loop through the bullet patterns and find the closest one to the robot's location
			for (int i = 0; i < enemyBulletPattern.size(); i++) {
				BulletPattern pattern = (BulletPattern)enemyBulletPattern.get(i);

				//If the bullet has traveled is less than 50 it's a hit (have it not be too far away)
				//Find the difference between the predicted velocity of the bullet and the actual velocity. 
				//Then if that is < .001 it means that the velocity is pretty accurate 
				if (Math.abs(pattern.distanceTraveled) < 50 && Math.abs(bulletVelocity(event.getBullet().getPower()) - pattern.bulletVelocity) < 0.001 ) {
					hitPattern = pattern;
				}

			}

			if (hitPattern != null) { 
				//If the bullet has been hit, then fire at the target location
				//fire(hitPattern, targetLocation); //Fix this latter (able to pass in these things)
				fire(20);
				//REmove this pattern as it is done
				enemyBulletPattern.remove(hitPattern);
			}
		}
	}

	//Try to predict the enemy's position??? (actually that's some hard math)

	//Finally add the actual dodge function
	public void dodge() { 
		BulletPattern dodgePattern = findCloseDodgePath();
		
		if (dodgePattern != null) {
			return;
		}
		
		double goAngle = absoluteBearing(dodgePattern.fireLocation, myLocation);
	}
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
		// Eventually have the robot never it the wall so it doesn't slow down
		back(20);
	}	
}

/* Ideas board/robot improvements:
	-Add a wall detection function to the robot (so it doesn't run into the wall)
	-Keep distance from enemies
	-More accurate enemy energy tracking
	-Get data for onBulletHitBullet
	-in choosing which dodge path to go through, change to the path that the robot will hit the first (instead of closest like now)
	-Incorporate stopping (in case it's better)
	-Add a function to the robot to predict the enemy's percise position (actually that may be some hard math)(at least improve enemy position tracking?)


	ehh:
	-Segmantation (no/too advanced)
	

	*/
