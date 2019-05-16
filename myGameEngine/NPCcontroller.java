package myGameEngine;

import java.util.Random;
import myGame.*;
import ray.rml.*;
import java.util.Vector;
import ray.rage.scene.*;
import java.util.UUID;
import java.lang.Math; 
import java.io.IOException;
import java.util.Iterator;

public class NPCcontroller
{
	private float DISTANCE_TO_SPAWN = 10.0f; // distance to spawn npc away from player
	private Vector<GhostNPC> npcList;
	private MyGame game;
	private ProtocolClient protClient;
	
	
	public NPCcontroller(MyGame g, ProtocolClient c)
	{
		game = g;
		protClient = c;
		npcList = new Vector<GhostNPC>();
	}
	
	public void update()
	{
		if (getCount() == 0)
		{
			createNPC();
		}
		/*
		for (int i=0; i<numNPCs; i++)
		{
			NPClist[i].updateLocation();
		}*/
	}
	
	public void createNPC()
	{
		Random rand = new Random(System.currentTimeMillis());// Randomize position
		Vector3 playerPos = game.getPlayerPosition();
		UUID newNPCid = UUID.randomUUID();
		float x = rand.nextFloat();
		float z = rand.nextFloat();
		
		Vector3 dirVector = Vector3f.createFrom(x, 0.0f, z);
		Vector3 dirUnitVector = dirVector.normalize();
		float newX = dirUnitVector.x() * (float) Math.sqrt(DISTANCE_TO_SPAWN);
		float newZ = dirUnitVector.z() * (float) Math.sqrt(DISTANCE_TO_SPAWN);
		float newY = game.getVerticalPosition(newX, newZ);
		Vector3 pos = Vector3f.createFrom(newX, newY, newZ);
		SceneNode npcNode = null;
		try
		{
			npcNode = game.getNPCnode(pos, newNPCid);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		GhostNPC newNPC = new GhostNPC(npcNode, newNPCid);
		npcList.add(newNPC);
		protClient.addGhostNPC(pos);// Send create message to protocol client
	}
	
	public int getCount()
	{
		return npcList.size();
	}
	
	public Iterator getIterator()
	{
		return npcList.iterator();
	}
}