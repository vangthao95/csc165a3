// Used for client side projection of NPC

package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import java.util.UUID;

public class NpcPlayer
{
	private UUID id;
	private float x, y, z;
	
	public NpcPlayer(UUID id) // constructor
	{
		this.id = id;
	}
	public UUID getID()
	{
		return id;
	}
	
	public Vector3 getPos()
	{
		return Vector3f.createFrom(x,y,z);
	}
	
	public void setPos(Vector3 newPos)
	{
		x = newPos.x();
		y = newPos.y();
		z = newPos.z();
	}
}