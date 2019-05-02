package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rml.*;
import myGame.*;

public class MoveLeftAction extends AbstractInputAction
{
	private Node playerNode;
	private ProtocolClient protClient;
	private MyGame myGame;
	public MoveLeftAction(Node n, ProtocolClient p, MyGame g)
	{
		playerNode = n;
		protClient = p;
		myGame = g;
	}
	
	public void performAction(float time, Event e)
	{
		Angle rotAmt = Degreef.createFrom(0.30f);
		Vector3 u = playerNode.getLocalUpAxis();
		playerNode.rotate(rotAmt, u);
	}
}