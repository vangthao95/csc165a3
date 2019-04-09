package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rml.*;
import myGame.*;

public class MoveBackwardAction extends AbstractInputAction
{
	private Node playerNode;
	private ProtocolClient protClient;
	private MyGame myGame;
	public MoveBackwardAction(Node n, ProtocolClient p, MyGame g)
	{
		playerNode = n;
		protClient = p;
		myGame = g;
	}
	
	public void performAction(float time, Event e)
	{
		playerNode.moveBackward(0.01f);
		myGame.updateVerticalPosition();
		protClient.sendMoveMessage(playerNode.getLocalPosition());		
	}
}