package za.redbridge.simulator.object;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.common.Transform;

import java.awt.Color;
import java.awt.Paint;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;

import sim.engine.SimState;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.FilterConstants;
import za.redbridge.simulator.portrayal.PolygonPortrayal;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.DPPortrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;
import za.redbridge.simulator.portrayal.STRTransform;
import za.redbridge.simulator.physics.AABBUtil;
import sim.engine.Steppable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import sim.util.Double2D;


/**
 * Detection class used to identify what is around each resource. Each resource contains multiple detection points are
 * their bodies.
 */
public class DetectionPoint implements Steppable{
    private final Vec2 [] positions;
    private boolean collided;
    private Vec2 worldPosition = null;
    private final Body body;
    private Portrayal portrayal;
    private int posNum;
    private final int side;

    public DetectionPoint(Vec2 [] positions, Body resBody, int posNum, int sideNum){
        this.positions = positions;
        this.body = resBody;
        double width = (double)Math.abs(positions[1].x - positions[2].x);
        double height = 0.1D;
        portrayal = createPortrayal(width, height);
        this.posNum = posNum;
        this.side = sideNum;
    }

    public DetectionPoint(DetectionPoint dp, int posNum) {
        this.positions = dp.getPositions();
        this.body = dp.getBody();
        portrayal = dp.getPortrayal();
        this.posNum = posNum;
        this.side = dp.getSide();
    }

    public void markColliding() {
        collided = true;
    }

    public Vec2[] getPositions() {
        return positions;
    }

    public int getSide() {
        return side;
    }

    public Vec2 [] getRelativePositions(){
        Vec2 [] relativePositions = new Vec2 [3];
        Transform bodyXFos = body.getTransform();
        for (int i = 0; i < positions.length; i++) {
            relativePositions[i] = Transform.mul(bodyXFos, positions[i]);
        }
        return relativePositions;
    }

    public Body getBody() {
        return body;
    }

    @Override
    public void step(SimState simState) {
        Simulation s = (Simulation) simState;
        portrayal.setTransform(body.getTransform());
        Vec2[] relPos = getRelativePositions();

        // These lines register the object's position to the model so that the MASON portrayals move with the simulator objects
        float objX = relPos[posNum].x;
        float objY = (float)s.getEnvironment().getHeight() - relPos[posNum].y;

        s.getEnvironment().setObjectLocation(this, new Double2D(objX, objY));
    }

    public boolean isNearCenter(Vec2 otherResPos) {
        Vec2[] relPos = getRelativePositions();
        float distBetween = relPos[0].sub(otherResPos).length();
        if (distBetween < (0.1f + Simulation.DISCR_GAP)) {
            return true;
        }
        else {
            return false;
        }
    }

    public Vec2 [] getWorldPositions(ResourceObject r) {
        Vec2 [] worldPositions = new Vec2 [3];
        if (worldPosition == null) {
            for (int i = 0; i < positions.length; i++) {
                worldPosition = r.getBody().getWorldPoint(positions[i]);
            }

        }
        return worldPositions;
    }

    public boolean isTaken() {
        return collided;
    }

    public Portrayal getPortrayal() {
        return portrayal;
    }

    public void setPosNum (int i) {
        this.posNum = i;
    }

    protected Portrayal createPortrayal(double width, double height) {

        return new DPPortrayal(width, height);
    }
}
