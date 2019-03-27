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
	private MyGame game;
	private UUID id;
	private Vector<GhostAvatar> ghostAvatars;
	
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
		if(messageTokens.length > 0)
		{
			if(messageTokens[0].compareTo("join") == 0) // receive join
			{ // format: join, success or join, failure
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
			{ // format: bye, remoteId
				UUID ghostID = UUID.fromString(messageTokens[1]);
				//removeGhostAvatar(ghostID);
			}
			else if (messageTokens[0].compareTo("create")==0)
			{ // format: create, remoteId, x,y,z or dsfr, remoteId, x,y,z
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));

				createGhostAvatar(ghostID, ghostPosition);
			}
			else if (messageTokens[0].compareTo("move") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				System.out.println("Here");
				Vector3 ghostPosition = Vector3f.createFrom(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				System.out.println("Received new move message for user " + ghostID.toString());
			}
			else if (messageTokens[0].compareTo("wantRequest") == 0)
			{
				System.out.println("Detail requested from server");
				wantRequestReply(messageTokens[1]);
			}
		}
	}
	public void sendJoinMessage() // format: join, localId
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
	{
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
	{ // format: (create, localId, x,y,z)
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
	{
		try
		{
			String message = new String("move," + id.toString());
			message += "," + pos.x()+"," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void createGhostAvatar(UUID newPlayerID, Vector3 pos)
	{
		/*if (ghostAvatars != null)
		{
			Iterator iterate_value = ghostAvatars.iterator();
			while (iterate_value.hasNext())
			{
				GhostAvatar current = (GhostAvatar)iterate_value.next();
				if (current.getID() == newPlayerID)
					return;
			}
		}*/
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
	
	// Data format:
	// [0] type of request
	// [1] requestee (self)
	// [2] requestor (client requesting the details)
	// [3] x position
	// [4] y position
	// [5] z position
	public void wantRequestReply(String clientRequestingDetails)
	{
		try
		{	
			String message = new String("wantReply," + id.toString());
			message += "," + clientRequestingDetails;
			Vector3 pos = game.getPlayerPosition();
			message += "," + pos.x()+"," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}		
}
