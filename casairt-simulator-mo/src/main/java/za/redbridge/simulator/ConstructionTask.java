package za.redbridge.simulator;

import org.jbox2d.dynamics.World;
import org.jbox2d.common.Vec2;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.config.SchemaConfig;
import org.jbox2d.dynamics.Body;
import org.jbox2d.common.Rot;
import org.jbox2d.common.Transform;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import sim.engine.Steppable;
import sim.engine.SimState;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.text.DecimalFormat;
import sim.util.Bag;
import sim.field.grid.SparseGrid2D;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Iterator;

/*
 *  The construction task class. Calculates fitness values for the simulator as well as manages robots and resources.
 *
 */
public class ConstructionTask implements Steppable{

    private static int nConstructionZones = 3;

    private ArrayList<ResourceObject> resources;
    private ArrayList<RobotObject> robots;
    private SchemaConfig schema;
    private World physicsWorld;
    private double teamFitness;
    private DecimalFormat df;

    private ArrayList<ConstructionZone> constructionZones;
    private int numCZsStarted = 0;
    private boolean IS_FIRST_CONNECTED = true;
    private int maxSteps = 0;

    private double aveRobotDistance;
    private double avePickupCount;
    private double aveResDistance;
    private double numAdjacentResources;
    private double numCorrectlyConnected;
    private double aveResDistanceFromCZ;
    private final double maxDistance;

    private int nConstructedResources;
    private int totalResourceValue = 0;
    private final int schemaNumber;
    private final ArrayList<ResourceObject> globalConstructionOrder;
    private ContToDiscrSpace discreteGrid;

    private ConstructionZone finalConstructionZone = new ConstructionZone(-1);

    public ConstructionTask(SchemaConfig schema, ArrayList<ResourceObject> r, ArrayList<RobotObject> robots, World world, int schemaNumber, double envWidth, double envHeight){
        this.schema = schema;
        this.schemaNumber = schemaNumber;
        resources = r;
        this.robots = robots;

        df = new DecimalFormat("###.####");

        physicsWorld = world;
        maxDistance = Math.sqrt(Math.pow(envWidth, 2) + Math.pow(envHeight, 2));

        for (ResourceObject res : resources) {
            totalResourceValue += res.getValue();
        }

        double numResources = (double)resources.size();
        int maxCZs = (int)Math.floor(numResources/2D);
        constructionZones = new ArrayList<ConstructionZone>();
        globalConstructionOrder = new ArrayList<>();
    }

    /**
     * Returns the resource objects in the simulator as an array
     * @return ArrayList of resource objects
     */
    public ArrayList getSimulationResources() {
        return resources;
    }

    /**
     * Each step checks if resources can be constructed together. If possible they get transformed to their discrete grid position.
     * @param simState SimState variable
     */
    @Override
    public void step(SimState simState) {
        Simulation s = (Simulation) simState;
        discreteGrid = s.getDiscreteGrid();

        // checks if all resources are constructed
        if (discreteGrid.getAllResources().size() == resources.size()) {
            System.out.println("All resources constructed!");
            s.finish();
        }

        // updates the adjacent list for each resource
        for (ResourceObject resource : resources) {
            resource.updateAdjacent(resources);
        }

        for(ResourceObject resource : resources){
            if (!resource.isConstructed()) {
                String [] resAdjacentList = resource.getAdjacentList();
                for (int i = 0; i < resAdjacentList.length; i++) {
                    if (constructionZones.size()==0) {
                        if (!resAdjacentList[i].equals("_")) {
                            ResourceObject neighbour = resource.getAdjacentResources()[i];
                            if (resource.pushedByMaxRobots() || neighbour.pushedByMaxRobots()) {
                                //If both resources are connected correctly according to the schema
                                if (((schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList)/resource.getNumSidesConnectedTo()) == 1)) {
                                    if (discreteGrid.canBeConnected(resource, neighbour, i)) {
                                        numCZsStarted ++;
                                        globalConstructionOrder.add(resource);
                                        globalConstructionOrder.add(neighbour);
                                        constructionZones.add(new ConstructionZone(numCZsStarted));
                                        constructionZones.get(0).startConstructionZone(resource, neighbour);

                                        alignResource(resource, null, i);
                                        //Reflect the connection side to the view from the other resource
                                        int reflection = 0;
                                        if (i == 0) {
                                            reflection = 1;
                                        }
                                        else if (i == 1) {
                                            reflection = 0;
                                        }
                                        else if (i == 2) {
                                            reflection = 3;
                                        }
                                        else {
                                            reflection = 2;
                                        }
                                        alignResource(neighbour, resource, reflection);
                                    }
                                }
                            }
                        }
                    }
                    else {
                        if ((!resAdjacentList[i].equals("_")) && (resource.pushedByMaxRobots())) {
                            // boolean shouldSTartNewCZ = true;
                            ResourceObject neighbour = resource.getAdjacentResources()[i];
                            if (neighbour.isConstructed() && ((schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList)/resource.getNumSidesConnectedTo()) == 1)) {
                                if (discreteGrid.canBeConnected(resource, neighbour, i)) {
                                    int czNum = neighbour.getCzNumber();
                                    globalConstructionOrder.add(resource);
                                    constructionZones.get(czNum-1).addResource(resource, false);
                                    alignResource(resource, neighbour, i);
                                }
                            }
                            else {
                                if(constructionZones.size()<nConstructionZones){
                                    if (resource.pushedByMaxRobots() || neighbour.pushedByMaxRobots()) {
                                        if ((schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList)/resource.getNumSidesConnectedTo()) == 1) {
                                            if (discreteGrid.canBeConnected(resource, neighbour, i)) {
                                                //Update the overall construction order (for Novelty)
                                                globalConstructionOrder.add(resource);
                                                globalConstructionOrder.add(neighbour);

                                                numCZsStarted++;
                                                constructionZones.add(new ConstructionZone(numCZsStarted));
                                                constructionZones.get(numCZsStarted-1).startConstructionZone(resource, neighbour);
                                                // tryCreateWeld(resource, neighbour);
                                                alignResource(resource, null, i);
                                                int reflectedSide = 0;
                                                if (i == 0) {
                                                    reflectedSide = 1;
                                                }
                                                else if (i == 1) {
                                                    reflectedSide = 0;
                                                }
                                                else if (i == 2) {
                                                    reflectedSide = 3;
                                                }
                                                else {
                                                    reflectedSide = 2;
                                                }
                                                alignResource(neighbour, resource, reflectedSide);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates construction zones. Recursively goes through resources and combines them into construction zones
     */
    public void updateConstructionZones() {
        ArrayList<ConstructionZone> newConstructionZones = new ArrayList<>();

        // check if there are construction zones
        if (constructionZones.size() > 0) {
            int czNum = 0;
            // go through every construction zone
            for(ConstructionZone cz : constructionZones){
                // list of possible traversals for a construction zone
                ArrayList<ArrayList<ResourceObject>> generatedTraversals = new ArrayList<>();

                //For each resource (in order of construction)
                for (ResourceObject res : cz.getConstructionOrder()) {
                    int resCZNum = res.getCzNumber();
                    //Lists to be updated as traversal happens
                    ArrayList<ResourceObject> traversal = new ArrayList<>();
                    ArrayList<ResourceObject> ignoreList = new ArrayList<>();

                    //Generate the traversal
                    discreteGrid.traverseConstructionZone(traversal, res, ignoreList);

                    //If there is no equivalent traversal already generated
                    if (!ConstructionTask.traversalsContains(traversal, generatedTraversals)) {

                        //Calculate the value of the traversal
                        int tValue = getConstructionValue(traversal);

                        //If this traversal has a higher value than the starting resource's CZ value
                        if (tValue >= cz.getTotalResourceValue()) {
                            // System.out.println("This traversal is more valuable!");
                            generatedTraversals.add(traversal); //add this traversal to the generated traversals list (should become a CZ)
                        }
                    }

                    for (ResourceObject generalResource : resources) {
                        generalResource.setVisited(false);
                    }
                }

                // loop through possibilites and get best
                if(generatedTraversals.size() > 0){
                    ArrayList<ResourceObject> bestPossibility = generatedTraversals.get(0);
                    int bestValue = 0;
                    for(ArrayList<ResourceObject> possibleTraversal : generatedTraversals){
                        int tempValue = getConstructionValue(possibleTraversal);
                        if(tempValue > bestValue){
                            bestValue = tempValue;
                            bestPossibility = possibleTraversal;
                        }
                    }

                    newConstructionZones.add(new ConstructionZone(bestPossibility, czNum));
                    czNum++;
                }
            }

            constructionZones.clear();
            for(ConstructionZone cz : newConstructionZones){
                constructionZones.add(cz);
            }
        }
    }

    /**
     * Aligns a given resource according to continuous space and maps it to the discrete grid
     * @param  res resource to be aligned
     * @param  resToConnectTo resource it is connected to
     * @param  connectionType side of the other resource it is connecting too
     */
    public void alignResource(ResourceObject res, ResourceObject resToConnectTo, int connectionType) {
        Body resBody = res.getBody();
        Transform xFos = resBody.getTransform();
        Transform newDiscrTransform = new Transform(discreteGrid.addResourceToDiscrSpace(res), new Rot(0f));

        xFos.set(newDiscrTransform);
        res.getPortrayal().setTransform(xFos);
        res.getBody().setTransform(xFos.p, xFos.q.getAngle());
    }

    /**
     * Gets the number of adjacent sides of the resource
     * @param  resource the given resource object
     * @return          number of adjacent sides
     */
    private int adjacentSides(ResourceObject resource){
        int n = 0;
        for(int i=0;i<resource.getAdjacentList().length;i++){
            if(resource.getAdjacentList()[i]!="_"){
                n++;
            }
        }
        return n;
    }

    /**
     * Returns the total value of a list of resources
     * @param  resourceArray list of resources
     * @return               int value of their combined value
     */
    public int getConstructionValue(ArrayList<ResourceObject> resourceArray){
        int value = 0;
        for(ResourceObject resource : resourceArray){
            value += resource.getValue();
        }
        return value;
    }

    public static boolean traversalsContains(ArrayList<ResourceObject> t, ArrayList<ArrayList<ResourceObject>> traversals) {
        boolean doesContain = false;
        for ( ArrayList<ResourceObject> prevTraversal : traversals) {
            //Loop through prev. traversal and check if all elements are contained in t
            boolean isTraversalEquiv = true;
            for (ResourceObject ptRes : prevTraversal) {
                if (!t.contains(ptRes)) {
                    isTraversalEquiv = false;
                    break;
                }
            }
            if (isTraversalEquiv) {
                doesContain = true;
                break;
            }
        }
        return doesContain;
    }

    public int[] configResQuantity(int i){
        return schema.getResQuantity(i);
    }

    public ArrayList<ResourceObject> getResources(){
        return resources;
    }

    public double getPerformance(){
        double performance = 0;
        return Double.valueOf(df.format(performance));
    }

    /**
     * finds the best construction zone according to value
     */
    public void getBestConstructionZone(){
        double bestValue = 0;
        ConstructionZone bestConstructionZone = new ConstructionZone(-2);

        if(constructionZones.size()>0){
            bestConstructionZone = constructionZones.get(0);
            bestValue = bestConstructionZone.getValue();

            for (ConstructionZone cz : constructionZones) {
                int newValue = cz.getValue();
                if(newValue > bestValue){
                    bestValue = newValue;
                    bestConstructionZone = cz;
                }
            }
        }

        finalConstructionZone = bestConstructionZone;
    }

    /**
     * Calculate a fitness value according to the largest construction zone
     * @return fitness value
     */
    public double calculateConstructionZoneFitness(){
        double fitness = 0;
        double bestValue = 0;

        ConstructionZone bestConstructionZone = new ConstructionZone(-2);

        if(constructionZones.size()>0){
            bestConstructionZone = constructionZones.get(0);
            bestValue = bestConstructionZone.getValue();

            for (ConstructionZone cz : constructionZones) {
                int newValue = cz.getValue();
                if(newValue > bestValue){
                    bestValue = newValue;
                    bestConstructionZone = cz;
                }
            }
        }
        else{
            return fitness;
        }

        finalConstructionZone = bestConstructionZone;
        fitness = bestValue;
        double totalValue = 0;
        for(ResourceObject resource : resources){
            totalValue += resource.getValue();
        }

        return fitness/totalValue;
    }

    /**
     * Calculates the fitness value according to the average distance between unconnected resources and the largest construction zone
     * @return fitness value
     */
    public double getAveRobotDistance() {
        double aveDist = 0D;
        ResourceObject firstResource = resources.get(0);
        double [] robotDistances = new double [robots.size()];
        double sumDistances = 0D;
        Vec2 frPos = firstResource.getBody().getPosition();
        int index = 0;

        //initiliaze distances with each robot's distance to firstResource
        for (RobotObject r : robots) {
            Vec2 robotPos = r.getBody().getPosition();
            Vec2 dist = robotPos.add(frPos.negate());
            robotDistances[index] = Math.sqrt(Math.pow(dist.x,2) + Math.pow(dist.y,2));
        }
        for (RobotObject r : robots) {
            Vec2 robotPos = r.getBody().getPosition();
            int cnt = 0;
            for (ResourceObject res : resources) {
                if (cnt > 0) {
                    Vec2 resPos = res.getBody().getPosition();
                    Vec2 dist = robotPos.add(resPos.negate());
                    double distBetween = Math.sqrt(Math.pow(dist.x,2) + Math.pow(dist.y,2));
                    if (distBetween < robotDistances[index]) {
                        robotDistances[index] = distBetween;
                    }
                }
            }
            sumDistances += robotDistances[index];
        }
        return sumDistances/(double)robots.size();
    }

    public ArrayList<ResourceObject> getGlobalConstructionOrder() {
        return globalConstructionOrder;
    }

    /**
     * Returns all of the construction zones
     * @return list of construction zones
     */
    public ArrayList<ConstructionZone> getConstructionZones() {
        ArrayList<ConstructionZone> returnedConstructionZones = new ArrayList<ConstructionZone>();
        //If there were no CZs created
        if (constructionZones.size()==0) {
            returnedConstructionZones.add(new ConstructionZone(0));
        }
        else {
            for(ConstructionZone cz : constructionZones){
                returnedConstructionZones.add(cz);
            }
        }

        return returnedConstructionZones;
    }

    public int getTotalResourcesConnected(){
        int total = 0;
        for (ConstructionZone cz: constructionZones) {
            total += cz.getNumberOfConnectedResources();
        }
        return total;
    }

    /**
     * Return the number of constructed resources in the environment
     * @return int value
     */
    public int getAdjacentPerformance(){
        ArrayList<ResourceObject> gridResources = discreteGrid.getAllResources();
        int nTotalResources = gridResources.size();
        return nTotalResources;
    }

    /**
     * Returns the value of the largest construction zone
     * @return int value
     */
    public int getConstructionPerformance(){
        return finalConstructionZone.getValue();
    }

    /**
     * Calculates the fitness value according to the average distance between unconnected resources and the largest construction zone
     * @return fitness value
     */
    public double calculateAverageDistance(){
        double maxDistance = 20*Math.sqrt(2);

        if(constructionZones.size()>0){
            finalConstructionZone.updateCZCenter();
            Vec2 centerPos = finalConstructionZone.getCenter();
            ArrayList<ResourceObject> connectedResources = finalConstructionZone.getConstructionOrder();

            double averageDistance = 0;
            int i = 0;
            for(ResourceObject res : resources){
                if(!connectedResources.contains(res)){
                    double distance = centerPos.sub(res.getBody().getPosition()).length();
                    averageDistance += distance;
                    i++;
                }
            }
            averageDistance = (double)averageDistance/i;
            double normalisedValue = averageDistance/maxDistance;
            return normalisedValue;
        }
        else{
            return maxDistance;
        }
    }

    /**
     * Calculate fitness value based on resources being constructed to other resources
     * @return normalized fitness value
     */
    public double calculateAdjacentFitness(){
        ArrayList<ResourceObject> gridResources = discreteGrid.getAllResources();
        int nTotalResources = gridResources.size();
        double fitness = (double) nTotalResources/(double)resources.size();
        return fitness;
    }

    /**
     * Calcualte fitness value based on the distance between robots and their closest resource
     * @return normalized fitness value
     */
    public double calculateAverageRobotDistanceFitness(){
        double averageDistance = 0;
        double maxDistance = 30*Math.sqrt(2);
        for(RobotObject robot : robots){
            float smallestDistance = 10000;
            for(ResourceObject resource : resources){
                float distanceBetween = robot.getBody().getPosition().sub(resource.getBody().getPosition()).length();
                if(distanceBetween <= smallestDistance){
                    smallestDistance = distanceBetween;
                }
            }
            averageDistance += smallestDistance;
        }
        averageDistance = averageDistance/robots.size();
        return (maxDistance - averageDistance)/maxDistance;
    }
}
