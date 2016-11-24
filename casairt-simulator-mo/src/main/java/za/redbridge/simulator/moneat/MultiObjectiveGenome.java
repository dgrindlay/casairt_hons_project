package za.redbridge.simulator.moneat;

import org.encog.neural.neat.training.NEATGenome;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.encog.EncogError;
import org.encog.engine.network.activation.ActivationFunction;
import org.encog.mathutil.randomize.RangeRandomizer;
import org.encog.ml.ea.genome.BasicGenome;
import org.encog.ml.ea.genome.Genome;
import org.encog.neural.neat.NEATNeuronType;
import org.encog.neural.neat.NEATPopulation;
import org.encog.util.Format;
import za.redbridge.simulator.moneat.Objective;
import org.encog.neural.neat.training.NEATNeuronGene;
import org.encog.neural.neat.training.NEATLinkGene;

/**
 * Extension of Encog NEATGenome class. Adds multiple objectives as well as methods to compare them.
 */

public class MultiObjectiveGenome extends NEATGenome implements Cloneable, Serializable, Comparable<MultiObjectiveGenome>{

	private static final long serialVersionUID = 1L;

    private ArrayList<Objective> fitnessObjectives;

	private int strength;
	private int rawfitness;

    public MultiObjectiveGenome(final NEATGenome other){
        super(other);

        fitnessObjectives = new ArrayList<Objective>();
		this.strength = 0;
		this.rawfitness = 0;
    }

    public MultiObjectiveGenome(final NEATGenome other, ArrayList<Objective> objectives){
        super(other);

        fitnessObjectives = new ArrayList<Objective>();
        for(Objective o : objectives){
            fitnessObjectives.add(o);
        }
		this.strength = 0;
		this.rawfitness = 0;
    }

    public MultiObjectiveGenome(final List<NEATNeuronGene> neurons,
			final List<NEATLinkGene> links, final int inputCount,
			final int outputCount){
        super(neurons, links, inputCount, outputCount);

        fitnessObjectives = new ArrayList<Objective>();
		this.strength = 0;
		this.rawfitness = 0;
    }

    public MultiObjectiveGenome(final Random rnd, final NEATPopulation pop,
			final int inputCount, final int outputCount,
			double connectionDensity){
        super(rnd, pop, inputCount, outputCount, connectionDensity);

        fitnessObjectives = new ArrayList<Objective>();
		this.strength = 0;
		this.rawfitness = 0;
    }

    public MultiObjectiveGenome(){

    }

    public void addScore(String name, double score){
        for(Objective j : fitnessObjectives){
            if(j.getName().equalsIgnoreCase(name)){
                j.setScore(score);
            }
        }
    }

	public void addObjectiveScore(int i, double j){
		fitnessObjectives.get(i).setScore(j);
	}

    public double getObjectiveScore(String name){
        for(Objective j : fitnessObjectives){
            if(j.getName().equalsIgnoreCase(name)){
                return j.getScore();
            }
        }
        return -1;
    }

	public double getObjectiveScore(int i){
		return fitnessObjectives.get(i).getScore();
	}

    public ArrayList<Objective> getObjectives(){
        return fitnessObjectives;
    }

	public double distanceBetween(MultiObjectiveGenome g){
		double temp = 0;
		for(int i=0;i<fitnessObjectives.size();i++){
			temp += Math.pow((fitnessObjectives.get(i).getScore()-g.getObjectives().get(i).getScore()), 2);
		}
		return Math.sqrt(temp);
	}

    public boolean dominates(MultiObjectiveGenome g){
        int n = 0;
        ArrayList<Objective> other = g.getObjectives();
        for(int i=0;i<fitnessObjectives.size();i++){
            if(fitnessObjectives.get(i).compare(other.get(i)) == 1){
                n += 1;
            }
        }

        if(n==fitnessObjectives.size()){
            return true;
        }
        else{
            return false;
        }
    }

	public double getStrength(){
		return this.strength;
	}

	public void setStrength(int value){
		this.strength = value;
	}

	public double getRawFitness(){
		return this.rawfitness;
	}

	public void setRawFitness(int value){
		this.rawfitness = value;
	}

	public int compareTo(MultiObjectiveGenome otherGenome){
		if(otherGenome.getScore() > this.getScore()){
			return 1;
		}
		else if(otherGenome.getScore() < this.getScore()){
			return -1;
		}
		else{
			return 0;
		}
	}
}
