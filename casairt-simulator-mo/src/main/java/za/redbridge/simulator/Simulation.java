package za.redbridge.simulator;

import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

import java.util.Set;
import org.encog.ml.data.MLData;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Double2D;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.factories.ResourceFactory;
import za.redbridge.simulator.object.PhysicalObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.object.TargetAreaObject;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.WallObject;
import za.redbridge.simulator.physics.SimulationContactListener;
import za.redbridge.simulator.portrayal.DrawProxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import za.redbridge.simulator.config.SchemaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main simulation state.
 *
 */
public class Simulation extends SimState {

    private static final float VELOCITY_THRESHOLD = 0.000001f;
    public static final float DISCR_GAP = 0.25f;

    private Continuous2D environment;
    private SparseGrid2D constructionEnvironment;
    private World physicsWorld;
    private PlacementArea placementArea;
    private DrawProxy drawProxy;

    private final SimulationContactListener contactListener = new SimulationContactListener();

    private static final float TIME_STEP = 1f / 10f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 3;

    private TargetAreaObject targetArea;
    private RobotFactory robotFactory;
    private ConstructionTask construction;
    private SchemaConfig schema;
    private final SimConfig config;
    private ResourceFactory resourceFactory;

    private boolean stopOnceCollected = false;
    private ContToDiscrSpace discr;

    private static final Logger log = LoggerFactory.getLogger(Simulation.class);

    public Simulation(SimConfig config, RobotFactory robotFactory, ResourceFactory resourceFactory, SchemaConfig schema) {
        super(config.getSimulationSeed());
        this.config = config;
        this.robotFactory = robotFactory;
        Settings.velocityThreshold = VELOCITY_THRESHOLD;
        this.resourceFactory = resourceFactory;
        this.schema = schema;
    }

    // starts the simulation, creating the environment and placing all objects
    @Override
    public void start() {
        super.start();

        environment = new Continuous2D(1.0, config.getEnvironmentWidth(), config.getEnvironmentHeight());
        drawProxy = new DrawProxy(environment.getWidth(), environment.getHeight());
        environment.setObjectLocation(drawProxy, new Double2D());

        physicsWorld = new World(new Vec2());
        placementArea = new PlacementArea((float) environment.getWidth(), (float) environment.getHeight());
        placementArea.setSeed(System.currentTimeMillis());
        schedule.reset();
        System.gc();

        physicsWorld.setContactListener(contactListener);

        // Create ALL the objects
        createWalls();

        robotFactory.placeInstances(placementArea.new ForType<>(), physicsWorld, config.getTargetAreaPlacement());
        discr = new ContToDiscrSpace(20,20,1D,1D, DISCR_GAP, schema, config.getConfigNumber());
        resourceFactory.setResQuantity(schema.getResQuantity(config.getConfigNumber()));
        resourceFactory.placeInstances(placementArea.new ForType<>(), physicsWorld);
        construction = new ConstructionTask(schema,resourceFactory.getPlacedResources(),robotFactory.getPlacedRobots(),physicsWorld, config.getConfigNumber(), environment.getWidth(), environment.getHeight());

        // Now actually add the objects that have been placed to the world and schedule
        for (PhysicalObject object : placementArea.getPlacedObjects()) {
            drawProxy.registerDrawable(object.getPortrayal());
            schedule.scheduleRepeating(object);
        }

        schedule.scheduleRepeating(construction);
        schedule.scheduleRepeating(simState -> physicsWorld.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS));
    }

    @Override
    public void finish(){
        super.finish();
    }

    // Walls are simply added to environment since they do not need updating
    private void createWalls() {
        int environmentWidth = config.getEnvironmentWidth();
        int environmentHeight = config.getEnvironmentHeight();
        // Left
        Double2D pos = new Double2D(0, environmentHeight / 2.0);
        Double2D v1 = new Double2D(0, -pos.y);
        Double2D v2 = new Double2D(0, pos.y);
        WallObject wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Right
        pos = new Double2D(environmentWidth, environmentHeight / 2.0);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Top
        pos = new Double2D(environmentWidth / 2.0, 0);
        v1 = new Double2D(-pos.x, 0);
        v2 = new Double2D(pos.x, 0);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Bottom
        pos = new Double2D(environmentWidth / 2.0, environmentHeight);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());
    }

    // sets the seed for the simulator. allows for resources and robots to be placed randomly each time
    @Override
    public void setSeed(long seed) {
        super.setSeed(seed);
        config.setSimulationSeed(seed);
    }

    private double getRobotAvgPolygonArea() {
        Set<PhysicalObject> objects = placementArea.getPlacedObjects();
        double totalArea = 0.0;

        for (PhysicalObject object: objects) {
            if (object instanceof RobotObject) {
                totalArea += ((RobotObject) object).getAverageCoveragePolgygonArea();
            }
        }
        return totalArea/config.getObjectsRobots();
    }

    /** Get the environment (forage area) for this simulation. */
    public Continuous2D getEnvironment() {
        return environment;
    }

    /**
     * Run the simulation for the number of iterations specified in the config.
     */
    public void run() {
        final int iterations = config.getSimulationIterations();
        runForNIterations(iterations);
    }

    /**
     * Run the simulation for a certain number of iterations.
     * @param n the number of iterations
     */
    public void runForNIterations(int n) {
        start();
        for (int i = 0; i < n; i++) {
            schedule.step(this);
        }
        finish();
    }

    /** If true, this simulation will stop once all the resource objects have been collected. */
    public boolean isStopOnceCollected() {
        return stopOnceCollected;
    }

    /** If set true, this simulation will stop once all the resource objects have been collected. */
    public void setStopOnceCollected(boolean stopOnceCollected) {
        this.stopOnceCollected = stopOnceCollected;
    }

    public SparseGrid2D getConstructionEnvironment() {
        return constructionEnvironment;
    }

    public ContToDiscrSpace getDiscreteGrid() {
        return discr;
    }

    /** Gets the progress of the simulation as a percentage */
    public double getProgressFraction() {
        return (double) schedule.getSteps() / config.getSimulationIterations();
    }

    /** Get the number of steps this simulation has been run for. */
    public long getStepNumber() {
        return schedule.getSteps();
    }

    /**
     * Launching the application from this main method will run the simulation in headless mode.
     */
    public static void main (String[] args) {
        doLoop(Simulation.class, args);
        System.exit(0);
    }

    // calculates the fitness for each objective and returns the values.
    public double [] getFitnessObjectives(){
        construction.updateConstructionZones();
        construction.getBestConstructionZone();
        double [] fitnessValues = new double[3];
        fitnessValues[0] = construction.calculateAdjacentFitness();
        fitnessValues[1] = construction.calculateConstructionZoneFitness();
        fitnessValues[2] = construction.calculateAverageDistance();
        return fitnessValues;
    }

    public double getFitness(){
        return 0.0;
    }

    // merges construction zones and sets the best one before calculating performance
    public void prePerformance(){
        construction.updateConstructionZones();
        construction.getBestConstructionZone();
    }

    // returns the performance for the number of constructed resources
    public int getAdjacentPerformance(){
        return construction.getAdjacentPerformance();
    }

    // returns the performance for the largest construction
    public int getConstructionPerformance(){
        return construction.getConstructionPerformance();
    }
}
