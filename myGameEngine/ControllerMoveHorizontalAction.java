package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class ControllerMoveHorizontalAction extends AbstractInputAction
{
	private Node avN;
	public ControllerMoveHorizontalAction(Node n)
	{
		avN = n;
	}
	
	public void performAction(float time, Event e)
	{
		float value = e.getValue();
		if (value > 0.2f)
		{
			
			avN.moveBackward(value/100.0f);
		}
		else if (value < -0.2f) 
		{
			avN.moveBackward(value/100.0f);
		}
		else 
		{
			return;
		}

	}
}