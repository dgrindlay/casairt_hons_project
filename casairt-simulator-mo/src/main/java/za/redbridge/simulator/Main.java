package za.redbridge.simulator;

import org.encog.Encog;
import org.encog.ml.MLMethod;
import org.encog.ml.MethodFactory;
import org.encog.ml.ea.train.basic.TrainEA;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.networks.BasicNetwork;

import org.encog.neural.neat.NEATPopulation;

import za.redbridge.simulator.neat.NEATUtil;
import za.redbridge.simulator.config.MorphologyConfig;
import za.redbridge.simulator.ScoreCalculator;
import za.redbridge.simulator.StatsRecorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.khepera.KheperaIIIPhenotype_simple;

import static za.redbridge.simulator.Utils.isBlank;
import static za.redbridge.simulator.Utils.readObjectFromFile;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import sim.display.Console;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HomogeneousRobotFactory;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.phenotype.Phenotype;
import za.redbridge.simulator.neat.NEATPhenotype;
import za.redbridge.simulator.phenotype.ChasingPhenotype_simple;
import za.redbridge.simulator.config.ExperimentConfig;
import za.redbridge.simulator.moneat.*;
import za.redbridge.simulator.config.SchemaConfig;

public class Main{

    private static String simulationConfigPath = "configs/simConfig.yml";
    private static String experimentConfigPath = "configs/experimentConfig.yml";
    private static String morphologyConfigPath = "configs/morphologyConfig.yml";
    private static String schemaConfigPath = "configs/schemaConfig.yml";

    public static int thread_count = 0;
    public static String RES_CONFIG;
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, ParseException {
        int configNumber = 0;
        int nIterations = 1000;

        // experiment configuration
        ExperimentConfig experimentConfig;
        if (!isBlank(experimentConfigPath)) {
            experimentConfig = new ExperimentConfig(experimentConfigPath);
        } else {
            experimentConfig = new ExperimentConfig();
        }

        // read in simulation config
        SimConfig simConfig;
        if (!isBlank(simulationConfigPath)) {
            simConfig = new SimConfig(simulationConfigPath);
        } else {
            simConfig = new SimConfig();
        }

        SchemaConfig schema = new SchemaConfig("configs/schemaConfig.yml", 10, 3);

        simConfig.setConfigNumber(configNumber);
        simConfig.setSimulationIterations(nIterations);

        // read morphology for creating phenotypes
        MorphologyConfig morphConfig = new MorphologyConfig(morphologyConfigPath);
        ScoreCalculator calculateScore = new ScoreCalculator(simConfig, schema, experimentConfig.getSimulationRuns(), morphConfig.getMorphology(experimentConfig.getMorphologyNumber()));

        // train a population of controllers
        trainMultiObjectivePopulation(simConfig ,experimentConfig, morphConfig, calculateScore);

        // evaluate(calculateScore, "results/Daniel-Laptop-20161108T1159/best networks/epoch-2/network1.ser");
    }

    /**
     * Demos a controller by running the simulator in a GUI
     * @param  calculateScore ScoreCalculator class to create simulation instance
     * @param  path File path to the saved genome
     */
    private static void demo(ScoreCalculator calculateScore, String path){
        log.info("demo network: "+path);
        NEATNetwork network = (NEATNetwork) readObjectFromFile(path);
        calculateScore.demo(network);
    }

    // train a population of genomes using multiple objective functions
    /**
     * Train a population of genomes using MO-NEAT
     * @param  simConfig simulation config parameters
     * @param  experimentConfig experiment config parameters
     * @param  morphConfig sensor configuration for robots
     * @param  calculateScore creates instance of simulations
     */
    private static void trainMultiObjectivePopulation(SimConfig simConfig ,ExperimentConfig experimentConfig,MorphologyConfig morphConfig, ScoreCalculator calculateScore){

        // initialise objectives
        ArrayList<Objective> objectives = new ArrayList<Objective>();
        if(simConfig.getNumberOfObjectives()==1){
            objectives.add(new Objective("Adjacent", true));
        }
        else if(simConfig.getNumberOfObjectives()==2){
            objectives.add(new Objective("Adjacent", true));
            objectives.add(new Objective("Schema", true));
        }
        else{
            objectives.add(new Objective("Adjacent", true));
            objectives.add(new Objective("Schema", true));
            objectives.add(new Objective("ResourceDistance", false));
        }

        final MultiObjectivePopulation population;
        population = new MultiObjectivePopulation(morphConfig.getMorphology(experimentConfig.getMorphologyNumber()).getNumSensors(), 2, experimentConfig.getPopulationSize(), objectives);
        population.setInitialConnectionDensity(experimentConfig.getConnectionDensity());
        population.reset();

        final MultiObjectivePopulation archive;
        archive = new MultiObjectivePopulation(morphConfig.getMorphology(experimentConfig.getMorphologyNumber()).getNumSensors(), 2, experimentConfig.getPopulationSize(), objectives);
        archive.setInitialConnectionDensity(experimentConfig.getConnectionDensity());
        archive.reset();

        log.info("population size: "+ population.getPopulationSize());
        log.info("archive size: "+experimentConfig.getArchiveSize());
        log.info("number of generations: "+experimentConfig.getGenerationNumber());
        log.info("simulation runs: "+ experimentConfig.getSimulationRuns());

        MultiObjectiveTrainEA train;
        train = NEATUtil.constructMultiObjectiveTrainer(population, archive, calculateScore, experimentConfig.getArchiveSize(), experimentConfig.getAddNodeProbability(),
            experimentConfig.getAddLinkProbability(), experimentConfig.getRemoveLinkProbability(), experimentConfig.getCrossoverProbability());

        log.info("initial population density: "+ experimentConfig.getConnectionDensity());
        log.info("add node probability: "+ experimentConfig.getAddNodeProbability());
        log.info("add link probability: "+ experimentConfig.getAddLinkProbability());
        log.info("remove link probability: "+ experimentConfig.getRemoveLinkProbability());
        log.info("crossover probability: "+ experimentConfig.getCrossoverProbability());

        if(thread_count > 0){
            train.setThreadCount(thread_count);
        }

        System.out.println();

        final StatsRecorder stats = new StatsRecorder(train, calculateScore);
        stats.recordIterationStats();
        for(int i=train.getIteration();i<experimentConfig.getGenerationNumber();i++){
            train.iteration();
            stats.recordIterationStats();
        }

        log.info("training complete");
        Encog.getInstance().shutdown();
    }

    /**
     * Evaluate a controller, performance is based on the number of constructed resources
     * @param  calculateScore creates simulation instance
     * @param  path file path to genome
     */
    public static void evaluate(ScoreCalculator calculateScore, String path){
        log.info("Evaluating network: "+ path);
        Evaluation eval = new Evaluation();
        NEATNetwork network = (NEATNetwork) readObjectFromFile(path);
        calculateScore.evaluate(network, eval);
    }
}
