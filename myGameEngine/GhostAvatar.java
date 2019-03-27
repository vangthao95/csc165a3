package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import java.util.UUID;

public class GhostAvatar
{
	private UUID id;
	private SceneNode node;
	private Entity entity;
	
	public GhostAvatar(UUID id, Vector3 position)
	{
		this.id = id;
	}
	// accessors and setters for id, node, entity, and position

}