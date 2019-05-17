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
import ray.ai.behaviortrees.*;

public class NPCcontroller
{
	private float DISTANCE_TO_SPAWN = 10.0f; // distance to spawn npc away from player
	private Vector<GhostNPC> npcList;
	private MyGame game;
	private ProtocolClient protClient;
	private float timeSinceTick;
	private float timeSinceThink;
	private float lastUpdate;

	private Vector<GhostNPC> nearbyP;
	
	
	public NPCcontroller(MyGame g, ProtocolClient c)
	{
		game = g;
		protClient = c;
		npcList = new Vector<GhostNPC>();
		nearbyP = null;
		timeSinceTick = 0.0f;
		timeSinceThink = 0.0f;
		lastUpdate = 0.0f;
	}
	
	public void deleteNPC(UUID id)
	{
		for (GhostNPC npc : npcList)
		{
			UUID npcID = npc.getID();
			if (id.toString().compareTo(npcID.toString()) == 0)
			{
				protClient.sendDeleteMsg(npcID);
				SceneNode n = npc.getNode();
				npcList.remove(npc);
				game.destroyNPCObjects(n);
				return;
			}
		}
		System.out.println("NPC to delete is not found within the list of NPCs");
	}
	
	public void deleteNPC_ForNonControllers(UUID id)
	{
		for (GhostNPC npc : npcList)
		{
			UUID npcID = npc.getID();
			if (id.toString().compareTo(npcID.toString()) == 0)
			{
				SceneNode n = npc.getNode();
				npcList.remove(npc);
				game.destroyNPCObjects(n);
				return;
			}
		}
		System.out.println("NPC to delete is not found within the list of NPCs");
	}
	
	public void update(float elapsedTime)
	{
		float dt = elapsedTime - lastUpdate;
		timeSinceThink += dt;
		timeSinceTick += dt;
		
		if (getCount() == 0)
		{
			createNPC();
			createNPC();
			createNPC();
		}
		
		
		if (timeSinceTick > 50.0f) // tick
		{
			for (GhostNPC npc : npcList)
			{
				Vector3 pos = npc.getMoveTowards();
				if (pos != null)
				{
					npc.getNode().moveForward(0.01f);
					game.updateVerticalPos(npc.getNode());
					protClient.moveNPC(npc.getID(), npc.getPos());
				}
				
			}
			timeSinceTick = 0.0f;
		}
		if (timeSinceThink > 500.0f) // think
		{
			for (GhostNPC npc : npcList)
			{
				npc.getBT().update(elapsedTime);
			}
			timeSinceThink = 0.0f;
		}
		/*
		for (int i=0; i<numNPCs; i++)
		{
			NPClist[i].updateLocation();
		}*/
		lastUpdate = elapsedTime;
	}
	
	public BehaviorTree createBT(GhostNPC npc)
	{
		BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
		bt.insertAtRoot(new BTSequence(10));
		bt.insert(10, new playerNearBy(protClient,this,npc,false));
		bt.insert(10, new turnsTowardPos(protClient, this, npc));
		bt.insert(10, new walkTowardsPos(protClient, this, npc));
		return bt;
	}
	
	public void createNPC()
	{
		Random rand = new Random();// Randomize position
		Vector3 playerPos = game.getPlayerPosition();
		UUID newNPCid = UUID.randomUUID();
		float x = rand.nextFloat()* 2.0f - 1.0f;
		float z = rand.nextFloat() * 2.0f - 1.0f;
		
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
		BehaviorTree bt = createBT(newNPC);
		newNPC.setBT(bt);
		npcList.add(newNPC);
		protClient.addGhostNPC(newNPCid, pos);// Send create message to protocol client
	}
	
	public void createNPC(UUID id, Vector3 pos)
	{
		SceneNode npcNode = null;
		try
		{
			npcNode = game.getNPCnode(pos, id);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		GhostNPC newNPC = new GhostNPC(npcNode, id);
		npcList.add(newNPC);
	}
	
	public Vector3 getPlayerPosition()
	{
		return game.getPlayerPosition();
	}
	
	public int getCount()
	{
		return npcList.size();
	}
	
	public Iterator getIterator()
	{
		return npcList.iterator();
	}
	
	public void updateVerticalPos(SceneNode node)
	{
		game.updateVerticalPos(node);
	}
	
	public class walkTowardsPos extends BTAction
	{
		ProtocolClient protClient;
		NPCcontroller npcC;
		GhostNPC curNPC;
		
		public walkTowardsPos(ProtocolClient c, NPCcontroller npcC, GhostNPC npc)
		{
			protClient = c;
			this.npcC = npcC;
			curNPC = npc;
		}
		
		protected BTStatus update(float elapsedTime)
		{
			curNPC.getNode().moveForward(0.01f);
			npcC.updateVerticalPos(curNPC.getNode());
			protClient.moveNPC(curNPC.getID(), curNPC.getPos());
			
			return BTStatus.BH_SUCCESS;
		}
	}
	
	public class turnsTowardPos extends BTAction
	{
		ProtocolClient protClient;
		NPCcontroller npcC;
		GhostNPC curNPC;
		
		public turnsTowardPos(ProtocolClient c, NPCcontroller npcC, GhostNPC npc)
		{
			protClient = c;
			this.npcC = npcC;
			curNPC = npc;
		}
		
		protected BTStatus update(float elapsedTime)
		{
			curNPC.lookAt(curNPC.getMoveTowards(), Vector3f.createFrom(0.0f, 1.0f, 0.0f));
			protClient.rotateNPC(curNPC.getID(), curNPC.getMoveTowards());
			return BTStatus.BH_SUCCESS;
		}
	}
	
	public class playerNearBy extends BTCondition
	{
		ProtocolClient protClient;
		NPCcontroller npcC;
		GhostNPC curNPC;
		private float DISTANCE_TO_PLAYER = 3f;
		
		public playerNearBy(ProtocolClient c, NPCcontroller npcC, GhostNPC npc, boolean toNegate)
		{
			super(toNegate);
			protClient = c;
			this.npcC = npcC;
			curNPC = npc;
		}
		
		public void setDistance(float dist) // can adjust for difficulty
		{
			DISTANCE_TO_PLAYER = dist;
		}
		
		protected boolean check()
		{
			Iterator ghostPlayers = protClient.getGhostIter();
			Vector3 curPosNPC = curNPC.getPos();
			float closestLength = 9999.0f;
			boolean playerNear = false;
			
			while (ghostPlayers.hasNext())
			{
				GhostAvatar curP = (GhostAvatar) ghostPlayers.next();
				Vector3 pPos = curP.getPos();
				Vector3 dist = Vector3f.createFrom(pPos.x() - curPosNPC.x(), 0.0f, pPos.z() - curPosNPC.z()); 
				float length = dist.length();
				if (length <= DISTANCE_TO_PLAYER && length < closestLength && length > 0.35f) // keep track of closest player
				{
					playerNear = true;
					curNPC.setMoveTowards(pPos);
					closestLength = length;
				}
				
			}
			
			Vector3 pos = npcC.getPlayerPosition();
			Vector3 dist = Vector3f.createFrom(pos.x() - curPosNPC.x(), 0.0f, pos.z() - curPosNPC.z());
			float length = dist.length();
			if (length <= DISTANCE_TO_PLAYER && length < closestLength && length > 0.35f)
			{
				playerNear = true;
				curNPC.setMoveTowards(pos);
			}
			else if (length <= 0.36f)
			{
				if (curNPC.getTouchedPlayer() == false)
				{
					game.decScore();
					curNPC.setTouchedPlayer();
				}
			}
			
			if (playerNear == false)
			{
				curNPC.setMoveTowards(null);
			}
			return playerNear;
		}
	}
	
}