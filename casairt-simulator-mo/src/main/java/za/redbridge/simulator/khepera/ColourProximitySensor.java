package za.redbridge.simulator.khepera;

import org.apache.commons.math3.distribution.GammaDistribution;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import java.awt.Color;
import za.redbridge.simulator.portrayal.ConePortrayal;
import za.redbridge.simulator.portrayal.Portrayal;

import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.sensor.sensedobjects.SensedObject;

/**
 * A rough estimation of the colour proximity sensor used in the Khepera III robot: the Vishay TCRT5000
 */
public class ColourProximitySensor extends AgentSensor {

    private static final float COLOUR_PROXIMITY_SENSOR_RANGE = 3.0f;
    private static final float COLOUR_PROXIMITY_SENSOR_FOV = 3.0f;

    public static final float RANGE = 3.0f;
    public static final float FIELD_OF_VIEW = 3.0f;

    private final GammaDistribution function = new GammaDistribution(2.5, 2.0);
    private int readingSize;

    public ColourProximitySensor(float bearing, float orientation, int readingSize) {
        this(bearing, orientation, COLOUR_PROXIMITY_SENSOR_RANGE, COLOUR_PROXIMITY_SENSOR_FOV, readingSize);
    }

    public ColourProximitySensor(float bearing, float orientation, float range, float fieldOfView, int readingSize) {
        super(bearing, orientation, range, fieldOfView);
        this.readingSize = readingSize;
    }

    @Override
    protected void provideObjectReading(List<SensedObject> sensedObjects, List<Double> output) {
        if (!sensedObjects.isEmpty()) {
            for(int i=0;i<readingSize;i++){
                if(i<sensedObjects.size()){
                    output.add(readingCurve(sensedObjects.get(0).getDistance()));
                }
                else{
                    output.add(0.0);
                }
            }
        } else {
            for(int i=0;i<readingSize;i++){
                output.add(0.0);
            }
        }
    }

    @Override
    public AgentSensor clone() {
        return new ColourProximitySensor(bearing, orientation, range, fieldOfView, readingSize);
    }

    @Override
    public int getReadingSize() {
        return this.readingSize;
    }

    @Override
    public void readAdditionalConfigs(Map<String, Object> stringObjectMap) throws ParseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getAdditionalConfigs() {
        return null;
    }

    protected double readingCurve(float distance) {
        // Output curve of the TCRT5000 seems to produce something like a Gamma distribution curve
        // See the datasheet for more information
        return Math.min(function.density(distance * 1000) * 6.64, 1.0);
    }

    @Override
    protected Portrayal createPortrayal() {
        return new ConePortrayal(range, fieldOfView, new Color(0, 0, 255, 50));
    }

}
