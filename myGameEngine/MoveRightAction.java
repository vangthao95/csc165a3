package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rml.*;

public class MoveRightAction extends AbstractInputAction
{
	private Node playerNode;
	private ProtocolClient protClient;
	public MoveRightAction(Node n, ProtocolClient p)
	{
		playerNode = n;
		protClient = p;
	}
	
	public void performAction(float time, Event e)
	{
		playerNode.moveLeft(0.01f);
		protClient.sendMoveMessage(playerNode.getLocalPosition());		
	}
}