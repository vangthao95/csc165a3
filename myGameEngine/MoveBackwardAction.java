package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class MoveBackwardAction extends AbstractInputAction
{
	private Node avN;
	public MoveBackwardAction(Node n)
	{
		avN = n;
	}
	
	public void performAction(float time, Event e)
	{
		avN.moveBackward(0.01f);
	}
}