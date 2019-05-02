package myGameEngine;

import ray.rage.scene.*;
import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import ray.rml.*;
import myGame.*;

public class MoveForwardAction extends AbstractInputAction
{
	private Node playerNode;
	private ProtocolClient protClient;
	private MyGame myGame;
	public MoveForwardAction(Node n, ProtocolClient p, MyGame g)
	{
		playerNode = n;
		protClient = p;
		myGame = g;
	}
	
	public void performAction(float time, Event e)
	{
		playerNode.moveForward(0.01f);
		//System.out.println(playerNode.getLocalPosition().x());
		myGame.throwGernade();
		myGame.updateVerticalPosition();
		protClient.sendMoveMessage(playerNode.getLocalPosition());
	}
}