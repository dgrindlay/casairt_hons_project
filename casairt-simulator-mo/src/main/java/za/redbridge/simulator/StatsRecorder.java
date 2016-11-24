package za.redbridge.simulator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.encog.ml.ea.genome.Genome;
import org.encog.ml.ea.population.Population;
import org.encog.ml.ea.train.EvolutionaryAlgorithm;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.training.NEATGenome;
import org.encog.neural.networks.BasicNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.encog.neural.neat.NEATPopulation;
import za.redbridge.simulator.moneat.MultiObjectivePopulation;
import za.redbridge.simulator.moneat.MultiObjectiveGenome;
import za.redbridge.simulator.moneat.MultiObjectiveTrainEA;
import java.util.List;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static za.redbridge.simulator.Utils.getLoggingDirectory;
import static za.redbridge.simulator.Utils.saveObjectToFile;

/**
 * Class for recording fitness and performance stats each generation.
 *
 */
public class StatsRecorder {
    private static final Logger log = LoggerFactory.getLogger(StatsRecorder.class);

    private final EvolutionaryAlgorithm trainer;
    private final ScoreCalculator calculator;

    private double currentBestScore = 0;
    private Path rootDirectory;
    private Path populationDirectory;
    private Path bestNetworkDirectory;

    // paths for files for each statistic
    private Path adjacentPerformanceStatsFile;
    private Path constructionPerformanceStatsFile;
    private Path objectiveOneFitnessStatsFile;
    private Path objectiveTwoFitnessStatsFile;
    private Path objectiveThreeFitnessStatsFile;

    private Path evaluationFitnessFile;
    private Genome currentBestGenome;

    public StatsRecorder(EvolutionaryAlgorithm trainer, ScoreCalculator calculator) {
        this.trainer = trainer;
        this.calculator = calculator;
        initFiles();
    }

    // initialises the directories and files
    private void initFiles() {
        initDirectories();
        initStatsFiles();
    }

    private void initDirectories() {
        rootDirectory = getLoggingDirectory();
        initDirectory(rootDirectory);

        populationDirectory = rootDirectory.resolve("populations");
        initDirectory(populationDirectory);

        bestNetworkDirectory = rootDirectory.resolve("best networks");
        initDirectory(bestNetworkDirectory);
    }

    private static void initDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            log.error("Unable to create directories", e);
        }
    }

    private void initStatsFiles() {
        adjacentPerformanceStatsFile = rootDirectory.resolve("adjacentPerformance.csv");
        initStatsFile(adjacentPerformanceStatsFile);

        constructionPerformanceStatsFile = rootDirectory.resolve("constructionPerformance.csv");
        initStatsFile(adjacentPerformanceStatsFile);

        objectiveOneFitnessStatsFile = rootDirectory.resolve("objective_1.csv");
        initStatsFile(objectiveOneFitnessStatsFile);

        objectiveTwoFitnessStatsFile = rootDirectory.resolve("objective_2.csv");
        initStatsFile(objectiveTwoFitnessStatsFile);

        objectiveThreeFitnessStatsFile = rootDirectory.resolve("objective_3.csv");
        initStatsFile(objectiveThreeFitnessStatsFile);
    }

    private static void initStatsFile(Path path) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            writer.write("generation, min, max, mean, standev\n");
        } catch (IOException e) {
            log.error("Unable to initialize stats file", e);
        }
    }

    // record all of the statistics
    public void recordIterationStats() {
        int generation = trainer.getIteration();
        log.info("generation " + generation + " complete");

        recordStats(calculator.getAdjacentPerformanceStatistics(), generation, adjacentPerformanceStatsFile);
        recordStats(calculator.getConstructionPerformanceStatistics(), generation, constructionPerformanceStatsFile);

        recordStats(calculator.getObjectiveOneStatistics(), generation, objectiveOneFitnessStatsFile);
        recordStats(calculator.getObjectiveTwoStatistics(), generation, objectiveTwoFitnessStatsFile);
        recordStats(calculator.getObjectiveThreeStatistics(), generation, objectiveThreeFitnessStatsFile);

        savePopulation((MultiObjectivePopulation) trainer.getPopulation(), generation);

        log.info("Non-dominated set of genomes:");
        List<Genome> nondominatedSet = ((MultiObjectiveTrainEA) trainer).getParetoFront();
        int i = 0;
        for(Genome genome : nondominatedSet){
            saveGenome((NEATGenome) genome, generation, i);
            i++;
        }
    }

    // saves a population of genomes
    private void savePopulation(Population population, int generation) {
        String filename = "generation-" + generation + ".ser";
        Path path = populationDirectory.resolve(filename);
        saveObjectToFile(population, path);
    }

    // decodes a genome into a neat network
    private NEATNetwork decodeNeatGenome(Genome genome) {
        return (NEATNetwork) trainer.getCODEC().decode(genome);
    }

    // saves a genome into a file that can then be demoed
    private void saveGenome(NEATGenome genome, int epoch, int i) {
        Path directory = bestNetworkDirectory.resolve("epoch-" + epoch);
        initDirectory(directory);

        String txt;

        log.info("Epoch: " + epoch + ", score: "  + genome.getScore());
        txt = String.format("epoch: %d, fitness: %f", epoch, genome.getScore());

        Path txtPath = directory.resolve("info" + i + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(txtPath, Charset.defaultCharset())) {
            writer.write(txt);
        } catch (IOException e) {
            log.error("Error writing best network info file", e);
        }

        NEATNetwork network = decodeNeatGenome(genome);
        saveObjectToFile(network, directory.resolve("network" + i + ".ser"));

        GraphvizEngine.saveGenome(genome, directory.resolve("graph" + i + ".dot"));
    }

    private void recordStats(DescriptiveStatistics stats, int generation, Path filepath) {
        double max = stats.getMax();
        double min = stats.getMin();
        double mean = stats.getMean();
        double sd = stats.getStandardDeviation();
        stats.clear();

        log.debug("Recording stats - min: " + min + ", max: " + max + ", mean :" + mean);
        saveStats(filepath, generation, min, max, mean, sd);
    }

    private BasicNetwork decodeGenome(Genome genome) {
        return (BasicNetwork) trainer.getCODEC().decode(genome);
    }

    private static void saveStats(Path path, int generation, double min, double max, double mean,
            double sd) {
        String line = String.format("%d, %f, %f, %f, %f\n", generation, min, max, mean, sd);

        final OpenOption[] options = {
                StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE
        };
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path,
                Charset.defaultCharset(), options))) {
            writer.append(line);
        } catch (IOException e) {
            log.error("Failed to append to log file", e);
        }
    }
}
