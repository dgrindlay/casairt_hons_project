package za.redbridge.simulator.config;

import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/*
 *  Reads in experiment parameters from a file and stores them to be used to create the simulator
 *
 */
public class ExperimentConfig extends Config {
    private static final int DEFAULT_CONFIG_NUMBER = 0;
    private static final int DEFAULT_MORPHOLOGY_NUMBER = 0;
    private static final int DEFAULT_SIMULATION_STEPS = 100;
    private static final int DEFAULT_SIMULATION_RUNS = 2;
    private static final int DEFAULT_POPULATION_SIZE = 20;
    private static final int DEFAULT_GENERATIONS = 10;
    private static final int DEFAULT_ARCHIVE_SIZE = 10;

    private static final double DEFAULT_ADD_NODE_PROBABILITY = 0.001;
    private static final double DEFAULT_ADD_LINK_PROBABILITY = 0.005;
    private static final double DEFAULT_REMOVE_LINK_PROBABILITY = 0.001;
    private static final double DEFAULT_CROSSOVER_PROBABILITY = 0.5;
    private static final double DEFAULT_CONECTION_DENSITY = 0.5;

    public enum FitnessFunction{
        OBJECTIVE, MULTIOBJECTIVE;
    }

    private int configNumber;
    private int morphologyNumber;
    private int simulationSteps;
    private int simulationRuns;
    private int populationSize;
    private int generations;
    private int archiveSize;

    private double addNode;
    private double addLink;
    private double removeLink;
    private double crossover;
    private double connectionDensity;

    public ExperimentConfig() {
        this.configNumber = DEFAULT_CONFIG_NUMBER;
        this.morphologyNumber = DEFAULT_MORPHOLOGY_NUMBER;
        this.simulationSteps = DEFAULT_SIMULATION_STEPS;
        this.simulationRuns = DEFAULT_SIMULATION_RUNS;
        this.populationSize = DEFAULT_POPULATION_SIZE;
        this.generations = DEFAULT_GENERATIONS;
        this.archiveSize = DEFAULT_ARCHIVE_SIZE;

        this.addNode = DEFAULT_ADD_NODE_PROBABILITY;
        this.addLink = DEFAULT_ADD_LINK_PROBABILITY;
        this.removeLink = DEFAULT_REMOVE_LINK_PROBABILITY;
        this.crossover = DEFAULT_CROSSOVER_PROBABILITY;
        this.connectionDensity = DEFAULT_CONECTION_DENSITY;
    }

    public ExperimentConfig(String filepath) {
        Yaml yaml = new Yaml();
        Map<String, Object> config = null;
        try (Reader reader = Files.newBufferedReader(Paths.get(filepath))) {
            config = (Map<String, Object>) yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //default values
        int configN = DEFAULT_CONFIG_NUMBER;
        int morphN = DEFAULT_MORPHOLOGY_NUMBER;
        int stepN = DEFAULT_SIMULATION_STEPS;
        int runN = DEFAULT_SIMULATION_RUNS;
        int popN = DEFAULT_POPULATION_SIZE;
        int generationN = DEFAULT_GENERATIONS;
        int archiveN = DEFAULT_ARCHIVE_SIZE;

        double addNodeDefault = DEFAULT_ADD_NODE_PROBABILITY;
        double addLinkDefault = DEFAULT_ADD_LINK_PROBABILITY;
        double removeLinkDefault = DEFAULT_REMOVE_LINK_PROBABILITY;
        double crossoverDefault = DEFAULT_CROSSOVER_PROBABILITY;
        double connectionDensityDefault = DEFAULT_CONECTION_DENSITY;

        Map configuration = (Map) config.get("configuration");
        if (checkFieldPresent(config, "configuration")) {
            Integer schema = (Integer) configuration.get("schema");
            if (checkFieldPresent(schema, "configuration:schema")) {
                configN = schema;
            }
            Integer morphology = (Integer) configuration.get("morphology");
            if (checkFieldPresent(morphology, "configuration:morphology")) {
                morphN = morphology;
            }
        }

        Map simulation = (Map) config.get("simulation");
        if (checkFieldPresent(simulation, "simulation")) {

            Integer runs = (Integer) simulation.get("runs");
            if (checkFieldPresent(simulation, "simulation:runs")) {
                runN = runs;
            }

            Integer steps = (Integer) simulation.get("steps");
            if (checkFieldPresent(steps, "simulation:steps")) {
                stepN = steps;
            }
        }

        Map ea = (Map) config.get("ea_variables");
        if (checkFieldPresent(ea, "ea_variables")) {
            Integer population = (Integer) ea.get("population");
            if (checkFieldPresent(population, "ea_variables:population")) {
                popN = population;
            }

            Integer generations = (Integer) ea.get("generations");
            if (checkFieldPresent(generations, "ea_variables:generations")) {
                generationN = generations;
            }

            Integer archives = (Integer) ea.get("archive");
            if (checkFieldPresent(archives, "ea_variables:archive")) {
                archiveN = archives;
            }

            Double addN = (Double) ea.get("add_node_probability");
            if (checkFieldPresent(addN, "ea_variables:add_node_probability")) {
                addNodeDefault = addN;
            }

            Double addL = (Double) ea.get("add_link_probability");
            if (checkFieldPresent(addL, "ea_variables:add_link_probability")) {
                addLinkDefault = addL;
            }

            Double removeL = (Double) ea.get("remove_link_probability");
            if (checkFieldPresent(removeL, "ea_variables:remove_link_probability")) {
                removeLinkDefault = removeL;
            }

            Double cross = (Double) ea.get("crossover_probability");
            if (checkFieldPresent(cross, "ea_variables:crossover_probability")) {
                crossoverDefault = cross;
            }

            Double connection = (Double) ea.get("connection_density");
            if (checkFieldPresent(connection, "ea_variables:connection_density")) {
                connectionDensityDefault = connection;
            }
        }

        this.configNumber = configN;
        this.morphologyNumber = morphN;
        this.simulationSteps = stepN;
        this.simulationRuns = runN;
        this.populationSize = popN;
        this.generations = generationN;
        this.archiveSize = archiveN;

        this.addNode = addNodeDefault;
        this.addLink = addLinkDefault;
        this.removeLink = removeLinkDefault;
        this.crossover = crossoverDefault;
        this.connectionDensity = connectionDensityDefault;
    }

    public ExperimentConfig(int configN, int morphN, int stepN, int runN, int popN, int generationN) {
        this.configNumber = configN;
        this.morphologyNumber = morphN;
        this.simulationSteps = stepN;
        this.simulationRuns = runN;
        this.populationSize = popN;
        this.generations = generationN;
    }

    public int getConfigNumber(){ return configNumber; }

    public int getMorphologyNumber(){ return morphologyNumber; }

    public int getSimulationSteps(){ return simulationSteps; }

    public int getSimulationRuns(){ return simulationRuns; }

    public int getPopulationSize(){ return populationSize; }

    public int getGenerationNumber(){ return generations; }

    public int getArchiveSize(){ return archiveSize; }

    public double getAddNodeProbability(){ return addNode; }

    public double getRemoveLinkProbability(){ return removeLink; }

    public double getAddLinkProbability(){ return addLink; }

    public double getCrossoverProbability(){ return crossover; }

    public double getConnectionDensity(){ return connectionDensity; }
}
