package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import java.util.UUID;

public class GhostAvatar
{
	private UUID id;
	private SceneNode node;
	
	public GhostAvatar(UUID id)
	{
		this.id = id;
	}
	
	public UUID getID()
	{
		return id;
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

	// accessors and setters for id, node, entity, and position

}