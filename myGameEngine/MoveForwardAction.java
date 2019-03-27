package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class MoveForwardAction extends AbstractInputAction
{
	private Node avN;
	public MoveForwardAction(Node n)
	{
		avN = n;
	}
	
	public void performAction(float time, Event e)
	{
		avN.moveForward(0.01f);
	}
}