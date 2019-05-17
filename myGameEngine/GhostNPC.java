// Used for client side projection of NPC

package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import java.util.UUID;
import ray.ai.behaviortrees.*;

public class GhostNPC
{
	private UUID id;
	private SceneNode node;
	private Vector3 towardsLoc;
	private BehaviorTree bt;
	private boolean touchPlayer = false;
	
	public GhostNPC(SceneNode node, UUID id) // constructor
	{
		this.id = id;
		this.node = node;
		towardsLoc = null;
	}
	
	public void setBT(BehaviorTree bt)
	{
		this.bt = bt;
	}
	
	public BehaviorTree getBT()
	{
		return bt;
	}
	
	public void setMoveTowards(Vector3 pos)
	{
		towardsLoc = pos;
	}
	
	public Vector3 getMoveTowards()
	{
		return towardsLoc;
	}
	
	public UUID getID()
	{
		return id;
	}
	
	public void setPos(Vector3 position)
	{
		node.setLocalPosition(position);
	}
	
	public Vector3 getPos()
	{
		return node.getLocalPosition();
	}
	
	public void rotate(float rot, Vector3f axisOfRotation)
	{
		Angle rotAmt = Degreef.createFrom(rot);
		node.rotate(rotAmt, axisOfRotation);	
	}
	
	public void setNode(SceneNode node)
	{
		this.node = node;
	}
	
	public void lookAt(Vector3 pos, Vector3 up)
	{
		node.lookAt(pos, up);
	}
	
	public SceneNode getNode()
	{
		return node;
	}
	
	public void setTouchedPlayer()
	{
		touchPlayer = true;
	}
	
	public boolean getTouchedPlayer()
	{
		return touchPlayer;
	}

}