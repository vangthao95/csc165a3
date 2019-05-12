// This is a ghost avatar for the server to use to keep track of all of the players' positions.

package myGameEngine;

import ray.rml.*;
import java.util.UUID;

public class ServerGhostAvatar
{
	private UUID id;
	private float x, y, z;
	
	public ServerGhostAvatar(UUID id, float x, float y, float z)
	{
		this.id = id;
		this.x = x;
		this.y = y;
		this.z = z;
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
	
	// accessors and setters for id, node, entity, and position

}