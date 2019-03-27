package myGameEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID>
{
	public GameServerUDP(int localPort) throws IOException
	{
		super(localPort, ProtocolType.UDP);
	}
 
	@Override
	public void processPacket(Object o, InetAddress senderIP, int sndPort)
	{
		String message = (String) o;
		String[] msgTokens = message.split(",");
		if(msgTokens.length > 0)
		{
			// case where server receives a JOIN message
			// format: join,localid
			if(msgTokens[0].compareTo("join") == 0)
			{
				try
				{
					IClientInfo ci;
					ci = getServerSocket().createClientInfo(senderIP, sndPort);
					UUID clientID = UUID.fromString(msgTokens[1]);
					System.out.println("Received join message from id: " + clientID.toString());
					addClient(ci, clientID);
					sendJoinedMessage(clientID, true);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			// case where server receives a CREATE message
			// format: create,localid,x,y,z
			else if(msgTokens[0].compareTo("create") == 0)
			{
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
				System.out.println("Received create message from id: " + clientID.toString()) ;
				sendCreateMessages(clientID, pos);
			}
			// case where server receives a BYE message
			// format: bye,localid
			else if(msgTokens[0].compareTo("bye") == 0)
			{
				UUID clientID = UUID.fromString(msgTokens[1]);
				System.out.println("Received bye message from id: " + clientID.toString());
				sendByeMessages(clientID);
				removeClient(clientID);
			}
			// format: move,localId,x,y,z,
			else if (msgTokens[0].compareTo("move") == 0)
			{
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
				System.out.println(clientID.toString() + " moved to x: " + pos[0] + " y: " + pos[1] + " z: " + pos[2]);
				sendMoveMessages(clientID, pos);
			}
			// format: wantReply,localId,requestorId,x,y,z
			else if (msgTokens[0].compareTo("wantReply") == 0)
			{
				UUID requestee = UUID.fromString(msgTokens[1]);
				System.out.println("Received details reply from id: " + requestee.toString());
				UUID requestor = UUID.fromString(msgTokens[2]);
				String[] pos = {msgTokens[3], msgTokens[4], msgTokens[5]};
				sendWantsDetailsReplies(requestor, requestee, pos);
			}
			// format: wantRequest,requestorId
			else if (msgTokens[0].compareTo("wantRequest") == 0)
			{
				UUID clientID = UUID.fromString(msgTokens[1]);
				sendWantsDetailsMessages(clientID);
			}
		}
	}
	public void sendJoinedMessage(UUID clientID, boolean success)
	{ // format: join,success or join,failure
		try
		{
			String message = new String("join,");
			if (success)
				message += "success";
			else
				message += "failure";
			sendPacket(message, clientID);
		}
		catch (IOException e)
		{
			System.out.println("Error creating join status reply");
			e.printStackTrace();
		}
	}
	public void sendCreateMessages(UUID clientID, String[] position)
	{ // format: create,remoteId,x,y,z
		try
		{
			String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		}
		catch(IOException e)
		{
			System.out.println("Error creating create messages");
			e.printStackTrace();
		}
	}
	public void sndDetailsMsg(UUID clientID, UUID remoteId, String[] position)
	{ // etc…..
	}
	
	// Request details from all clients besides the current clientID
	// requestorId here is localId
	public void sendWantsDetailsMessages(UUID clientID)
	{ // format: wantRequest,requestorId
		try
		{
			System.out.println(clientID.toString() + " is requesting details for all other clients...");
			String message = new String("wantRequest," + clientID.toString());
			forwardPacketToAll(message, clientID);
		}
		catch(IOException e)
		{
			System.out.println("Error creating detail request message");
			e.printStackTrace();
		}
	}
	
	public void sendWantsDetailsReplies(UUID requestor, UUID requestee, String[] position)
	{ // format: create,localId,x,y,z
		try
		{
			System.out.println("Sending details reply from " + requestee.toString() + " to " + requestor.toString());
			String message = new String("create," + requestee.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			sendPacket(message, requestor);
		}
		catch(IOException e)
		{
			System.out.println("Error creating message for detail replies");
			e.printStackTrace();
		}
	}
	public void sendMoveMessages(UUID clientID, String[] position)
	{ // format: move,localId,x,y,z
		try
		{
			String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		}
		catch (IOException e)
		{
			System.out.println("Error creating send move messages");
			e.printStackTrace();
		}
	}
	public void sendByeMessages(UUID clientID)
	{ // etc…..
	}
}