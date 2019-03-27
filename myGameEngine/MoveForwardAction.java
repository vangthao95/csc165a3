package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rml.*;

public class MoveForwardAction extends AbstractInputAction
{
	private Node playerNode;
	private ProtocolClient protClient;
	public MoveForwardAction(Node n, ProtocolClient p)
	{
		playerNode = n;
		protClient = p;
	}
	
	public void performAction(float time, Event e)
	{
		playerNode.moveForward(0.01f);
		//System.out.println(playerNode.getLocalPosition().x());
		protClient.sendMoveMessage(playerNode.getLocalPosition());
	}
}