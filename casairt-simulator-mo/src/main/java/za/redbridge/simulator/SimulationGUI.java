package za.redbridge.simulator;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import sim.util.Bag;
import za.redbridge.simulator.portrayal.*;
import za.redbridge.simulator.object.*;
import java.awt.geom.Ellipse2D;
import org.jbox2d.common.Vec2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 *  The simulation GUI used to demo controllers
 */

public class SimulationGUI extends GUIState {

    private Display2D display;
    private JFrame displayFrame;
    private ContinuousPortrayal2D environmentPortrayal = new ContinuousPortrayal2D();

    public SimulationGUI(SimState state) {
        super(state);
    }

    @Override
    public void init (Controller controller) {
        super.init(controller);

        display = new Display2D(600, 600, this) {
            @Override
            public boolean handleMouseEvent(MouseEvent event) {
                boolean returnB = super.handleMouseEvent(event);
                // System.out.println(event.getX() + " " + event.getY());
                return returnB;
            }
        };

        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("CASAIRT Simulation");

        controller.registerFrame(displayFrame);

        displayFrame.setVisible(true);
        display.attach(environmentPortrayal, "Construction Area");
    }

    @Override
    public void start() {
        super.start();

        // Set the portrayal to display the environment
        final Simulation simulation = (Simulation) state;
        environmentPortrayal.setField(simulation.getEnvironment());

        /**
        Adds invisible MASON portrayals for each instance of the resource, robot and targetArea objects
        => double click on an object in the simulation and it'll show the inspector for that object
        **/
        environmentPortrayal.setPortrayalForClass(ResourceObject.class, new LabelledPortrayal2D(new RectanglePortrayal2D(new Color(0,0,0,0)), null){
            @Override
            public void draw(Object object, java.awt.Graphics2D graphics, DrawInfo2D info) {
                ResourceObject res = (ResourceObject) object;
                if (res.isConstructed()) {
                    this.setLabelShowing(true);
                    this.setLabelScaling(5);
                    this.label = res.getType();
                    this.align = LabelledPortrayal2D.ALIGN_CENTER;
                    this.offsety = -10D;
                    Color resColor = (Color) res.getPortrayal().getPaint();
                    if(res.getPortrayal().getPaint()==null){
                        System.out.println("Resource: "+res+" portrayal is null");
                    }
                    double y = (299 * resColor.getRed() + 587 * resColor.getGreen() + 114 * resColor.getBlue()) / 1000;
                    Color toUse = y >= 128 ? Color.black : Color.white;
                    this.paint = toUse;
                }
                else {
                    this.setLabelShowing(false);
                }
                super.draw(object, graphics, info);
            }
        });

        environmentPortrayal.setPortrayalForClass(RobotObject.class, new OvalPortrayal2D(new Color(0,0,0,0)));
        environmentPortrayal.setPortrayalForClass(DetectionPoint.class, new OvalPortrayal2D(0.2D) {
            @Override
            public void draw(Object object, java.awt.Graphics2D graphics, DrawInfo2D info) {
                super.draw(object, graphics, info);
            }
        });

        // Set up the display
        display.reset();
        display.setBackdrop(Color.white);
        display.repaint();
    }

    @Override
    public boolean step() {
        final Simulation simulation = (Simulation) state;
        // Checks if all resources are collected and stops the simulation
        // if (simulation.allResourcesCollected()) {
        //     simulation.finish();
        //     simulation.start();
        //     start();
        // }
        // System.out.println(simulation.getStepNumber());

        // if(simulation.getStepNumber() == simulation.getSimulationIterations()){
        //     simulation.checkConstructionTask();
        //     simulation.finish();
        // }

        return super.step();
    }

    @Override
    public void quit() {
        super.quit();

        if (displayFrame != null) {
            displayFrame.dispose();
        }

        displayFrame = null;
        display = null;
    }

}
