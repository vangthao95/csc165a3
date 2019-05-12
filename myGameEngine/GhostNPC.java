// Used for client side projection of NPC

package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import java.util.UUID;

public class GhostNPC
{
	private UUID id;
	private SceneNode node;
	private Entity entity;
	
	public GhostNPC(UUID id) // constructor
	{
		this.id = id;
	}
	
	public UUID getID()
	{
		return id;
	}
	
	public void setPos(Vector3 position)
	{
		node.setLocalPosition(position);
	}
	
	public Vector3 getPos(Vector3 position)
	{
		return node.getLocalPosition();
	}
	
	public void rotate(float rot, Vector3f axisOfRotation)
	{
		Angle rotAmt = Degreef.createFrom(rotation);
		node.rotate(rotAmt, axisOfRotation);	
	}
	
	public void setNode(SceneNode node)
	{
		this.node = node;
	}
	
	public SceneNode getNode()
	{
		return node;
	}

}