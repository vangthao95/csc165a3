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

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game; // Used to call functions like create ghost avatar
	private UUID id; // Unique id for each player
	private Vector<GhostAvatar> ghostAvatars; // Store ghost avatars of all other players
	private ConcurrentHashMap<UUID, GhostNPC> ghostNPCs;
	
	public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game)
	throws IOException
	{
		super(remAddr, remPort, pType);
		this.game = game;
		this.id = UUID.randomUUID();
		this.ghostAvatars = new Vector<GhostAvatar>();
		this.ghostNPCs = new ConcurrentHashMap<UUID, GhostNPC>();
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
			if (messageTokens[0].compareTo("NPC") == 0)
			{
				if (messageTokens[1].compareTo("moveNPC") == 0)
				{
					UUID npcID = UUID.fromString(messageTokens[2]);
					float x = Float.parseFloat(messageTokens[3]);
					float z = Float.parseFloat(messageTokens[5]);
					float y = game.getVericalPosition(x, z);
					Vector3 npcPos = Vector3f.createFrom(x, y, z);
					updateNpcMoveGhostAvatars(npcID, npcPos);
				}
			}
			if(messageTokens[0].compareTo("join") == 0) // receive join
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
			else if (messageTokens[0].compareTo("statusCheck") == 0)
			{ // format: statusCheck
				System.out.println("Status check received from server... sending reply...");
				sendStatusReply();
			}
			else if (messageTokens[0].compareTo("createNPC") == 0)
			{
				UUID GhostNPCID = UUID.fromString(messageTokens[1]);
				Vector3 pos = Vector3f.createFrom(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				createGhostNPC(GhostNPCID, pos);
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
			GhostAvatar newPlayer = new GhostAvatar(newPlayerID);
			game.addGhostAvatarToGameWorld(newPlayer, pos);
			ghostAvatars.add(newPlayer);

		}
		catch (Exception e)
		{
			System.out.println("Error creating ghost avatar");
			e.printStackTrace();
		}
	}
	
	public void createGhostNPC(UUID GhostNPCID, Vector3 pos)
	{
		try
		{
			GhostNPC newNPC = new GhostNPC(GhostNPCID);
			game.addGhostNPCToGameWorld(newNPC, pos);
			ghostNPCs.put(GhostNPCID, newNPC);
		}
		catch (Exception e)
		{
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
	
	public void updateNpcMoveGhostAvatars(UUID id, Vector3 pos)
	{
		GhostNPC curGhost = ghostNPCs.get(id);
		curGhost.setPos(pos);
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
}
