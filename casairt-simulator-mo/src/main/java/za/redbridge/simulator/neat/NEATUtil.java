package za.redbridge.simulator.neat;

import org.encog.ml.CalculateScore;
import org.encog.ml.ea.opp.CompoundOperator;
import org.encog.ml.ea.opp.selection.TruncationSelection;
import org.encog.ml.ea.opp.selection.TournamentSelection;
import org.encog.ml.ea.train.basic.TrainEA;
import org.encog.neural.neat.NEATCODEC;
import org.encog.neural.neat.training.opp.NEATCrossover;
import org.encog.neural.neat.training.opp.NEATMutateAddLink;
import org.encog.neural.neat.training.opp.NEATMutateAddNode;
import org.encog.neural.neat.training.opp.NEATMutateRemoveLink;
import org.encog.neural.neat.training.opp.NEATMutateWeights;
import org.encog.neural.neat.training.opp.links.MutatePerturbLinkWeight;
import org.encog.neural.neat.training.opp.links.MutateResetLinkWeight;
import org.encog.neural.neat.training.opp.links.SelectFixed;
import org.encog.neural.neat.training.opp.links.SelectProportion;
import org.encog.neural.neat.training.species.OriginalNEATSpeciation;
import org.encog.neural.neat.NEATPopulation;
import za.redbridge.simulator.moneat.MultiObjectivePopulation;
import za.redbridge.simulator.moneat.MultiObjectiveTrainEA;
import za.redbridge.simulator.ScoreCalculator;

public final class NEATUtil {

    private NEATUtil() {
    }

    public static TrainEA constructNEATTrainer(CalculateScore calculateScore, int inputCount,
            int outputCount, int populationSize, double addNode, double addLink, double removeLink, double crossover) {
        NEATPopulation pop = new NEATPopulation(inputCount, outputCount, populationSize);
        pop.reset();
        return constructNEATTrainer(pop, calculateScore, addNode, addLink, removeLink, crossover);
    }

    /**
     * Construct a NEAT (or HyperNEAT trainer.
     * @param population The population.
     * @param calculateScore The score function.
     * @return The NEAT EA trainer.
     */
    public static TrainEA constructNEATTrainer(NEATPopulation population,
            CalculateScore calculateScore, double addNode, double addLink, double removeLink, double crossover) {
        TrainEA result = new TrainEA(population, calculateScore);
        result.setSpeciation(new OriginalNEATSpeciation());

        // put this in a config file to change between truncation and tournament selection
        // result.setSelection(new TruncationSelection(result, 0.3));
        result.setSelection(new TournamentSelection(result, 4));
        CompoundOperator weightMutation = new CompoundOperator();
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(1),
                        new MutatePerturbLinkWeight(0.004)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(2),
                        new MutatePerturbLinkWeight(0.004)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(3),
                        new MutatePerturbLinkWeight(0.004)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectProportion(0.02),
                        new MutatePerturbLinkWeight(0.004)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(1),
                        new MutatePerturbLinkWeight(0.2)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(2),
                        new MutatePerturbLinkWeight(0.2)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(3),
                        new MutatePerturbLinkWeight(0.2)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectProportion(0.02),
                        new MutatePerturbLinkWeight(0.2)));
        weightMutation.getComponents().add(0.03, new NEATMutateWeights(new SelectFixed(1),
                        new MutateResetLinkWeight()));
        weightMutation.getComponents().add(0.03, new NEATMutateWeights(new SelectFixed(2),
                        new MutateResetLinkWeight()));
        weightMutation.getComponents().add(0.03, new NEATMutateWeights(new SelectFixed(3),
                        new MutateResetLinkWeight()));
        weightMutation.getComponents().add(0.01, new NEATMutateWeights(new SelectProportion(0.02),
                        new MutateResetLinkWeight()));
        weightMutation.getComponents().finalizeStructure();

        result.setChampMutation(weightMutation);
        result.addOperation(crossover, new NEATCrossover());
        result.addOperation(0.493, weightMutation);
        result.addOperation(addNode, new NEATMutateAddNode());
        result.addOperation(addLink, new NEATMutateAddLink());
        result.addOperation(removeLink, new NEATMutateRemoveLink());
        result.getOperators().finalizeStructure();

        result.setCODEC(new NEATCODEC());

        return result;
    }

    public static MultiObjectiveTrainEA constructMultiObjectiveTrainer(MultiObjectivePopulation population, MultiObjectivePopulation archive,
            ScoreCalculator calculateScore, int archiveSize,double addNode, double addLink, double removeLink, double crossover){
        MultiObjectiveTrainEA result = new MultiObjectiveTrainEA(population, archive, calculateScore, archiveSize);
        result.setSpeciation(new OriginalNEATSpeciation());

        // put this in a config file to change between truncation and tournament selection
        result.setSelection(new TruncationSelection(result, 0.3));
        CompoundOperator weightMutation = new CompoundOperator();
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(1),
                        new MutatePerturbLinkWeight(0.004)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(2),
                        new MutatePerturbLinkWeight(0.004)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(3),
                        new MutatePerturbLinkWeight(0.004)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectProportion(0.02),
                        new MutatePerturbLinkWeight(0.004)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(1),
                        new MutatePerturbLinkWeight(0.2)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(2),
                        new MutatePerturbLinkWeight(0.2)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectFixed(3),
                        new MutatePerturbLinkWeight(0.2)));
        weightMutation.getComponents().add(0.1125, new NEATMutateWeights(new SelectProportion(0.02),
                        new MutatePerturbLinkWeight(0.2)));
        weightMutation.getComponents().add(0.03, new NEATMutateWeights(new SelectFixed(1),
                        new MutateResetLinkWeight()));
        weightMutation.getComponents().add(0.03, new NEATMutateWeights(new SelectFixed(2),
                        new MutateResetLinkWeight()));
        weightMutation.getComponents().add(0.03, new NEATMutateWeights(new SelectFixed(3),
                        new MutateResetLinkWeight()));
        weightMutation.getComponents().add(0.01, new NEATMutateWeights(new SelectProportion(0.02),
                        new MutateResetLinkWeight()));
        weightMutation.getComponents().finalizeStructure();

        result.setChampMutation(weightMutation);
        result.addOperation(crossover, new NEATCrossover());
        result.addOperation(0.493, weightMutation);
        result.addOperation(addNode, new NEATMutateAddNode());
        result.addOperation(addLink, new NEATMutateAddLink());
        result.addOperation(removeLink, new NEATMutateRemoveLink());
        result.getOperators().finalizeStructure();

        result.setCODEC(new NEATCODEC());

        return result;
    }
}
