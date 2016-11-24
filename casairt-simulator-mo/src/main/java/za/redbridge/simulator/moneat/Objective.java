package za.redbridge.simulator.moneat;

import java.io.Serializable;

/**
 * Class used to store the score of an objective.
 */

public class Objective implements Serializable{

    private static final long serialVersionUID = 1L;

    private String name;
    private double score = 0;
    private boolean maximise;

    public Objective(String name, boolean maximise){
        this.name = name;
        this.maximise = maximise;
    }

    public Objective(Objective other){
        this.name = other.getName();
        this.maximise = other.getMaximise();
    }

    public String getName(){
        return this.name;
    }

    public boolean getMaximise(){
        return this.maximise;
    }

    public double getScore(){
        return this.score;
    }

    public void setScore(double newScore){
        this.score = newScore;
    }

    public void setName(String newName){
        this.name = newName;
    }

    /**
     * Compares two objectives based on if they need to be maximised or minimised
     * @param  o other objective
     * @return   -1, 0 or 1
     */
    public int compare(Objective o){
        if(name.equalsIgnoreCase(o.getName())){
            if(maximise){
                if(score > o.getScore()){
                    return 1;
                }
                else if(score == o.getScore()){
                    return 0;
                }
                else{
                    return -1;
                }
            }
            else{
                if(score < o.getScore()){
                    return 1;
                }
                else if(score == o.getScore()){
                    return 0;
                }
                else{
                    return -1;
                }
            }
        }
        else{
            return -2;
        }
    }

    @Override
	public String toString() {
		String string = this.name + ": " + this.score;
		return string;
	}
}
