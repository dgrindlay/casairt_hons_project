package za.redbridge.simulator.khepera;

import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.object.WallObject;
import za.redbridge.simulator.portrayal.ConePortrayal;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.sensor.sensedobjects.SensedObject;

import java.awt.*;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Sensor responsible for detecting walls
 */
public class WallCollisionSensor extends AgentSensor
{
    private static final float COLOR_SENSOR_RANGE = 1.0f;
    private static final float COLOR_SENSOR_FOV = 1.8f;

    public static final float RANGE = 2.0f;
    public static final float FIELD_OF_VIEW = 1.5f;

    private static final Paint color = new Color(100, 255, 0, 100);
    private int readingSize;

    public WallCollisionSensor(float bearing, float orientation, int readingSize)
    {
        this(bearing, orientation, COLOR_SENSOR_RANGE, COLOR_SENSOR_FOV, readingSize);
    }

    public WallCollisionSensor(float bearing, float orientation, float range, float fieldOfView, int readingSize)
    {
        super(bearing, orientation, range, fieldOfView);
        this.readingSize = readingSize;
    }

    /**
     * Converts a list of objects that have been determined to fall within the sensor's range into
     * a list of readings in the range [0.0, 1.0].
     *
     * @param sensedObjects the objects in the sensor's field, *sorted by distance*
     * @param output  the output vector for this sensor. Write the sensor output to this list (which
     */
    @Override
    protected void provideObjectReading(List<SensedObject> sensedObjects, List<Double> output)
    {
        if (!sensedObjects.isEmpty()) {
            SensedObject closest = sensedObjects.get(0);
            if (closest.getObject() instanceof ResourceObject)
            {
                // System.out.println("Resource detected");
                ResourceObject temp = (ResourceObject) closest.getObject();
                if(temp.isConstructed()){
                    output.add(1.0);
                }
                else{
                    output.add(0.0);
                }
            }
            else if (closest.getObject() instanceof WallObject)
            {
                output.add(1.0);
            }
            else output.add(0.0);
        }
        else
        {
            output.add(0.0);
        }
    }

    @Override
    public int getReadingSize()
    {
        return readingSize;
    }

    @Override
    public void readAdditionalConfigs(Map<String, Object> stringObjectMap) throws ParseException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getAdditionalConfigs()
    {
        return null;
    }

    @Override
    public AgentSensor clone() {
        return new WallCollisionSensor(bearing, orientation, range, fieldOfView, readingSize);
    }

    @Override
    protected Portrayal createPortrayal() {
        return new ConePortrayal(range, fieldOfView, color);
    }
}
