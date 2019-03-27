package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rml.*;

public class MoveForwardAction extends AbstractInputAction
{
	private Node avN;
	private ProtocolClient protClient;
	public MoveForwardAction(Node n, ProtocolClient p)
	{
		avN = n;
		protClient = p;
	}
	
	public void performAction(float time, Event e)
	{
		avN.moveForward(0.01f);
		protClient.sendMoveMessage((Vector3f)avN.getLocalPosition());
	}
}