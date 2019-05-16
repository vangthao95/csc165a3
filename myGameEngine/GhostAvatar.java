package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import java.util.UUID;

public class GhostAvatar
{
	private UUID id;
	private SceneNode node;
	
	public GhostAvatar(SceneNode node, UUID id)
	{
		this.id = id;
		this.node = node;
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

	public void rotate(float rotation, Vector3 axisOfRotation)
	{
		Angle rotAmt = Degreef.createFrom(rotation);
		node.rotate(rotAmt, axisOfRotation);
	}
	// accessors and setters for id, node, entity, and position

}