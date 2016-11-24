package za.redbridge.simulator.neat;

import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.neat.NEATNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sim.util.Double2D;
import za.redbridge.simulator.phenotype.Phenotype;
import za.redbridge.simulator.Morphology;
import za.redbridge.simulator.sensor.AgentSensor;

/**
 * NEAT genome phenotype. Inputs sensor reading values to the ANN and returns the output
 */
public class NEATPhenotype implements Phenotype{

    private final MLData input;
    // ANN
    private final NEATNetwork network;
    private List<AgentSensor> sensors;
    private Morphology morphology;

    public NEATPhenotype(Morphology morphology, NEATNetwork network){
        this.morphology = morphology;
        this.network = network;
        int nSensor = morphology.getTotalReadingSize();
        sensors = new ArrayList<AgentSensor>();
        for(AgentSensor sensor : morphology.getSensorList()){
            sensors.add(sensor);
        }
        this.input = new BasicMLData(nSensor);
    }

    @Override
    public List<AgentSensor> getSensors(){
        return sensors;
    }

    /**
     * Allows the robots to be moved according to their ANN
     * @param  readings list of readings for each sensor
     * @return          Double2D value. Contains 2 values for left and right wheel
     */
    @Override
    public Double2D step(List<List<Double>> readings){
        final MLData input = this.input;

        List<Double> temp = new ArrayList<Double>();
        for(int i=0;i<readings.size();i++){
            for(int j=0;j<readings.get(i).size();j++){
                temp.add(readings.get(i).get(j));
            }
        }

        for(int i=0;i<input.size();i++){
            input.setData(i, temp.get(i));
        }
        MLData output = network.compute(input);
        Double2D result = new Double2D(output.getData(0) * 2.0 - 1.0, output.getData(1) * 2.0 - 1.0);
        return result;
    }

    @Override
    public Phenotype clone(){
        return new NEATPhenotype(this.morphology.clone(), network);
    }

    @Override
    public void configure(Map<String, Object> stringObjectMap) {
        throw new UnsupportedOperationException();
    }
}
