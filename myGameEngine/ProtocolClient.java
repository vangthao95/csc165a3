package myGameEngine;

import java.util.UUID;
import java.util.Vector;
import java.util.Iterator;
import java.net.InetAddress;
import java.io.IOException;
import java.lang.String;
import ray.networking.client.GameConnectionClient;
import java.util.concurrent.ConcurrentHashMap;
import ray.rml.*;
import myGame.*;
import java.util.Iterator;
import ray.rage.scene.*;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game; // Used to call functions like create ghost avatar
	private UUID id; // Unique id for each player
	private Vector<GhostAvatar> ghostAvatars; // Store ghost avatars of all other players
	//private Vector<GhostNPC> ghostNPCs; // Store NPCs
	private boolean handlingNPC;
	
	public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game)
	throws IOException
	{
		super(remAddr, remPort, pType);
		this.game = game;
		this.id = UUID.randomUUID();
		this.ghostAvatars = new Vector<GhostAvatar>();
		//this.ghostNPCs = new Vector<GhostNPC>();
		handlingNPC = false;
	}
	
	public Iterator getGhostIter()
	{
		return ghostAvatars.iterator();
	}
	
	@Override
	protected void processPacket(Object msg)
	{
		if (msg == null)
			return;
		String strMessage = (String)msg;
		String[] messageTokens = strMessage.split(",");
		System.out.println(strMessage);
		// Message processing
		// 0th Index contains type of message
		if(messageTokens.length > 0)
		{
			
			// NPC stuff maybe
			if (messageTokens[0].compareTo("NPC") == 0)
			{
				if (messageTokens[1].compareTo("move") == 0)
				{
					handleNPCMove(messageTokens);
				}
				else if (messageTokens[1].compareTo("rotate") == 0)
				{
					handleNPCrotate(messageTokens);
				}
				else if (messageTokens[1].compareTo("setAsClientHandlingNPC") == 0)
				{
					System.out.println("Processing NPC controller...");
					handlingNPC = true;
					game.initializeNPCcontroller();
			
				}
				else if (messageTokens[1].compareTo("createNPC") == 0)
				{
					System.out.println("A NPC has been created");
					UUID ghostID = UUID.fromString(messageTokens[3]);
					Vector3 pos = Vector3f.createFrom(
						Float.parseFloat(messageTokens[4]),
						Float.parseFloat(messageTokens[5]),
						Float.parseFloat(messageTokens[6]));
					createGhostNPC(ghostID, pos);
				}
				else if (messageTokens[1].compareTo("requestingInfo") == 0)
				{
					System.out.println("A client is requesting NPC info");
					UUID requestorID = UUID.fromString(messageTokens[2]);
					sendNPCInfoReply(requestorID);
				}
				else if (messageTokens[1].compareTo("requestingInfoReply") == 0)
				{
					processGhostInfoReply(messageTokens);
				}
			}
			else if (messageTokens[0].compareTo("statusCheck") == 0)
			{ // format: statusCheck
				System.out.println("Status check received from server... sending reply...");
				sendStatusReply();
			}
			else if (messageTokens[0].compareTo("bulletCreate") == 0)
			{
				UUID playerID = UUID.fromString(messageTokens[1]);
				UUID bulletID = UUID.fromString(messageTokens[2]);
				Vector3 pos = Vector3f.createFrom(
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]),
					Float.parseFloat(messageTokens[5]));
				Vector3 frontVector = Vector3f.createFrom(
					Float.parseFloat(messageTokens[6]),
					Float.parseFloat(messageTokens[7]),
					Float.parseFloat(messageTokens[8]));
				Vector3 currVector = Vector3f.createFrom(
					Float.parseFloat(messageTokens[9]),
					Float.parseFloat(messageTokens[10]),
					Float.parseFloat(messageTokens[11]));
				processShootMessage(playerID, bulletID, pos, frontVector, currVector);
			}
			else if (messageTokens[0].compareTo("bulletDelete") == 0)
			{
				UUID playerID = UUID.fromString(messageTokens[1]);
				UUID bulletID = UUID.fromString(messageTokens[2]);
				processBulletDelete(playerID, bulletID);
			}
			else if(messageTokens[0].compareTo("join") == 0) // receive join
			{ // format: join,success or join,failure
				if(messageTokens[1].compareTo("success") == 0)
				{
					System.out.println("Connected to server successfully");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
					sendDetailsRequest();
				}
			}
			else if(messageTokens[0].compareTo("bye") == 0) // receive bye
			{ // format: bye,remoteId
				UUID ghostID = UUID.fromString(messageTokens[1]);
				removeGhostAvatar(ghostID);
			}
			else if (messageTokens[0].compareTo("create")==0)
			{ // format: create,remoteId,x,y,z
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));

				createGhostAvatar(ghostID, ghostPosition);
			}
			else if (messageTokens[0].compareTo("move") == 0)
			{ // format: move,remoteId,x,y,z
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
					
				//System.out.println("Received new move message for user " + ghostID.toString());
				updateMoveGhostAvatars(ghostID, ghostPosition);
			}
			else if (messageTokens[0].compareTo("rotate") == 0)
			{ // format: rotate,remoteId,rot,x,y,z
				UUID ghostID = UUID.fromString(messageTokens[1]);
				float rot = Float.parseFloat(messageTokens[2]);
				Vector3 axisOfRotation = Vector3f.createFrom(
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]),
					Float.parseFloat(messageTokens[5]));
					
				updateRotateGhostAvatars(ghostID, rot, axisOfRotation);
			}
			else if (messageTokens[0].compareTo("wantRequest") == 0)
			{ // format: wantRequest,requestorId
				System.out.println("Detail requested from server");
				sendWantRequestReply(messageTokens[1]);
			}
			else if (messageTokens[0].compareTo("deleteNPC") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[2]);
				game.deleteGhostNPC2(ghostID);
			}
			else if (messageTokens[0].compareTo("bulletCollisionRequestDeleteNPC") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[2]);
				processGhostNPCDeleteRequest(ghostID);
			}
		}
	}
	public void sendJoinMessage() // format: join,localId
	{
		try
		{
			System.out.println("Sending Join packet...");
			sendPacket(new String("join," + id.toString()));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void sendDetailsRequest()
	{ // format: (wantRequest,localId)
		try
		{
			System.out.println("Requesting details from all connected clients");
			String message = new String("wantRequest,") + id.toString();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public void sendCreateMessage(Vector3 pos)
	{ // format: (create,localId,x,y,z)
		try
		{
			String message = new String("create," + id.toString());
			message += "," + pos.x()+"," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public void sendMoveMessage(Vector3 pos)
	{ // format: (move,localId,x,y,z)
		try
		{
			String message = new String("move," + id.toString());
			message += "," + pos.x()+ "," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public void sendRotateMessage(float rot, Vector3 axis)
	{ // format: (rotate,localId,rot,x,y,z)
		try
		{
			String message = new String("rotate," + id.toString());
			message += "," + rot + "," + axis.x() + "," + axis.y() + "," + axis.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void createGhostAvatar(UUID newPlayerID, Vector3 pos)
	{
		try
		{
			SceneNode ghostN = game.addGhostAvatarToGameWorld(pos, newPlayerID);
			GhostAvatar newPlayer = new GhostAvatar(ghostN, newPlayerID);
			ghostAvatars.add(newPlayer);

		}
		catch (Exception e)
		{
			System.out.println("Error creating ghost avatar");
			e.printStackTrace();
		}
	}
	
	public void sendWantRequestReply(String requestorId)
	{ // format: (wantReply,localId,requestorId,x,y,z)
		try
		{	
			String message = new String("wantReply," + id.toString());
			message += "," + requestorId;
			Vector3 pos = game.getPlayerPosition();
			message += "," + pos.x()+"," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			System.out.println("Error creating detail request reply");
			e.printStackTrace();
		}
		
	}

	public void updateMoveGhostAvatars(UUID ghostID, Vector3 pos)
	{
		try
		{
			for (int i = 0; i < ghostAvatars.size(); i++)
			{
				GhostAvatar currElem = ghostAvatars.elementAt(i);
				//System.out.println(ghostID.toString() + " checking " + currElem.getID().toString());
				if (currElem.getID().toString().compareTo(ghostID.toString()) == 0)
				{
					currElem.setPos(pos);
					return;
				}
			}	
		}
		catch (Exception e)
		{
			System.out.println("Error updating ghost avatar's position");
			e.printStackTrace();
		}
	}
	
	public void updateRotateGhostAvatars(UUID ghostID, float rot, Vector3 axisOfRotation)
	{
		try 
		{
			for (int i = 0; i < ghostAvatars.size(); i++)
			{
				GhostAvatar currElem = ghostAvatars.elementAt(i);
				if (currElem.getID().toString().compareTo(ghostID.toString()) == 0)
				{
					currElem.rotate(rot, axisOfRotation);
					return;
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error updating ghost avatar's rotation");
			e.printStackTrace();
		}
	}
	
	
	public void sendStatusReply()
	{ // format: (statusReply,localId)
		try
		{
			String message = new String("statusReply," + id.toString());
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void removeGhostAvatar(UUID ghostID)
	{
		GhostAvatar avatar = null;
		try 
		{
			for (int i = 0; i < ghostAvatars.size(); i++)
			{
				GhostAvatar currElem = ghostAvatars.elementAt(i);
				if (currElem.getID().toString().compareTo(ghostID.toString()) == 0)
				{
					avatar = currElem;
					break;
				}
			}
			game.removeGhostAvatar(avatar);
			ghostAvatars.remove(avatar);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void addGhostNPC(UUID ghostID, Vector3 pos)
	{
		try
		{
			String message = new String("NPC,");
			message += "createNPC,";
			message += id.toString() + ",";
			message += ghostID.toString() + ",";
			message += pos.x() + "," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		// Randomize a position away from player
	}
	
	/*public int getNPCcount()
	{
		return ghostNPCs.size();
	}*/
	
	public void sendNPCInfoReply(UUID requestorID)
	{
		Iterator iter = null;
		try
		{
			iter = game.getnpcIterator();
			if (iter == null) 
				return;
			String message = new String("NPC,requestingInfoReply," + id.toString() + "," + requestorID.toString());
			while (iter.hasNext())
			{
				System.out.println("Processing Ghosts to send info to other clients...");
				GhostNPC currNPC = (GhostNPC) iter.next();
				UUID npcID = currNPC.getID(); // ghost UUID
				Vector3 pos = currNPC.getPos();// ghost pos
				
				message += "," + npcID.toString();
				message += "," + pos.x() + "," + pos.y() + "," + pos.z();
			}
			message += ",endNPCInfo";
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void processGhostInfoReply(String[] messageTokens)
	{
		System.out.println("Ghost info reply successfully received");
		int i = 4;
		while (messageTokens[i].compareTo("endNPCInfo") != 0)
		{
			UUID npcID = UUID.fromString(messageTokens[i]);
			i++;
			float x = Float.parseFloat(messageTokens[i]);
			i++;
			float y = Float.parseFloat(messageTokens[i]);
			i++;
			float z = Float.parseFloat(messageTokens[i]);
			i++;
			Vector3 pos = Vector3f.createFrom(x, y, z);
			game.createNPC(npcID, pos);
		}
	}
	
	public void createGhostNPC(UUID GhostNPCID, Vector3 pos)
	{
		try
		{
			game.createNPC(GhostNPCID, pos);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void rotateNPC(UUID npcID, Vector3 pos)
	{
		try
		{
			String message = new String("NPC,rotate," + id.toString() + "," + npcID.toString());
			message += "," + pos.x() + "," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void handleNPCrotate(String[] messageTokens)
	{
		UUID npcID = UUID.fromString(messageTokens[3]);
		float x = Float.parseFloat(messageTokens[4]);
		float y = Float.parseFloat(messageTokens[5]);
		float z = Float.parseFloat(messageTokens[6]);
		Vector3 pos = Vector3f.createFrom(x,y,z);
		Iterator iter = null;
		iter = game.getnpcIterator();
		if (iter != null)
		{
			while (iter.hasNext())
			{
				GhostNPC curNPC = (GhostNPC) iter.next();
				if (curNPC.getID().toString().compareTo(npcID.toString()) == 0)
				{
					curNPC.lookAt(pos, Vector3f.createFrom(0.0f, 1.0f, 0.0f));
					return;
				}
			}
		}
	}
	
	public void moveNPC(UUID npcID, Vector3 pos)
	{
		try
		{
			String message = new String("NPC,move," + id.toString() + "," + npcID.toString());
			message += "," + pos.x() + "," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void handleNPCMove(String[] messageTokens)
	{
		UUID npcID = UUID.fromString(messageTokens[3]);
		float x = Float.parseFloat(messageTokens[4]);
		float y = Float.parseFloat(messageTokens[5]);
		float z = Float.parseFloat(messageTokens[6]);
		Vector3 pos = Vector3f.createFrom(x,y,z);
		Iterator iter = null;
		iter = game.getnpcIterator();
		if (iter != null)
		{
			while (iter.hasNext())
			{
				GhostNPC curNPC = (GhostNPC) iter.next();
				if (curNPC.getID().toString().compareTo(npcID.toString()) == 0)
				{
					curNPC.setPos(pos);
					return;
				}
			}
		}
	}
	
	public void sendDeleteMsg(UUID ghostID)
	{
		try
		{
			String message = new String("deleteNPC," + id.toString());
			message += "," + ghostID.toString();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void bulletCollisionRequestDelete(UUID ghostID)
	{
		try 
		{
			String message = new String("bulletCollisionRequestDeleteNPC," + id.toString());
			message += "," + ghostID.toString();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void processGhostNPCDeleteRequest(UUID ghostID)
	{
		game.deleteGhostNPC(ghostID);
	}
	
	public void sendShootMessage(UUID bulletID, Vector3 pos, Vector3 frontVector, Vector3 currVector)
	{
		try
		{
			String message = new String("bulletCreate," + id.toString());
			message += "," + bulletID.toString();
			message += "," + pos.x() + "," + pos.y() + "," + pos.z();
			message += "," + frontVector.x() + "," + frontVector.y() + "," + frontVector.z();
			message += "," + currVector.x() + "," + currVector.y() + "," + currVector.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void processShootMessage(UUID playerID, UUID bulletID, Vector3 pos, Vector3 frontVector, Vector3 currVector)
	{
		try
		{
			game.ghostShoot(playerID, bulletID, pos, frontVector, currVector);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void timeoutRemoveBullet(UUID bulletID)
	{
		try
		{
			String message = new String("bulletDelete," + id.toString());
			message += "," + bulletID.toString();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void processBulletDelete(UUID playerID, UUID bulletID)
	{
		game.ghostBulletDelete(playerID, bulletID);
	}
}
