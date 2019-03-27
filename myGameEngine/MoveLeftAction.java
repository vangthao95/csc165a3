package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class MoveLeftAction extends AbstractInputAction
{
	private Node avN;
	public MoveLeftAction(Node n)
	{
		avN = n;
	}
	
	public void performAction(float time, Event e)
	{
		avN.moveLeft(-0.01f);
	}
}