package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import java.util.UUID;

public class GhostNPC
{
	private UUID id;
	private SceneNode node;
	private Entity entity;
	
	public GhostNPC(int id, Vector3 position) // constructor
	{
		this.id = id;
	}
	
	public void setPosition(Vector3 position)
	{
		node.setLocatlPosition(position);
	}
	
	public void getPosition(Vector3 position)
	{
		return node.getLocatlPosition();
	}
}