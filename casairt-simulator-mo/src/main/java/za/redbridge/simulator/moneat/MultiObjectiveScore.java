package za.redbridge.simulator.moneat;

import java.util.ArrayList;
import java.io.Serializable;

/**
 * A score class that contains values for a number of objectives. Assigned to a single genome to store score values and other variables
 * used for calculating the end score.
 */

public class MultiObjectiveScore implements Serializable{

    private static final long serialVersionUID = 1L;

    private ArrayList<Objective> objectives;
    private double strength;
    private double rawfitness;
    private double score;

    public MultiObjectiveScore(){
        objectives = new ArrayList<>();
        this.strength = 0;
        this.rawfitness = 0;
        this.score = 0;
    }

    public MultiObjectiveScore(ArrayList<Objective> newObjectives){
        objectives = new ArrayList<Objective>();
        for(Objective objective : newObjectives){
            objectives.add(new Objective(objective));
        }
        this.strength = 0;
        this.rawfitness = 0;
        this.score = 0;
    }

    public void setStrength(double value){
        this.strength = value;
    }

    public void setRawFitness(double value){
        this.rawfitness = value;
    }

    public void setScore(double value){
        this.score = value;
    }

    public double getStrength(){
        return this.strength;
    }

    public double getRawFitness(){
        return this.rawfitness;
    }

    public double getScore(){
        return this.score;
    }

    public void addObjective(Objective newObjective){
        objectives.add(new Objective(newObjective));
    }

    public Objective getObjective(String name){
        for(Objective obj : objectives){
            if(obj.getName().equalsIgnoreCase(name)){
                return obj;
            }
        }
        return null;
    }

    public Objective getObjective(int i){
        return objectives.get(i);
    }

    public ArrayList<Objective> getObjectives(){
        for(Objective obj : objectives){

        }
        return objectives;
    }

    public void setObjectiveScore(String name, double score){
        for(Objective obj : objectives){
            if(obj.getName().equalsIgnoreCase(name)){
                obj.setScore(score);
                break;
            }
        }
    }

    public void setObjectiveScores(ArrayList<Double> values){
        for(int i=0;i<objectives.size();i++){
            objectives.get(i).setScore(values.get(i));
        }
    }

    public double getObjectiveScore(String name, double score){
        for(Objective obj : objectives){
            if(obj.getName().equalsIgnoreCase(name)){
                return obj.getScore();
            }
        }
        return -1.0;
    }

    /**
     * Returns true if this set of objectives dominates another set of objectives or false if not
     * @param  otherObjectives other genomes objectives
     * @return                 boolean value
     */
    public boolean dominates(MultiObjectiveScore otherObjectives){
        int nGreaterThan = 0;
        int nEqualTo = 0;
        ArrayList<Objective> otherObjectiveList = otherObjectives.getObjectives();
        if(objectives.size()!=otherObjectiveList.size()){
            throw new RuntimeException("Different number of objectives");
        }
        else{
            for(int i=0;i<objectives.size();i++){
                if(objectives.get(i).compare(otherObjectives.getObjective(i)) == 1){
                    nGreaterThan += 1;
                }
                else if(objectives.get(i).compare(otherObjectives.getObjective(i)) == 0){
                    nEqualTo += 1;
                }
            }

            if(nEqualTo==objectives.size()){
                return false;
            }
            else if((nGreaterThan+nEqualTo)==objectives.size()){
                return true;
            }
            else if(nGreaterThan==objectives.size()){
                return true;
            }
            else return false;
        }
    }

    public int numberOfObjectives(){
        return objectives.size();
    }

    /**
     * Calculates the distance between two sets of objectives in objective space
     * @param  otherScore other genomes objective class
     * @return            double value for distance
     */
    public double distanceBetween(MultiObjectiveScore otherScore){
		double temp = 0;
		for(int i=0;i<objectives.size();i++){
			temp += Math.pow((objectives.get(i).getScore()-otherScore.getObjectives().get(i).getScore()), 2);
		}
		return Math.sqrt(temp);
	}

    @Override
    public String toString(){
        String temp = "";
        for(Objective obj : objectives){
            temp += "Objective: " + obj.getName() + " Score: " + obj.getScore() + "\n";
        }
        return temp;
    }
}
