package za.redbridge.simulator;

import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.collision.shapes.MassData;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Arrays;
import sim.util.Double2D;
import java.util.Random;

import sim.engine.SimState;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.Collideable;
import za.redbridge.simulator.physics.FilterConstants;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;
import za.redbridge.simulator.object.*;
import za.redbridge.simulator.config.SchemaConfig;

import static za.redbridge.simulator.physics.AABBUtil.getAABBHeight;
import static za.redbridge.simulator.physics.AABBUtil.getAABBWidth;
import static za.redbridge.simulator.physics.AABBUtil.resizeAABB;

/*
 *  A construction zone object. Contains details and methods about a single construction zone.
 *
 */

public class ConstructionZone {

    //hash set so that object values only get added to the construction zone once
    private final Set<ResourceObject> connectedResources = new HashSet<>();

    private final ArrayList<ResourceObject> resOrder;  //update size to be the config's resource size
    private Vec2 czPos;

    private int resource_count = 0;
    private int ACount = 0;
    private int BCount = 0;
    private int CCount = 0;
    private final int czNum;
    private final Color czColor;
    private int value;

    // create an empty construction zone
    public ConstructionZone (int czNum) {
        this.czNum = czNum;
        resOrder = new ArrayList<>();
        Random rand = new Random();
        int randVal1 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal2 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal3 = rand.nextInt((255 - 1) + 1) + 1;
        czColor = new Color(255 - randVal1, 255 - randVal2, 255 - randVal3);
    }

    // create a construction zone from a list of resources
    public ConstructionZone(ArrayList<ResourceObject> updatedResources, int czNum) {
        this.czNum = czNum;
        resOrder = new ArrayList<>();

        Random rand = new Random();
        int randVal1 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal2 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal3 = rand.nextInt((255 - 1) + 1) + 1;
        czColor = new Color(255 - randVal1, 255 - randVal2, 255 - randVal3);

        for (ResourceObject r : updatedResources) {
            addResource(r, true);
        }
    }

    /**
     * Starts a construction zone between two resources
     * @param  r1 the first resource
     * @param  r2 the second resource
     */
    public void startConstructionZone(ResourceObject r1, ResourceObject r2) {
        Vec2 r1Pos = r1.getBody().getPosition();
        Vec2 r2Pos = r2.getBody().getPosition();
        float aveX = (r1Pos.x + r2Pos.x)/2;
        float aveY = (r1Pos.y + r2Pos.y)/2;
        czPos = new Vec2(aveX, aveY);

        addResource(r1, true);
        addResource(r2, true);
    }

    /**
     * Updates the center position of the construction zone
     */
    public void updateCZCenter() {
        float x = 0;
        float y = 0;
        for (ResourceObject res : resOrder) {
            x += (res.getBody().getPosition().x);
            y += (res.getBody().getPosition().y);
        }
        czPos = new Vec2((float)(x/resOrder.size()), (float)(y/resOrder.size()));
    }

    /**
     * Adds a resource to the construction zone
     * @param  resource the resource to be added
     * @param  isFirstConnection if it is the first two resources to be connected
     */
    public void addResource(ResourceObject resource, boolean isFirstConnection) {
        double FResource = 0D;
        if (connectedResources.add(resource)) {
            if (isFirstConnection) {
                if(resource.getValue() > 0) {
                    resOrder.add(resource);
                    resource_count++;
                }

                if (resource.getType().equals("A")) {
                    ACount++;
                }
                else if (resource.getType().equals("B")) {
                    BCount++;
                }
                else if (resource.getType().equals("C")) {
                    CCount++;
                }

                resource.setConstructed();
                resource.setCzNumber(czNum);
                resource.getPortrayal().setPaint(czColor);
                resource.getBody().setType(BodyType.STATIC);
            }
            else {
                if (resource.pushedByMaxRobots()) {
                    if(resource.getValue() > 0) {
                        resOrder.add(resource);
                        resource_count++;
                    }

                    if (resource.getType().equals("A")) {
                        ACount++;
                    }
                    else if (resource.getType().equals("B")) {
                        BCount++;
                    }
                    else if (resource.getType().equals("C")) {
                        CCount++;
                    }

                    resource.setConstructed();
                    resource.setCzNumber(czNum);
                    resource.getPortrayal().setPaint(czColor);
                    resource.getBody().setType(BodyType.STATIC);
                }
            }
        }
    }

    /**
     * Gets the value of the construction zone based on the type of resources
     * @return int value
     */
    public int getValue(){
        int value = 0;
        for(ResourceObject resource : resOrder){
            value += resource.getValue();
        }
        return value;
    }

    public List<ResourceObject> updateCZNumber(int newCZNum) {
        List<ResourceObject> returnResources = new ArrayList<>();
        for (ResourceObject r : resOrder) {
            r.setCzNumber(newCZNum);
            returnResources.add(r);
        }
        return returnResources;
    }

    public void addNewResources (List<ResourceObject> newResources) {
        for (ResourceObject r : newResources) {
            addResource(r, true);
        }
    }

    /**
     * Clears the construction zone
     */
    public void clearCZ() {
        connectedResources.clear();
        resOrder.clear();
        resource_count = 0;
        ACount = 0;
        BCount = 0;
        CCount = 0;
        czPos = null;
    }

    public Vec2 getCZPos () {
        return czPos;
    }

    public Set<ResourceObject> getConnectedResources () {
        return connectedResources;
    }

    public ArrayList<ResourceObject> getConstructionOrder () {
        return resOrder;
    }

    public boolean isInConstructionZone(ResourceObject r) {
        if (connectedResources.contains(r)) {
            return true;
        }
        else {
            return false;
        }
    }

    public int getNumberOfConnectedResources() {
        return resource_count;
    }

    /**
     * Returns the number of correctly connected resources according to the schema
     * @param   schema the schema configuration
     * @param   configNum the number of the schema
     * @return  number of resources 
     */
    public int getNumCorrectlyConnected (SchemaConfig schema, int configNum) {
        int numCorrect = 0;
        for (ResourceObject res : connectedResources) {
            String [] adjacent = res.getAdjacentList();
            if (schema.checkConfig(configNum, res.getType(), adjacent) == 4) {
                numCorrect++;
            }
        }
        return numCorrect;
    }

    /**
    Calculates the Fcorr for this construction zone:
        Fcorr = ((#correct sides/#shared sides) per block in CZ)/#simulation resources
    **/
    public double getCZCorrectness(SchemaConfig schema, int configNum) {
        double correctness = 0D;
        for (ResourceObject res : connectedResources) {
            String[] adjacent = res.getAdjacentList();
            int sharedSides = 0;  //counter for the number of sides this resource shares with other resources
            for (int i = 0; i < adjacent.length; i++) {
                if (adjacent[i] != "_") {
                    sharedSides++;
                }
            }

            correctness += schema.checkConfig(configNum, res.getType(), adjacent)/(double)sharedSides;
        }
        return correctness;
    }

    public int[] getResTypeCount() {
        int [] typeCount = {ACount, BCount, CCount};
        return typeCount;
    }

    public double getTotalResourceValue() {
        double totalValue = 0D;
        for (ResourceObject res : connectedResources) {
            totalValue += res.getValue();
        }
        return totalValue;
    }

    public static int [] getOverallTypeCount (ConstructionZone[] czs) {
        int[] typeCount = new int[3];
        for (int i = 0;  i < czs.length; i++) {
            int[] czTypeCount = czs[i].getResTypeCount();
            typeCount[0] += czTypeCount[0];
            typeCount[1] += czTypeCount[1];
            typeCount[2] += czTypeCount[2];
        }
        return typeCount;
    }

    public Color getCZColor() {
        return czColor;
    }

    public static ArrayList<ResourceObject> getOverallConstructionOrder (ArrayList<ConstructionZone> czs) {
        ArrayList<ResourceObject> overallOrder = new ArrayList<>();
        for (int i = 0; i < czs.size(); i++) {
            for (ResourceObject r : czs.get(i).getConstructionOrder()) {
                overallOrder.add(r);
            }
        }
        return overallOrder;
    }

    public Vec2 getCenter(){
        return this.czPos;
    }
}
