package za.redbridge.simulator.moneat;

import org.encog.neural.neat.NEATPopulation;
// import za.redbridge.simulator.neat.NEATPopulation;
import java.io.Serializable;
import java.util.Random;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationFunction;
import org.encog.engine.network.activation.ActivationSteepenedSigmoid;
import org.encog.mathutil.randomize.factory.RandomFactory;
import org.encog.ml.MLError;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.ea.codec.GeneticCODEC;
import org.encog.ml.ea.genome.Genome;
import org.encog.ml.ea.species.BasicSpecies;
import org.encog.neural.neat.training.NEATGenome;
import org.encog.neural.neat.training.NEATInnovationList;
import org.encog.neural.neat.NEATCODEC;
import org.encog.neural.neat.FactorNEATGenome;
import za.redbridge.simulator.moneat.MultiObjectiveGenome;
import za.redbridge.simulator.moneat.Objective;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

/**
 * Extension of NEATPopulation. Takes in multiple objectives
 */

public class MultiObjectivePopulation extends NEATPopulation{

    private MultiObjectiveScore multiObjectiveScore;

    public MultiObjectivePopulation(){

    }

    public MultiObjectivePopulation(int inputCount, int outputCount, int populationSize) {
        super(inputCount, outputCount, populationSize);
        multiObjectiveScore = new MultiObjectiveScore();
    }

    public MultiObjectivePopulation(int inputCount, int outputCount, int populationSize, ArrayList<Objective> objectives) {
        super(inputCount, outputCount, populationSize);
        multiObjectiveScore = new MultiObjectiveScore(objectives);
    }

    @Override
    public void reset(){
        setCODEC(new NEATCODEC());
        setGenomeFactory(new FactorNEATGenome());

        getSpecies().clear();

		// reset counters
		getGeneIDGenerate().setCurrentID(1);
		getInnovationIDGenerate().setCurrentID(1);

		final Random rnd = getRandomNumberFactory().factor();

		// create one default species
		final BasicSpecies defaultSpecies = new BasicSpecies();
		defaultSpecies.setPopulation(this);

		// create the initial population
		for (int i = 0; i < getPopulationSize(); i++) {
			final NEATGenome genome = getGenomeFactory().factor(rnd, this,
					getInputCount(), getOutputCount(),
					getInitialConnectionDensity());
			defaultSpecies.add(genome);
		}
		defaultSpecies.setLeader(defaultSpecies.getMembers().get(0));
		getSpecies().add(defaultSpecies);

		// create initial innovations
		setInnovations(new NEATInnovationList(this));
    }

    public void init(){
        setCODEC(new NEATCODEC());
        setGenomeFactory(new FactorNEATGenome());

        getSpecies().clear();

		// reset counters
		getGeneIDGenerate().setCurrentID(1);
		getInnovationIDGenerate().setCurrentID(1);

		final Random rnd = getRandomNumberFactory().factor();

		// create one default species
		final BasicSpecies defaultSpecies = new BasicSpecies();
		defaultSpecies.setPopulation(this);

		// // create the initial population
		// for (int i = 0; i < getPopulationSize(); i++) {
		// 	final NEATGenome tempGenome = getGenomeFactory().factor(rnd, this,
		// 			getInputCount(), getOutputCount(),
		// 			getInitialConnectionDensity());
        //     final MultiObjectiveGenome genome = new MultiObjectiveGenome(tempGenome, genomeObjectives);
		// 	defaultSpecies.add(genome);
		// }
		// defaultSpecies.setLeader(defaultSpecies.getMembers().get(0));
		// getSpecies().add(defaultSpecies);

		// create initial innovations
		setInnovations(new NEATInnovationList(this));
    }

    public ArrayList<Objective> getPopulationObjectives(){
        return multiObjectiveScore.getObjectives();
    }
}
