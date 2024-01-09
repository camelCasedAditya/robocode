//Robot for the java program "robocode"
//Right now trying to get it to dodge bullets effectivly
//The main movement is based off a method called surf (it was invented by user ABC - https://robowiki.net/wiki/User:ABC), but advanced (or even basic sometimes) surfs are too complicated so I tried to sort of emulate it (to varrying degrees of success)
//It basically takes information on a bullet pattern (or at least tries) that the enemy fired and uses that to try and dodge the bullets
//I will refering to the bullet pattern as waves for easier understanding (waves: enemy bullet pattern. Robot will "surf" the waves)
//Code inspired by bots made by https://robowiki.net/wiki/User:Voidious
//The same sort of method is used for targeting the enemy (albeit to a simpler degree)
import robocode.*;
import java.awt.*;
import robocode.util.Utils;
import java.awt.geom.*;     // for Point2D's (java x,y coordinates)
import java.util.ArrayList;
import java.util.List;

//Api help: https://robocode.sourceforge.io/docs/robocode

public class DodgeRobot extends TeamRobot {
    public static int BINS = 47;
    public static double surfStats[] = new double[BINS];
    public Point2D.Double myLocation;     // our bot's location
    public Point2D.Double enemyLocation;  // enemy bot's location

    //Waves of the bullets the enemy shoots
    public ArrayList opponentWaves;
    public ArrayList surfDirections;
    public ArrayList surfAbsBearings;
    //The waves our robot shoots
    public ArrayList bulletWaves;
    
    //Helps the robot keeep distance later in the code (slightly off perpendicular)(Testings)
    public static double enemyEnergy = 100.0;
    static final double A_LITTLE_LESS_THAN_HALF_PI = 1.25; //Testing
    


    //goes with gun code (the wave bullet class) 
    //ist<WaveBullet> bulletWaves = new ArrayList<WaveBullet>();

    //goes with gun code 
    
    static int[] stats = new int[31]; 
                       // 31 is the number of unique GuessFactors we're using for targeting 
					  // Note: this must be odd number so we can get
					  // GuessFactor 0 at middle.
    //int[][] stats = new int[13][31]; // onScannedRobot can scan up to 1200px, so there are only 13. (this did not work in testing)
    //goes with gun code (temp)
    int direction = 1;

   /** This is a rectangle that represents an 800x600 battle field,
    * used for a method called "WallSmoothing" from someone named "PEZ"  -https://robowiki.net/wiki/User:PEZ 
    * It basically has a varriable called "wall stick", which indicates
    * the amount of space that should be between the tank and a wall at all times
    */
    public static Rectangle2D.Double fieldRect
        = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
    public static double WALL_STICK = 160;

    //Inistialize all the things
    public void run() {

        //Set colors 
        setBodyColor(new Color(128, 50, 50));
		setGunColor(new Color(200, 30, 20));
		setRadarColor(new Color(150, 40, 70));
		setScanColor(Color.white);
		setBulletColor(Color.blue);

        opponentWaves = new ArrayList();
        surfDirections = new ArrayList();
        //absolute bearings of the surve (absolute bearings are the headings+bearings)
        surfAbsBearings = new ArrayList();
        bulletWaves = new ArrayList();

        //sets the gun independant from the robots turn 
        setAdjustGunForRobotTurn(true);
        //Sets radar independant from gun turn
        setAdjustRadarForGunTurn(true);

        do {
            //The radar will keep scanning for things (scanner spins real fast)
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        } while (true);
    }

    //When the scanner finds an enemy robot (impliment it not being a team member later)
    public void onScannedRobot(ScannedRobotEvent event) {
         if (isTeammate(event.getName())) {
            return;
        }
        //Find my location
        myLocation = new Point2D.Double(getX(), getY());

        //Get the lateral velocity of the enemy
        double lateralVelocity = getVelocity()*Math.sin(event.getBearingRadians());
        //Get the absolute bearing of the enemy
        double absoluteBearing = event.getBearingRadians() + getHeadingRadians();

        //Goes with gun code
        //double gunTurnAmount;

        //Turning the radar towards the absolute bearing of the enemy robot/target.
        //This will help track the enemy the turn rate is adjusted because of the error between where the radar is looking and the target location
        //Helps keep enemy in sight
        setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing
            - getRadarHeadingRadians()) * 2);

        //Add values to surfDirections and surfAbsBearing 
        surfDirections.add(0,
            Integer.valueOf((lateralVelocity >= 0) ? 1 : -1)); //if the lateral velocity is greater than or equal to zero add 1 to surf directions
                                                             //else add negative one                 
                                                             //Tracks the direction the target is moving laterally            
        surfAbsBearings.add(0, Double.valueOf(absoluteBearing + Math.PI)); //Absolute bearing is the direction to the target robot, it then rotates it 180 degrees
                                                                                 //Tracks the abolute bearings of targets 
                                                                                 
                                                                                 
        
        /* detect a bullet on the tick after it is fired. Its source is from the enemy's location on the previous tick (that's when they called setFire or setFireBullet)
           it has already advanced by its velocity from that location and the last data the enemy saw before turning the gun for this bullet is from two ticks ago
            --because in the execution of one tick, bullets are fired before guns are turned. 
             -- So we must account for that when collecting the data of the enemy */
        //Get the bullet power of the of the enemy 
        double bulletPower = enemyEnergy - event.getEnergy();
        //This is the range of powers bullets can have (so we are seeing if the enemy fired a bullet and didn't loose energy due to other things)
        if (bulletPower < 3.01 && bulletPower > 0.09
            && surfDirections.size() > 2) { //And if the size of the surfDirections is greater than 2 (so we have at least 3 waves/points of changing direcitons)
            EnemyWave ew = new EnemyWave(); //create a new wave
            ew.fireTime = getTime() - 1; //Detects the fire time of the enemy 
                                         //Then minus one to go back a tick to when the enemy actually called "setFire"/"setFireBullet"
            ew.bulletVelocity = bulletVelocity(bulletPower); //Velocity of the bullet
            ew.distanceTraveled = bulletVelocity(bulletPower); //Set the distance traveled to the bullet velocity for the moment
            ew.direction = ((Integer)surfDirections.get(2)).intValue(); //Bullet has already advanced by its velocity from the location by the
                                                                              //and the last data the enemy saw before turning the gun for this bullet is from two ticks ago 
            ew.directAngle = ((Double)surfAbsBearings.get(2)).doubleValue();
            ew.fireLocation = (Point2D.Double)enemyLocation.clone(); // last tick
            
    
            opponentWaves.add(ew); //Add to the enemy wave data array
        }

        enemyEnergy = event.getEnergy(); //Energy of enemy

        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
        enemyLocation = project(myLocation, absoluteBearing, event.getDistance()); //Get the enemy location (projected from the absolute bearing and the distance from the enemy)
                                                                                   //Project passes in a point, angle, and distance/length and returns a point for where the enemy is
                                                                                   // (uses trig to get enemy location)
        
        updateWaves();
        doSurfing();

        
        // gun code would go here (add firing and stuff? Gun go br)(this was added later so it's not really connected to the other code)
        //This is kinda rough gun code but it works? (I think) //This code was inspired by user Kawigi's (https://robowiki.net/wiki/User:Kawigi) GuessFactorTargetingBots 
        //Would improve and add things from the dodge cod if had mroe time
        // find enemy's location:
		double enemyX = getX() + Math.sin(absoluteBearing) * event.getDistance();
		double enemyY = getY() + Math.cos(absoluteBearing) * event.getDistance(); 
		
		// Processing gun waves
		for (int i=0; i < bulletWaves.size(); i++)
		{
			WaveBullet currentWave = (WaveBullet)bulletWaves.get(i);
            //if the wave has hit enemy
			if (currentWave.checkHit(enemyX, enemyY, getTime()))
			{
				bulletWaves.remove(currentWave);
				i--;
			}
		}
		
		//double power = Math.min(3, Math.max(.1, /* some function */));
        //If there is more than one enemy, fire at full power, else fire at a power that is inversely proportional to the distance from the enemy
        double power = getOthers() > 1 ? 3 : Math.min(3, Math.max(600 / event.getDistance(), 1)); 
		// don't try to figure out the direction they're moving 
		// if they're not moving, just use the direction we had before
		if (event.getVelocity() != 0)
		{
			if (Math.sin(event.getHeadingRadians()- absoluteBearing)*event.getVelocity() < 0)
				direction = -1;
			else
				direction = 1;
		}
        int[] currentStats = stats; //This is kind of weird but it may be used later (if I have time)
        //Create a new wave bullet
		WaveBullet newWave = new WaveBullet(getX(), getY(), absoluteBearing, power,
                        direction, getTime(), currentStats);

        

        
        int bestindex = 15;	// initialize it to be in the middle, guessfactor 0. (0-31)
		for (int i=0; i<31; i++) {
			if (currentStats[bestindex] < currentStats[i]) {
				bestindex = i;
            }
		
		// this should do the opposite of the math in the WaveBullet:
        //Using the data to actually aim and fire at the enemy
		double guessfactor = (double)(bestindex - (stats.length - 1) / 2)
                        / ((stats.length - 1) / 2);
		double angleOffset = direction * guessfactor * newWave.gunMaxEscapeAngle(); 
                double gunAdjust = Utils.normalRelativeAngle(
                        absoluteBearing - getGunHeadingRadians() + angleOffset);

                if (event.getEnergy() == 0) { //If the robot is disabled (this fixes the tracking breaking when a robot becomes disabled)
                    gunAdjust = robocode.util.Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()+lateralVelocity / 15);
                    setTurnGunRightRadians(gunAdjust);
		            setFireBullet(power);
                } else {

                setTurnGunRightRadians(gunAdjust);
                //Prevent the robot from firing if the gun has to adjust more than half a robot turn (9 pixels laterally)
                if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, event.getDistance())) {
                        setFireBullet(power);
                        bulletWaves.add(newWave);
                }  
            }
        }
    }
	
    
    

    //Updates the waves (enemy bullet pattern)(this is called once per tick)
    public void updateWaves() {
        //Looops through the waves array list
        for (int x = 0; x < opponentWaves.size(); x++) {
            //Gets the wave
            EnemyWave ew = (EnemyWave)opponentWaves.get(x);

            //Gets the distance traveled by the wave
            ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity; //distance = time*velocity  
            //If the distance traveled is greater than the distance from the enemy to the wave
            if (ew.distanceTraveled >
                myLocation.distance(ew.fireLocation) + 50) { //Add 50 to the robot distance to give it a little bit of leeway (to track the onHitByBullet event)
                opponentWaves.remove(x);  //update the distance that each wave has traveled from its source, and delete any waves that have clearly passed. 
                x--;                     //if it was removed when it passed 0, we could still run into a bullet and get hit near our rear edge, and we would have already deleted the appropriate wave to link it to.
              //^Go back one in the array list (so we don't skip over any waves)                         
            }
        }
    }
    
    //Gets the closest surfable wave
    //Find the closets wave that hasn't passed the robot and returns it to the movement data/algorithm
    // TO IMPROVE: CHANGE TO FIRST WAVE (that will hit the robot) ISTEAD OF CLOSEST WAVE
     public EnemyWave getClosestSurfableWave() {
        double closestDistance = 50000; // use some big number here
        EnemyWave surfWave = null; //The wave we are surfing

        for (int x = 0; x < opponentWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)opponentWaves.get(x); //Get the wave
            double distance = myLocation.distance(ew.fireLocation) //Get the distance from the robot to the wave
                - ew.distanceTraveled;
                
                //If the distance is greater than the bullet velocity and less than the closest distance (so it is the closest wave)
                //(note that depending on a bullet's velocity and the position of the robot it could technically still hit the robot because of how Robocode game physics work 
                // (bullets will advance by velocity one more time before checking collisions). //However it is unlikely.
                // I found this is generally easier and doesn't really affect the performance of the robot.
            if (distance > ew.bulletVelocity && distance < closestDistance) {
                surfWave = ew; //Set the surf wave to the wave we are surfing
                closestDistance = distance; 
            }
        }

        return surfWave;
    } 
    
    //TESTING TO GET THE WAVE THAT WILL HIT THE ROBOT FIRST
     //Gets the wave that will hit the robot first
    public EnemyWave getFirstSurfableWave() {
        double earliestHitTime = Double.POSITIVE_INFINITY; // use positive infinity as the initial earliest hit time
        EnemyWave surfWave = null; //The wave that will hit the robot first

        for (int x = 0; x < opponentWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)opponentWaves.get(x); //Get the wave
            double hitTime = getTime() + (myLocation.distance(ew.fireLocation) - ew.distanceTraveled) / ew.bulletVelocity; //Calculate the estimated hit time

            if (hitTime < earliestHitTime) { //If the hit time is earlier than the current earliest hit time
                surfWave = ew; //Set the surf wave to the wave that will hit the robot first
                earliestHitTime = hitTime;
            }
        }
        return surfWave;
    }

    
    // Given the EnemyWave that the bullet was on, and the point where we were hit, calculate the index into the stat array for that factor.
    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)  - ew.directAngle); //Calculates the offset angle 
                                                                                                  //The offset angle is basically the relative angle that the enemy aimed at us 
                                                                                                  //(the current angle from the robot to the wave source minus the original angle from the robot to the source of the wave (the one at fire time))   
        double guessFactor = Utils.normalRelativeAngle(offsetAngle) //The guess factor (https://robowiki.net/wiki/GuessFactor - a way of measuring firing angles that takes into account the enemy's relative direction and the max escaping angle the enemy can go)
            / maxEscapeAngle(ew.bulletVelocity) * ew.direction; //Guess factor is offset angle divided by max escape angle

        return (int)limit(0,
            (guessFactor * ((BINS - 1) / 2)) + ((BINS - 1) / 2), 
                BINS - 1);                                                           //This reverses the sign  of the guess factor if the robot was moving counter clockwise at the fire time
                                                                                    //converts from range of -1 (counterclockwise) to 1 (clockwise) (0 is straight ahead)
                                                                                    //Example: With 50 bins, the middle bin (index 25) is GF 0, ther are 25 more bins on each side. 
                                                                                    //So if you multiply the GuessFactor by 25, you will get a number from -25 to 25. 
                                                                                    //Since we want a number from 0 to 50 in the array, add another 25.

    }

     // Given the EnemyWave that the bullet was on, and the point where we were hit, update stat array to show the danger in that area.
     // It passes along the location of the bullet that hit the robot to update the stat array
     //Get the array index for the hit location on the wave (this is just getFactorIndex) and update the stat array using "BinSmoothing" - https://robowiki.net/wiki/Bin_Smoothing 
    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);

        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }

    //When the robot is hit by a bullet (FIX to make only enemy bullets that hit the robot)
    public void onHitByBullet(HitByBulletEvent event) {
         if (isTeammate(event.getBullet().getName())) {
            return;
        } 
        // Get the name of the enemy that hit us
        String enemyName = event.getBullet().getName();

        //Update the targeting and dodging data (test)
        //updateTargeting(enemyName);
        //updateDodging(enemyName);
    


        //If the opponent collection isn't empty...
        // (If the opponent collection is empty, we must have missed the detection of the wave somehow.)
        if (!opponentWaves.isEmpty()) {
            // Get the point where we were hit
            Point2D.Double hitBulletLocation = new Point2D.Double(
                event.getBullet().getX(), event.getBullet().getY());
            //initialize a new enemywave (hitwave) (set to null for now)
            EnemyWave hitWave = null;

            // look through the EnemyWaves, and find one that could've hit us.
            for (int x = 0; x < opponentWaves.size(); x++) {
                //Get the wave
                EnemyWave ew = (EnemyWave)opponentWaves.get(x);

                //If the distance it has traveled is within 50 units of our current distance from the source of the wave
                //and the velocity is the same (within 0.001 units) as the bullet that hit us...
                if (Math.abs(ew.distanceTraveled -
                    myLocation.distance(ew.fireLocation)) < 50
                    && Math.abs(bulletVelocity(event.getBullet().getPower()) 
                        - ew.bulletVelocity) < 0.001) {
                    //Set hit wave to the current wave we are looking at
                    hitWave = ew;
                    break;  //The current enemy wave is the one that hit us, so we can stop looking for the wave that hit us.
                }
            }
            //If the hit wave isn't null (if we found a wave that hit us)
            if (hitWave != null) {
                //Update surf stats array
                logHit(hitWave, hitBulletLocation);
                
                // Remove that wave
                opponentWaves.remove(opponentWaves.lastIndexOf(hitWave));
            }
        }
    }

    public void onBulletHitBullet(BulletHitBulletEvent event) {
        //Do basically the same thing as on hitbybullet as this is just more data to be collected
         if (isTeammate(event.getBullet().getName())) {
            return;
        } 
        // Get the name of the enemy that hit us
        String enemyName = event.getBullet().getName();

        //If the opponent collection isn't empty...
        // (If the opponent collection is empty, we must have missed the detection of the wave somehow.)
        if (!opponentWaves.isEmpty()) {
            // Get the point where we were hit
            Point2D.Double hitBulletLocation = new Point2D.Double(
                event.getBullet().getX(), event.getBullet().getY());
            //initialize a new enemywave (hitwave) (set to null for now)
            EnemyWave hitWave = null;

            // look through the EnemyWaves, and find one that could've hit us.
            for (int x = 0; x < opponentWaves.size(); x++) {
                //Get the wave
                EnemyWave ew = (EnemyWave)opponentWaves.get(x);

                //If the distance it has traveled is within 50 units of our current distance from the source of the wave
                //and the velocity is the same (within 0.001 units) as the bullet that hit us...
                if (Math.abs(ew.distanceTraveled -
                    myLocation.distance(ew.fireLocation)) < 50
                    && Math.abs(bulletVelocity(event.getBullet().getPower()) 
                        - ew.bulletVelocity) < 0.001) {
                    //Set hit wave to the current wave we are looking at
                    hitWave = ew;
                    break;  //The current enemy wave is the one that hit us, so we can stop looking for the wave that hit us.
                }
            }
            //If the hit wave isn't null (if we found a wave that hit us)
            if (hitWave != null) {
                //Update surf stats array
                logHit(hitWave, hitBulletLocation);
                
                // Remove that wave
                opponentWaves.remove(opponentWaves.lastIndexOf(hitWave));
            }
        }
    }
    //tests
    /*public void updateDodging(String enemyName) {
           opponentWaves.add(enemyName);
        }
    
    public void updateTargeting(String enemyName) {
           bulletWaves.add(enemyName);
        }
        */
    

    //A percise position prediction method (the most complicated thing) based on rozu's methods (https://robowiki.net/wiki/User:Rozu)
    //Given the rules of Robocode Physics, the wave we are surfing
    //and the orbiting direction we are predicting (1 = clockwise, -1 = counter-clockwise),
    //it predicts where we would be when the wave intercepts us.
    //Pass in EnemyWave and a int for direction
    public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
        //Gather the current location, velocity, and heading of the robot
        Point2D.Double predictedPosition = (Point2D.Double)myLocation.clone(); 
        double predictedVelocity = getVelocity();
        double predictedHeading = getHeadingRadians();
        double maxTurning, moveAngle, moveDir;

        int counter = 0; // number of ticks in the future
        boolean intercepted = false;

        do { 
            //This is the core of the algorithm, it predicts the position of the robot
            //Each tick, predict the absolute angle at which we are trying to move
            //This is done by:
            //staying perpendicular to the absolute angle to the wave source. 
            //Once we have that angle, pass it to the Wall Smoothing method to get the angle to move with. 
            moveAngle =
                wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,  //Calculate abs bearing between the location where the enemy wave was fired and the robot's predicted position
                predictedPosition) + (direction * (Math.PI/2)), direction) //Add 90 degrees to the abs bearing (to get the perpendicular angle) (CHANGED TO A LITTLE LESS THAN THAT)
                - predictedHeading;   //Get the relative angle at which the robot should move by subtracting the predicted heading from the perpendicular angle
            moveDir = 1;   //Clockwise 

            //If the cos of move angle is less than 0
            if(Math.cos(moveAngle) < 0) {
                //Add 180 degrees (pi) to the move angle
                moveAngle += Math.PI;
                //Counter clockwise
                moveDir = -1;
            }

            moveAngle = Utils.normalRelativeAngle(moveAngle);

            // (maxTurning) - can't turn more then this in one tick
            //0.25*(40-3*abs(predictedVelocity))
            //Making the max turning angle smaller when the robot is moving faster
            //This helps the robot be more accurate because of robocode physics 
            maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
            //adjusts the predictedHeading based on the moveAngle but ensures that it does not exceeed the maxTurning angle 
            //Also makes sure that predictedHeading is a relative angle
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + limit(-maxTurning, moveAngle, maxTurning));

            // if predictedVelocity and moveDir have different signs...
            // break, since the robot is moving in the wrong direction
            // otherwise: accelerate                                         (look at the factor "2")(max breaking speed in the api is 2 pixels/tick)
            predictedVelocity +=
                (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
            predictedVelocity = limit(-8, predictedVelocity, 8); //Limit the predicted velocity to -8 and 8 (max velocity in the api is 8 pixels/tick)

            // calculate the new predicted position
            predictedPosition = project(predictedPosition, predictedHeading,
                predictedVelocity);

            counter++; //add one tick 

            //Check if the predicted position is intercepted by a wave
            if (predictedPosition.distance(surfWave.fireLocation) <
                surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                + surfWave.bulletVelocity) {
                intercepted = true;
            }
            //While the predicted position is not intercepted and the ticks counter is less than 500
        } while(!intercepted && counter < 500);

        return predictedPosition;
    }


    public double checkDanger(EnemyWave surfWave, int direction) {
        //Predict the position of our robot when the wave intercepts us for each orbit direciton
        int index = getFactorIndex(surfWave,
            predictPosition(surfWave, direction));

        //Get the score from the stat array for the GuessFactor of that position
        return surfStats[index];
    }

    //Finally do surfin
    //Check the orbit direction which is safest 
    //(clockwise: 1, counter clockwise: -1)
    public void doSurfing() {
        EnemyWave surfWave = getFirstSurfableWave();

        if (surfWave == null) { return; }

        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);

        //Go in that direction by using wall smoothing and set back as front
        double goAngle = absoluteBearing(surfWave.fireLocation, myLocation); 
        if (dangerLeft < dangerRight) {
            goAngle = wallSmoothing(myLocation, goAngle - Math.PI/2, -1);   //Math.PI/2 = 90 degrees -1 = counter clockwise
        } else {
            goAngle = wallSmoothing(myLocation, goAngle + Math.PI/2, 1); //Math.PI/2 = 90 degrees 1 = clockwise 
        }

        //Adjusts the turn direction based on the desired direction (robot moves backwards if it needs to turn more than 90 degrees which is faster than turning and moving forward)
        setBackAsFront(this, goAngle);
    }

    public void onHitRobot(HitRobotEvent event) {
        
        EnemyWave surfWave = getFirstSurfableWave();

        double dangerLeft = checkDanger(surfWave, -1); //Checking danger (same code as above)
        double dangerRight = checkDanger(surfWave, 1);

        if (dangerLeft < dangerRight) { //Basically if the robot collides with another robot (where it becomes useless) it makes sure robot will orbit in a direction away  
                                        //(uses basically the same code fomr doSurfing)
            if (getVelocity() < .2) { //In case it is stuck and hitting a wall (if it basically stops)
                wallSmoothing(myLocation, A_LITTLE_LESS_THAN_HALF_PI, -1); 
            } else {
                back(5);
                wallSmoothing(myLocation, A_LITTLE_LESS_THAN_HALF_PI, 1); 
            }

        } else {
            if (getVelocity() < .2) { //In case it gets stuck next to a wall
                wallSmoothing(myLocation, A_LITTLE_LESS_THAN_HALF_PI, 1);
            } else {
                back(5);
                wallSmoothing(myLocation, A_LITTLE_LESS_THAN_HALF_PI, -1); 
            }        
        }
    }

    
     
   
   
   
    //Methods that make the code more efficient (basically utilities to call in the code)
    class EnemyWave { //The enemy pattern class it stores things
        Point2D.Double fireLocation;
        long fireTime;
        double bulletVelocity, directAngle, distanceTraveled;
        int direction;

        public EnemyWave() { }
    }

    //Wall smoothing by PEZ ()
    //  A method to avoid collisions with walls without having to reverse (robot will turn and move right along the wall)
    //  https://robowiki.net/wiki/Wall_Smoothing
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!fieldRect.contains(project(botLocation, angle, WALL_STICK))) {
            angle += orientation*0.05;
        }
        return angle;
    }

    //Passes in a point, angle, and distance/length and returns a point/x,y coords (basically a vector)
    public static Point2D.Double project(Point2D.Double sourceLocation,
        double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,  //x = x + sin(angle)*length (x component of vector) 
            sourceLocation.y + Math.cos(angle) * length);                       //y = y + cos(angle)*length (y component of vector)
    }

    //Returns the absolute bearing between two points
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    //Limits a value between a min and max
    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    //Function for the bullet Velocity (so it's easier to calculate) given by the api (a bullet of full power travels at 17units/second)
    public static double bulletVelocity(double power) {
        return (20.0 - (3.0*power));
    }

    //Returns the max escape angle (the max angle the enemy can go to dodge the bullet)
    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0/velocity);
    }

    //Sets the back of the robot as the front
    public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
        double angle =
            Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI/2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1*angle);
           } else {
                robot.setTurnRightRadians(angle);
           }
            robot.setAhead(100);
        }
    }

    //This is for the gun code 
    public class WaveBullet {
            private double startX, startY, startBearing, power; 
        private long   fireTime;
        private int    direction; 
        private int[]  returnSegment;
        
        public WaveBullet(double x, double y, double bearing, double power,
                int direction, long time, int[] segment)
        {
            startX         = x;     //Location
            startY         = y;     //lcation
            startBearing   = bearing;  //locations we fired from
            this.power     = power;     //Power (speed) of the bullet
            this.direction = direction; //Direction of opponent relative to robot (remember: 1 = clockwise, -1 = counter clockwise)
            fireTime       = time;  //Time fired
            returnSegment  = segment; //array of stats (kind of where to return the "answer" to)
        }
        
        //Basically Same functions as above but without passing in things (it makes it a bit confusing but I didn't consider it when making the original code)(i named it differently to help differientiate)
        public double gunBulletVelocity()
        {
            return 20 - power * 3;
        }
        
        public double gunMaxEscapeAngle()
        {
            return Math.asin(8 / gunBulletVelocity());
        } 
        //Check if the wave has hit an enemy
        //If true figure out the GuessFactor the enemy is at and find the index of the array to return the "answer" to (and add one to that index/increment)
        public boolean checkHit(double enemyX, double enemyY, long currentTime)
        {
            // if the distance from the wave origin to our enemy has passed
            // the distance the bullet would have traveled...
            if (Point2D.distance(startX, startY, enemyX, enemyY) <= (currentTime - fireTime) * gunBulletVelocity())
            {
                double desiredDirection = Math.atan2(enemyX - startX, enemyY - startY); //Get the desired direction of the enemy
                double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing); //Get the angle offset (the difference between the desired direction and the direction we fired from)
                double guessFactor = Math.max(-1, Math.min(1, angleOffset / gunMaxEscapeAngle())) * direction; //Get the guess factor (the angle offset divided by the max escape angle times the direction) 
                                                                                                            //(other checks are in place for things that should not be possible (ratio is not > 1 or < -1))
                int index = (int) Math.round((returnSegment.length - 1) /2 * (guessFactor + 1)); //Find the appropiate index. 
                                                                                                  //THe result is a value that represents a proportional position with the return segment array based on the guessFactor
                                                                                                  //Which is rounded to the nearest integer (so it can be used as an index)
                returnSegment[index]++; //Increment the index of the array
                return true;
            }
            return false;
        }
    }
}

//Improvements ideas:
/*
 * Keep Distance?
 * Add logic to keep still if that would be better
 * Add onBulletHitBullet logic
 * choose the wave that hit the robot first instead of a close one
 */