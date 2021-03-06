package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rml.*;
import myGame.*;

public class MoveRightAction extends AbstractInputAction
{
	private Node playerNode;
	private ProtocolClient protClient;
	private MyGame myGame;
	public MoveRightAction(Node n, ProtocolClient p, MyGame g)
	{
		playerNode = n;
		protClient = p;
		myGame = g;
	}
	
	public void performAction(float time, Event e)
	{
		Angle rotAmt = Degreef.createFrom(-0.70f);
		Vector3 u = playerNode.getLocalUpAxis();
		playerNode.rotate(rotAmt, u);	
		protClient.sendRotateMessage(rotAmt.valueDegrees(), u);
	}
}