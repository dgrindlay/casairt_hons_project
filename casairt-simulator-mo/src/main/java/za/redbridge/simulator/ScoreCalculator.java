package za.redbridge.simulator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.encog.ml.CalculateScore;
import org.encog.ml.MLMethod;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.NEATNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import sim.display.Console;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HomogeneousRobotFactory;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.factories.ConfigurableResourceFactory;
import za.redbridge.simulator.factories.ResourceFactory;
import za.redbridge.simulator.phenotype.Phenotype;
import za.redbridge.simulator.neat.NEATPhenotype;
import za.redbridge.simulator.config.SchemaConfig;
import za.redbridge.simulator.Evaluation;

// runs an entire simulation with a specific network and calculates the fitness
public class ScoreCalculator implements CalculateScore{

    private final SimConfig simConfig;
    private final SchemaConfig schema;
    private final int simRuns;
    private final Morphology morphology;

    private final DescriptiveStatistics adjacentPerformanceStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics constructionPerformanceStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics fitnessStats = new SynchronizedDescriptiveStatistics();

    private final DescriptiveStatistics objectiveOneFitnessStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics objectiveTwoFitnessStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics objectiveThreeFitnessStats = new SynchronizedDescriptiveStatistics();

    private static final Logger log = LoggerFactory.getLogger(ScoreCalculator.class);

    public ScoreCalculator(SimConfig simConfig, SchemaConfig schema ,int simRuns, Morphology morphology){
        this.simConfig = simConfig;
        this.simRuns = simRuns;
        this.morphology = morphology;
        this.schema = schema;
    }

    /**
     * calculates the score for a controller by running it in a simulation instance.
     * Uses NEAT neuroevolution method for single objective experiments
     * @param  method the genome/network controller
     * @return        score value for controller
     */
    @Override
    public double calculateScore(MLMethod method){
        long start = System.nanoTime();
        NEATNetwork network = (NEATNetwork) method;
        RobotFactory robotFactory = new HomogeneousRobotFactory(new NEATPhenotype(morphology, network),
                    simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                    simConfig.getObjectsRobots());

        // create new configurable resource factory
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, schema);

        // opens simulation GUI window
        // SimulationGUI video = new SimulationGUI(simulation);
        // Console console = new Console(video);
        // console.setVisible(true);
        // video.start();

        double score = 0;

        double fitness = 0;
        double adjacentPerformance = 0;
        double constructionPerformance = 0;
        for(int i=0;i<simRuns;i++){
            // log.info("simulation run - "+i+" out of "+simRuns);
            simulation.run();
            double tempFitness = simulation.getFitness();
            double tempAdjacentPerformance = simulation.getAdjacentPerformance();
            double tempConstructionPerformance = simulation.getConstructionPerformance();

            fitness += tempFitness;
            adjacentPerformance += tempAdjacentPerformance;
            constructionPerformance += tempConstructionPerformance;

            // fitness += 20 * (1.0 - simulation.getProgressFraction());
        }

        double finalFitness = fitness/simRuns;
        double finalAdjacentPerformance = adjacentPerformance/simRuns;
        double finalConstructionPerformance = constructionPerformance/simRuns;

        fitnessStats.addValue(finalFitness);
        adjacentPerformanceStats.addValue(finalAdjacentPerformance);
        constructionPerformanceStats.addValue(finalConstructionPerformance);

        // long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        // performanceStats.addValue(duration);
        return score;
    }

    /**
     * Calculates the score for a controller by running it in a simulation instance.
     * Uses MO-NEAT neuroevolution method for multi-objective experiments
     * @param  method genome/controller that is being tested
     * @return        list of scores for each objective
     */
    public ArrayList<Double> calculateMultipleScores(MLMethod method){
        NEATNetwork network = (NEATNetwork) method;
        RobotFactory robotFactory = new HomogeneousRobotFactory(new NEATPhenotype(morphology, network),
                    simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                    simConfig.getObjectsRobots());

        // create new configurable resource factory
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, schema);

        int nObjectives = simConfig.getNumberOfObjectives();
        ArrayList<Double> fitnessValues = new ArrayList<Double>();
        for(int i=0;i<nObjectives;i++){
            fitnessValues.add(0.0);
        }

        double objectiveOneFitness = 0;
        double objectiveTwoFitness = 0;
        double objectiveThreeFitness = 0;

        double adjacentPerformance = 0;
        double constructionPerformance = 0;

        for(int i=0;i<simRuns;i++){
            // run the simulation
            simulation.run();

            // get the fitness value for each objective
            double [] tempFitness = simulation.getFitnessObjectives();
            for(int j=0;j<nObjectives;j++){
                fitnessValues.set(j, fitnessValues.get(j) + (Double) tempFitness[j]);
            }

            // get the performance values of the simulation run
            double tempAdjacentPerformance = simulation.getAdjacentPerformance();
            double tempConstructionPerformance = simulation.getConstructionPerformance();

            adjacentPerformance += tempAdjacentPerformance;
            constructionPerformance += tempConstructionPerformance;

            // fitness += 20 * (1.0 - simulation.getProgressFraction()); // if you want to add a time bonus
        }

        // average the fitness and performance values over the simulation runs
        for(int i=0;i<fitnessValues.size();i++){
            fitnessValues.set(i, fitnessValues.get(i)/simRuns);
        }

        double finalAdjacentPerformance = adjacentPerformance/simRuns;
        double finalConstructionPerformance = constructionPerformance/simRuns;

        // add the fitness and performance values to the statistics
        if(nObjectives==1){
            objectiveOneFitnessStats.addValue(fitnessValues.get(0));
        }
        else if(nObjectives==2){
            objectiveOneFitnessStats.addValue(fitnessValues.get(0));
            objectiveTwoFitnessStats.addValue(fitnessValues.get(1));
        }
        else if(nObjectives==3){
            objectiveOneFitnessStats.addValue(fitnessValues.get(0));
            objectiveTwoFitnessStats.addValue(fitnessValues.get(1));
            objectiveThreeFitnessStats.addValue(fitnessValues.get(2));
        }

        adjacentPerformanceStats.addValue(finalAdjacentPerformance);
        constructionPerformanceStats.addValue(finalConstructionPerformance);
        return fitnessValues;
    }

    /**
     * Demos a controller in a simulated Environment
     * @param method genome/controller being tested
     */
    public void demo(MLMethod method) {
        NEATNetwork network = (NEATNetwork) method;
        RobotFactory robotFactory = new HomogeneousRobotFactory(new NEATPhenotype(morphology, network),
                    simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                    simConfig.getObjectsRobots());

        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, schema);
        SimulationGUI video = new SimulationGUI(simulation);

        Console console = new Console(video);
        console.setVisible(true);
    }

    /**
     * Evaluates a controller in a simulated environment
     * @param  method genome/controler being tested
     * @param  eval evaluation class that saves statistics to files
     */
    public void evaluate(MLMethod method, Evaluation eval){
        NEATNetwork network = (NEATNetwork) method;
        RobotFactory robotFactory = new HomogeneousRobotFactory(new NEATPhenotype(morphology, network),
                    simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                    simConfig.getObjectsRobots());

        // create new configurable resource factory
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, schema);

        double adjacentPerformance = 0;

        for(int i=0;i<20;i++){
            // run the simulation
            simulation.run();
            simulation.prePerformance();
            // get the performance values of the simulation run
            double tempAdjacentPerformance = simulation.getAdjacentPerformance();
            eval.saveEvaluationStats(tempAdjacentPerformance);
            adjacentPerformance += tempAdjacentPerformance;
        }
    }

    public DescriptiveStatistics getAdjacentPerformanceStatistics() {
        return adjacentPerformanceStats;
    }

    public DescriptiveStatistics getConstructionPerformanceStatistics() {
        return constructionPerformanceStats;
    }

    public DescriptiveStatistics getObjectiveOneStatistics() {
        return objectiveOneFitnessStats;
    }

    public DescriptiveStatistics getObjectiveTwoStatistics() {
        return objectiveTwoFitnessStats;
    }

    public DescriptiveStatistics getObjectiveThreeStatistics() {
        return objectiveThreeFitnessStats;
    }

    @Override
    public boolean shouldMinimize() {
        return true;
    }

    @Override
    public boolean requireSingleThreaded() {
        return false;
    }
}
