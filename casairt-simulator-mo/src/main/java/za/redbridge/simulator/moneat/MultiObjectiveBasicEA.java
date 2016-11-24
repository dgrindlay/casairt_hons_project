package za.redbridge.simulator.moneat;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.encog.Encog;
import org.encog.EncogError;
import org.encog.EncogShutdownTask;
import org.encog.mathutil.randomize.factory.RandomFactory;
import org.encog.ml.CalculateScore;
import org.encog.ml.MLContext;
import org.encog.ml.MLMethod;
import org.encog.ml.ea.codec.GeneticCODEC;
import org.encog.ml.ea.codec.GenomeAsPhenomeCODEC;
import org.encog.ml.ea.genome.Genome;
import org.encog.ml.ea.opp.EvolutionaryOperator;
import org.encog.ml.ea.opp.OperationList;
import org.encog.ml.ea.opp.selection.SelectionOperator;
import org.encog.ml.ea.opp.selection.TournamentSelection;
import org.encog.ml.ea.population.Population;
import org.encog.ml.ea.rules.BasicRuleHolder;
import org.encog.ml.ea.rules.RuleHolder;
import org.encog.ml.ea.score.AdjustScore;
import org.encog.ml.ea.score.parallel.ParallelScore;
import org.encog.ml.ea.sort.GenomeComparator;
import org.encog.ml.ea.sort.MaximizeAdjustedScoreComp;
import org.encog.ml.ea.sort.MaximizeScoreComp;
import org.encog.ml.ea.sort.MinimizeAdjustedScoreComp;
import org.encog.ml.ea.sort.MinimizeScoreComp;
import org.encog.ml.ea.species.SingleSpeciation;
import org.encog.ml.ea.species.Speciation;
import org.encog.neural.neat.training.species.OriginalNEATSpeciation;
import org.encog.ml.ea.species.Species;
import org.encog.ml.ea.train.EvolutionaryAlgorithm;
import org.encog.ml.genetic.GeneticError;
import org.encog.util.concurrency.MultiThreadable;
import org.encog.util.logging.EncogLogging;
import org.encog.ml.ea.train.basic.EAWorker;
import za.redbridge.simulator.ScoreCalculator;
import java.util.Collections;
import java.util.Comparator;
import za.redbridge.simulator.moneat.MultiObjectivePopulation;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main algorithm for MO-NEAT. Iteratively evolves a population of NEAT controllers
 *
 * Extension of ENCOG library class
 */
public class MultiObjectiveBasicEA implements EvolutionaryAlgorithm, MultiThreadable,
		EncogShutdownTask, Serializable {

    /**
	 * The serial id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Calculate the score adjustment, based on adjusters.
	 *
	 * @param genome
	 *            The genome to adjust.
	 * @param adjusters
	 *            The score adjusters.
	 */
	public static void calculateScoreAdjustment(final Genome genome,
			final List<AdjustScore> adjusters) {
		final double score = genome.getScore();
		double delta = 0;

		for (final AdjustScore a : adjusters) {
			delta += a.calculateAdjustment(genome);
		}

		genome.setAdjustedScore(score + delta);
	}

	/**
	 * Should exceptions be ignored.
	 */
	private boolean ignoreExceptions;

	/**
	 * The genome comparator.
	 */
	private GenomeComparator bestComparator;

	/**
	 * The genome comparator.
	 */
	private GenomeComparator selectionComparator;

	/**
	 * The population.
	 */
	private MultiObjectivePopulation archivePopulation;

	private MultiObjectivePopulation population;

	/**
	 * The score calculation function.
	 */
	private final ScoreCalculator scoreFunction;

	/**
	 * The selection operator.
	 */
	private SelectionOperator selection;

	/**
	 * The score adjusters.
	 */
	private final List<AdjustScore> adjusters = new ArrayList<AdjustScore>();

	/**
	 * The operators. to use.
	 */
	private final OperationList operators = new OperationList();

	/**
	 * The CODEC to use to convert between genome and phenome.
	 */
	private GeneticCODEC codec = new GenomeAsPhenomeCODEC();

	/**
	 * Random number factory.
	 */
	private RandomFactory randomNumberFactory = Encog.getInstance()
			.getRandomFactory().factorFactory();

	/**
	 * The validation mode.
	 */
	private boolean validationMode;

	/**
	 * The iteration number.
	 */
	private int iteration;

	/**
	 * The desired thread count.
	 */
	private int threadCount;

	/**
	 * The actual thread count.
	 */
	private int actualThreadCount = -1;

	/**
	 * The speciation method.
	 */
	private Speciation speciation = new SingleSpeciation();

	/**
	 * This property stores any error that might be reported by a thread.
	 */
	private Throwable reportedError;

	/**
	 * The best genome from the last iteration.
	 */
	private Genome oldBestGenome;

	/**
	 * The population for the next iteration.
	 */
	private final List<Genome> newPopulation = new ArrayList<Genome>();

	/**
	 * The mutation to be used on the top genome. We want to only modify its
	 * weights.
	 */
	private EvolutionaryOperator champMutation;

	/**
	 * The percentage of a species that is "elite" and is passed on directly.
	 */
	private double eliteRate = 0.3;

	/**
	 * The number of times to try certian operations, so an endless loop does
	 * not occur.
	 */
	private int maxTries = 5;

	/**
	 * The best ever genome.
	 */
	private Genome bestGenome;

	/**
	 * The thread pool executor.
	 */
	private ExecutorService taskExecutor;

	/**
	 * Holds the threads used each iteration.
	 */
	private final List<Callable<Object>> threadList = new ArrayList<Callable<Object>>();

	/**
	 * Holds rewrite and constraint rules.
	 */
	private RuleHolder rules;

	private int maxOperationErrors = 500;

	/**
	 * List of archived genomes
	 */
    private List<Genome> archiveList;

	/**
	 * List of genomes in archive that are non-dominated
	 */
    private List<Genome> nondominatedSet;

	/**
	 * Value for k-th nearest neighbour calculations
	 */
    private int k;

	/**
	 * Mapping between genomes and their score values
	 */
	private ConcurrentHashMap<Genome, MultiObjectiveScore> populationMap;

	/**
	 * The objectives given to the simulator through the population
	 */
	private ArrayList<Objective> populationObjectives;

	/**
	 * Size of the archve - about a third of the population size
	 */
	private int archiveSize;

	/**
	 * Construct an EA.
	 *
	 * @param thePopulation
	 *            The population.
	 * @param theScoreFunction
	 *            The score function.
	 */
	public MultiObjectiveBasicEA(final MultiObjectivePopulation thePopulation, final MultiObjectivePopulation theArchive,
			final ScoreCalculator theScoreFunction, int archiveSize) {

		this.population = thePopulation;
		this.archivePopulation = theArchive;
		this.scoreFunction = theScoreFunction;
		this.archiveSize = archiveSize;
		this.selection = new TournamentSelection(this, 4);
		this.rules = new BasicRuleHolder();

        this.archiveList = new ArrayList<Genome>();
		this.nondominatedSet = new ArrayList<Genome>();
        this.k = (int) Math.sqrt(this.population.getPopulationSize()+archiveSize);
		this.populationMap = new ConcurrentHashMap<Genome, MultiObjectiveScore>();
		this.populationObjectives = thePopulation.getPopulationObjectives();

		// set the score compare method
		if (theScoreFunction.shouldMinimize()) {
			this.selectionComparator = new MinimizeAdjustedScoreComp();
			this.bestComparator = new MinimizeScoreComp();
		} else {
			this.selectionComparator = new MaximizeAdjustedScoreComp();
			this.bestComparator = new MaximizeScoreComp();
		}

		// set the iteration for population
		for (final Species species : thePopulation.getSpecies()) {
			for (final Genome genome : species.getMembers()) {
				setIteration(Math.max(getIteration(),
						genome.getBirthGeneration()));
			}
		}

		// set the iteration for archive
		for (final Species species : theArchive.getSpecies()) {
			for (final Genome genome : species.getMembers()) {
				setIteration(Math.max(getIteration(),
						genome.getBirthGeneration()));
			}
		}

		// Set a best genome, just so it is not null.
		// We won't know the true best genome until the first iteration.
		if( this.population.getSpecies().size()>0 && this.population.getSpecies().get(0).getMembers().size()>0 ) {
			this.bestGenome = (Genome) this.population.getSpecies().get(0).getMembers().get(0);
		}
	}

	/**
	 * Add a child to the next iteration.
	 *
	 * @param genome
	 *            The child.
	 * @return True, if the child was added successfully.
	 */
	public boolean addChild(final Genome genome) {
		synchronized (this.newPopulation) {
			if (this.newPopulation.size() < getPopulation().getPopulationSize()) {
				// don't readd the old best genome, it was already added
				if (genome != this.oldBestGenome) {

					if (isValidationMode()) {
						if (this.newPopulation.contains(genome)) {
							throw new EncogError(
									"Genome already added to population: "
											+ genome.toString());
						}
					}

					this.newPopulation.add(genome);
				}

				if (!Double.isInfinite(genome.getScore())
						&& !Double.isNaN(genome.getScore())
						&& getBestComparator().isBetterThan(genome,
								this.bestGenome)) {
					this.bestGenome = genome;
					getPopulation().setBestGenome(this.bestGenome);
				}
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addOperation(final double probability,
			final EvolutionaryOperator opp) {
		getOperators().add(probability, opp);
		opp.init(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addScoreAdjuster(final AdjustScore scoreAdjust) {
		this.adjusters.add(scoreAdjust);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void calculateScore(Genome g) {
        // try rewrite
        getRules().rewrite(g);

        // decode
        final MLMethod phenotype = getCODEC().decode(g);
        ArrayList<Double> scoreValues = new ArrayList<Double>();

        // deal with invalid decode
        if (phenotype == null) {
            if (getBestComparator().shouldMinimize()) {
                scoreValues.add(Double.POSITIVE_INFINITY);
            } else {
                scoreValues.add(Double.NEGATIVE_INFINITY);
            }
        } else {
            if (phenotype instanceof MLContext) {
                ((MLContext) phenotype).clearContext();
            }
            // score = getScoreFunction().calculateScore(phenotype);
            scoreValues = getScoreFunction().calculateMultipleScores(phenotype);
        }

		MultiObjectiveScore genomeScore = new MultiObjectiveScore(populationObjectives);
		genomeScore.setObjectiveScores(scoreValues);

		// print objective scores
		// System.out.println(genomeScore);

		// put into map
		populationMap.put(g, genomeScore);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finishTraining() {
		// wait for threadpool to shutdown
		if (this.taskExecutor != null) {
			this.taskExecutor.shutdown();
			try {
				this.taskExecutor.awaitTermination(Long.MAX_VALUE,
						TimeUnit.MINUTES);
			} catch (final InterruptedException e) {
				throw new GeneticError(e);
			} finally {
				this.taskExecutor = null;
				Encog.getInstance().removeShutdownTask(this);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenomeComparator getBestComparator() {
		return this.bestComparator;
	}

	/**
	 * @return the bestGenome
	 */
	@Override
	public Genome getBestGenome() {
		return this.bestGenome;
	}

    public MultiObjectivePopulation getArchive(){
        return this.archivePopulation;
    }

	/**
	 * @return the champMutation
	 */
	public EvolutionaryOperator getChampMutation() {
		return this.champMutation;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GeneticCODEC getCODEC() {
		return this.codec;
	}

	/**
	 * @return the eliteRate
	 */
	public double getEliteRate() {
		return this.eliteRate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getError() {
		// do we have a best genome, and does it have an error?
		if (this.bestGenome != null) {
			double err = this.bestGenome.getScore();
			if( !Double.isNaN(err) ) {
				return err;
			}
		}

		// otherwise, assume the worst!
		if (getScoreFunction().shouldMinimize()) {
			return Double.POSITIVE_INFINITY;
		} else {
			return Double.NEGATIVE_INFINITY;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getIteration() {
		return this.iteration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMaxIndividualSize() {
		return this.archivePopulation.getMaxIndividualSize();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMaxTries() {
		return this.maxTries;
	}

	/**
	 * @return the oldBestGenome
	 */
	public Genome getOldBestGenome() {
		return this.oldBestGenome;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OperationList getOperators() {
		return this.operators;
	}

	/**
	 * @return The population.
	 */
	@Override
	public Population getPopulation() {
		return this.archivePopulation;
	}

	/**
	 * @return the randomNumberFactory
	 */
	public RandomFactory getRandomNumberFactory() {
		return this.randomNumberFactory;
	}

	/**
	 * @return the rules
	 */
	@Override
	public RuleHolder getRules() {
		return this.rules;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AdjustScore> getScoreAdjusters() {
		return this.adjusters;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ScoreCalculator getScoreFunction() {
		return this.scoreFunction;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SelectionOperator getSelection() {
		return this.selection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenomeComparator getSelectionComparator() {
		return this.selectionComparator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getShouldIgnoreExceptions() {
		return this.ignoreExceptions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Speciation getSpeciation() {
		return this.speciation;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getThreadCount() {
		return this.threadCount;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidationMode() {
		return this.validationMode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void iteration() {

		if (this.actualThreadCount == -1) {
			preIteration();
		}

		if (getPopulation().getSpecies().size() == 0) {
			throw new EncogError("Population is empty, there are no species.");
		}

		this.iteration++;

		// Clear new population to just best genome.
		this.newPopulation.clear();
		this.newPopulation.add(this.bestGenome);
		this.oldBestGenome = this.bestGenome;

		// execute species in parallel
		this.threadList.clear();
		for (final Species species : getPopulation().getSpecies()) {
			int numToSpawn = species.getOffspringCount();
			// System.out.println("Number to spawn: "+numToSpawn);

			// now add one task for each offspring that each species is allowed
			while (numToSpawn-- > 0) {
				final MultiObjectiveEAWorker worker = new MultiObjectiveEAWorker(this, species);
				this.threadList.add(worker);
			}
		}

		// run all threads and wait for them to finish
		try {
			this.taskExecutor.invokeAll(this.threadList);
		} catch (final InterruptedException e) {
			EncogLogging.log(e);
		}

		// handle any errors that might have happened in the threads
		if (this.reportedError != null && !getShouldIgnoreExceptions()) {
			throw new GeneticError(this.reportedError);
		}

		// validate, if requested
		if (isValidationMode()) {
			if (this.oldBestGenome != null
					&& !this.newPopulation.contains(this.oldBestGenome)) {
				throw new EncogError(
						"The top genome died, this should never happen!!");
			}

			if (this.bestGenome != null
					&& this.oldBestGenome != null
					&& getBestComparator().isBetterThan(this.oldBestGenome,
							this.bestGenome)) {
				throw new EncogError(
						"The best genome's score got worse, this should never happen!! Went from "
								+ this.oldBestGenome.getScore() + " to "
								+ this.bestGenome.getScore());
			}
		}

		System.out.println("New population size: "+ newPopulation.size());

		// add newPopulation and archive members to the same total list
		List<Genome> totalPopulation = new ArrayList<>();
		for (Genome genome : newPopulation) {
			totalPopulation.add(genome);
		}

		for(Genome genome : archiveList){
			totalPopulation.add(genome);
		}

		System.out.println("Archive population size: "+archiveList.size());

		System.out.println("Total population size: "+totalPopulation.size());

		// iterate through totalPopulation and find final score for each
		speaFitnessAssignment(totalPopulation);

		// add all nondominated members from the totalPopulation to the archive
		archiveList.clear();
		for(Genome genome : totalPopulation){
			if(genome.getScore() < 1){
				archiveList.add(genome);
			}
		}

		System.out.println("Size of archive after adding non-dominated: "+archiveList.size());

		// fill/reduce the archive depending on size
		if(archiveList.size() < archiveSize){
			// System.out.println("Add to the archive");
            Collections.sort(totalPopulation, new Comparator<Genome>(){
				@Override
				public int compare(Genome g2, Genome g1){
					if(g2.getScore() < g1.getScore()){
						return -1;
					}
					else if(g2.getScore() > g1.getScore()){
						return 1;
					}
					else return 0;
				}
			});

			int numberToAdd = archiveSize - archiveList.size();
			int j = 0;
			while(numberToAdd != 0){
				if(!archiveList.contains(totalPopulation.get(j))){
					archiveList.add(totalPopulation.get(j));
					numberToAdd --;
				}
				j++;
			}
        }
        else if(archiveList.size() > archiveSize){
			// System.out.println("Reduce the archive by: "+(archiveList.size()-archiveSize));
			int reduceBy = archiveList.size()-archiveSize;
			int k = 0;
			while(k<reduceBy){
				int index = reduceArchive(archiveList);
				archiveList.remove(index);
				k++;
			}
        }

		System.out.println("Size of archive after filling/reducing: "+archiveList.size());

		// clear the nondominated set
		nondominatedSet.clear();

		// add nondominated members in the archive to the nondominated set
		for(Genome genome : archiveList){
			if(genome.getScore() < 1){
				nondominatedSet.add(genome);
			}
		}

		System.out.println("Number of genomes in non-dominated set: "+nondominatedSet.size());

		// speciate on the archive list
		this.speciation.performSpeciation(this.archiveList);

        // purge invalid genomes
        this.archivePopulation.purgeInvalidGenomes();
	}

	// takes in total population (archive + population) and calculates fitness values based on spea2 algorithm
	// sets the score and adjusted score for every genome in the population and archive
	public void speaFitnessAssignment(List<Genome> totalPopulation){
		// calculate strength values
        for(Genome g1 : totalPopulation){
            int dominateCount = 0;
            for(Genome g2 : totalPopulation){
                if(g1!=g2){
                    if(populationMap.get(g1).dominates(populationMap.get(g2))){
                        dominateCount++;
                    }
                }
            }
            populationMap.get(g1).setStrength(dominateCount);
        }

        // calculate raw fitness values
        for(Genome g1 : totalPopulation){
            int rawfitness = 0;
            for(Genome g2 : totalPopulation){
                if(g1!=g2){
                    if(populationMap.get(g2).dominates(populationMap.get(g1))){
                        rawfitness += populationMap.get(g2).getStrength();
                    }
                }
            }
            populationMap.get(g1).setRawFitness(rawfitness);
        }

        // calculate k-th nearest neighbour and set the score for the genome
        for(Genome g1 : totalPopulation){
            ArrayList<Double> neighbourList = new ArrayList<Double>();
            for(Genome g2 : totalPopulation){
                if(g1!=g2){
                    double distance = populationMap.get(g1).distanceBetween(populationMap.get(g2));
                    neighbourList.add(distance);
                }
            }
            Collections.sort(neighbourList);
            double density = 1.0/(neighbourList.get(k) + 2);
			double score = populationMap.get(g1).getRawFitness() + density;
            g1.setScore(score);
            g1.setAdjustedScore(score);
			populationMap.get(g1).setScore(score);

			// System.out.println("Genome - strength: "+populationMap.get(g1).getStrength()+", rawfitness: "+populationMap.get(g1).getRawFitness()+", density: "+density+", score: "+score);
        }
	}

	public int reduceArchive(List<Genome> archive){
		double lowestDistance = Double.MAX_VALUE;
		int i = -1;
		int index = 0;
		for(Genome g1 : archive){
			i++;
            ArrayList<Double> neighbourList = new ArrayList<Double>();
            for(Genome g2 : archive){
                if(g1!=g2){
                    double distance = populationMap.get(g1).distanceBetween(populationMap.get(g2));
                    neighbourList.add(distance);
                }
            }
            Collections.sort(neighbourList);
			double distance = neighbourList.get(k);

			if(distance < lowestDistance){
				index = i;
				lowestDistance = distance;
			}
        }
		return index;
	}

	public List<Genome> getParetoFront(){
		return nondominatedSet;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void performShutdownTask() {
		finishTraining();
	}

	/**
	 * Called before the first iteration. Determine the number of threads to
	 * use.
	 */
	private void preIteration() {
		this.speciation.init(this);

		// find out how many threads to use
		if (this.threadCount == 0) {
			this.actualThreadCount = Runtime.getRuntime().availableProcessors();
		} else {
			this.actualThreadCount = this.threadCount;
		}

		// score the initial population
		final MultiObjectiveParallelScore pscore = new MultiObjectiveParallelScore(this.population,
				getCODEC(), new ArrayList<AdjustScore>(), getScoreFunction(),
				this.actualThreadCount, this.populationMap);
		pscore.setThreadCount(this.actualThreadCount);
		pscore.process();
		this.actualThreadCount = pscore.getThreadCount();

		// start up the thread pool
		if (this.actualThreadCount == 1) {
			this.taskExecutor = Executors.newSingleThreadScheduledExecutor();
		} else {
			this.taskExecutor = Executors
					.newFixedThreadPool(this.actualThreadCount);
		}

		// register for shutdown
		Encog.getInstance().addShutdownTask(this);

		// just pick the first genome with a valid score as best, it will be
		// updated later.
		// also most populations are sorted this way after training finishes
		// (for reload)
		// if there is an empty population, the constructor would have blow
		final List<Genome> list = this.population.flatten();

		// iterate through and set MultiObjectiveScore for each of them
		speaFitnessAssignment(list);

		int idx = 0;
		do {
			this.bestGenome = list.get(idx++);
		} while (idx < list.size()
				&& (Double.isInfinite(this.bestGenome.getScore()) || Double
						.isNaN(this.bestGenome.getScore())));

		getPopulation().setBestGenome(this.bestGenome);

		// speciate
		final List<Genome> genomes = this.population.flatten();

		// System.out.println("Genome scores:");
		// for(Genome genome : genomes){
		// 	System.out.println(genome);
		// }

		this.speciation.performSpeciation(genomes);

		// System.out.println("Number of genomes before speciation: "+genomes.size());
		// System.out.println("Number of genomes after speciation: "+this.archivePopulation.flatten().size());

		// purge invalid genomes
        this.archivePopulation.purgeInvalidGenomes();

		// System.out.println("Number of genomes after purging invalids: "+this.archivePopulation.flatten().size());
	}

	/**
	 * Called by a thread to report an error.
	 *
	 * @param t
	 *            The error reported.
	 */
	public void reportError(final Throwable t) {
		synchronized (this) {
			if (this.reportedError == null) {
				this.reportedError = t;
			}
		}
	}

	/**
	 * Set the comparator.
	 *
	 * @param theComparator
	 *            The comparator.
	 */
	@Override
	public void setBestComparator(final GenomeComparator theComparator) {
		this.bestComparator = theComparator;
	}

	/**
	 * @param champMutation
	 *            the champMutation to set
	 */
	public void setChampMutation(final EvolutionaryOperator champMutation) {
		this.champMutation = champMutation;
	}

	/**
	 * Set the CODEC to use.
	 *
	 * @param theCodec
	 *            The CODEC to use.
	 */
	public void setCODEC(final GeneticCODEC theCodec) {
		this.codec = theCodec;
	}

	/**
	 * @param eliteRate
	 *            the eliteRate to set
	 */
	public void setEliteRate(final double eliteRate) {
		this.eliteRate = eliteRate;
	}

	/**
	 * Set the current iteration number.
	 *
	 * @param iteration
	 *            The iteration number.
	 */
	public void setIteration(final int iteration) {
		this.iteration = iteration;
	}

	/**
	 * @param maxTries
	 *            the maxTries to set
	 */
	public void setMaxTries(final int maxTries) {
		this.maxTries = maxTries;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPopulation(final Population thePopulation) {
		this.population = (MultiObjectivePopulation) thePopulation;
	}

	public void setArchivePopulation(final Population thePopulation) {
		this.archivePopulation = (MultiObjectivePopulation) thePopulation;
	}

	/**
	 * @param randomNumberFactory
	 *            the randomNumberFactory to set
	 */
	public void setRandomNumberFactory(final RandomFactory randomNumberFactory) {
		this.randomNumberFactory = randomNumberFactory;
	}

	/**
	 * @param rules
	 *            the rules to set
	 */
	@Override
	public void setRules(final RuleHolder rules) {
		this.rules = rules;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSelection(final SelectionOperator selection) {
		this.selection = selection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSelectionComparator(final GenomeComparator theComparator) {
		this.selectionComparator = theComparator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setShouldIgnoreExceptions(final boolean b) {
		this.ignoreExceptions = b;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSpeciation(final Speciation speciation) {
		this.speciation = speciation;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setThreadCount(final int numThreads) {
		this.threadCount = numThreads;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setValidationMode(final boolean validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * @return the maxOperationErrors
	 */
	public int getMaxOperationErrors() {
		return maxOperationErrors;
	}

	/**
	 * @param maxOperationErrors the maxOperationErrors to set
	 */
	public void setMaxOperationErrors(int maxOperationErrors) {
		this.maxOperationErrors = maxOperationErrors;
	}
}
