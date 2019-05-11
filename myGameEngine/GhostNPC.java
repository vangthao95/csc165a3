package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;

public class GhostNPC
{
	private int id;
	private SceneNode node;
	private Entity entity;
	
	public GhostNPC(int id, Vector3 position) // constructor
	{
		this.id = id;
	}
	
	public void setPosition(Vector3 position)
	{
		node.setLocalPosition(position);
	}
	
	public Vector3 getPosition(Vector3 position)
	{
		return node.getLocalPosition();
	}
	

}