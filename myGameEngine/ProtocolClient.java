package myGameEngine;

import java.util.UUID;
import java.util.Vector;
import java.util.Iterator;
import java.net.InetAddress;
import java.io.IOException;
import java.lang.String;
import ray.networking.client.GameConnectionClient;
import ray.rml.*;
import myGame.*;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game; // Used to call functions like create ghost avatar
	private UUID id; // Unique id for each player
	private Vector<GhostAvatar> ghostAvatars; // Store ghost avatars of all other players
	
	public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game)
	throws IOException
	{
		super(remAddr, remPort, pType);
		this.game = game;
		this.id = UUID.randomUUID();
		this.ghostAvatars = new Vector<GhostAvatar>();
	}
	@Override
	protected void processPacket(Object msg)
	{
		String strMessage = (String)msg;
		String[] messageTokens = strMessage.split(",");
		
		// Message processing
		// 0th Index contains type of message
		if(messageTokens.length > 0)
		{
			if(messageTokens[0].compareTo("join") == 0) // receive join
			{ // format: join,success or join,failure
				if(messageTokens[1].compareTo("success") == 0)
				{
					System.out.println("Connected to server successfully");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
					sendDetailsRequest();
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{
					game.setIsConnected(false);
				}
			}
			else if(messageTokens[0].compareTo("bye") == 0) // receive bye
			{ // format: bye,remoteId
				UUID ghostID = UUID.fromString(messageTokens[1]);
				//removeGhostAvatar(ghostID);
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
				updateGhostAvatars(ghostID, ghostPosition);
			}
			else if (messageTokens[0].compareTo("wantRequest") == 0)
			{ // format: wantRequest,requestorId
				System.out.println("Detail requested from server");
				sendWantRequestReply(messageTokens[1]);
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
	public void sendByeMessage()
	{ // etc….. 
	}
	public void sendDetailsForMessage(UUID remId, Vector3f pos)
	{ // etc….. 
	}
	public void sendMoveMessage(Vector3 pos)
	{ // format: (move,localId,x,y,z)
		try
		{
			String message = new String("move," + id.toString());
			message += "," + pos.x()+"," + pos.y() + "," + pos.z();
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

	public void updateGhostAvatars(UUID ghostID, Vector3 pos)
	{
		try
		{
			for (int i = 0; i < ghostAvatars.size(); i++)
			{
				GhostAvatar currElem = ghostAvatars.elementAt(i);
				System.out.println(ghostID.toString() + " checking " + currElem.getID().toString());
				if (currElem.getID().toString().compareTo(ghostID.toString()) == 0)
				{
					currElem.setPos(pos);
					return;
				}
			}	
		}
		catch (Exception e)
		{
			System.out.println("Error updating ghost avatars");
		}
	}
}
