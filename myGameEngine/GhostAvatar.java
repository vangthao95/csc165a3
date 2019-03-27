package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import java.util.UUID;

public class GhostAvatar
{
	private UUID id;
	private SceneNode node;
	private Vector3 initialPosition;
	
	public GhostAvatar(UUID id, Vector3 position)
	{
		this.id = id;
		initialPosition = position;
	}
	
	public UUID getID()
	{
		return id;
	}
	
	public Vector3 getInitialPosition()
	{
		return initialPosition;
	}
	
	public Vector3 getPos()
	{
		return node.getLocalPosition();
	}
	
	public void setPos(Vector3 newPos)
	{
		node.setLocalPosition(newPos);
	}
	
	public SceneNode getNode()
	{
		return node;
	}
	
	public void setNode(SceneNode newNode)
	{
		node = newNode;
	}

	public void setEntity(Entity newEntity)
	{
		node.attachObject(newEntity);
	}
	// accessors and setters for id, node, entity, and position

}