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
 *  Used to evaluate a controller by running it in the simulator for
 *  a number of times and averaging the performance
 *
 */
public class Evaluation {

    private static final Logger log = LoggerFactory.getLogger(Evaluation.class);
    private Path rootDirectory;
    private Path evaluationFitnessFile;

    // initialises the directories for saving result files
    public Evaluation(){
        rootDirectory = getLoggingDirectory();
        initDirectory(rootDirectory);

        evaluationFitnessFile = rootDirectory.resolve("evaluationFitness.csv");
        initStatsFile(evaluationFitnessFile);
    }

    private static void initDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            log.error("Unable to create directories", e);
        }
    }

    private static void initStatsFile(Path path) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            writer.write("adjacent resources\n");
        } catch (IOException e) {
            log.error("Unable to initialize stats file", e);
        }
    }

    public void saveEvaluationStats(double performance){
        String line = String.format("%f\n", performance);
        final OpenOption[] options = {
                StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE
        };
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(evaluationFitnessFile,
                Charset.defaultCharset(), options))) {
            writer.append(line);
        } catch (IOException e) {
            log.error("Failed to append to log file", e);
        }
    }
}
