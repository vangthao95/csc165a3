package myGameEngine;

import java.io.IOException;
import java.lang.InterruptedException;
import java.net.InetAddress;
import java.util.UUID;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;
import java.util.Timer; // Timer
import java.util.TimerTask; // Periodically scheduling a task
import java.util.Date; // Get date
import java.lang.Thread; // For sleep
import java.util.concurrent.ConcurrentHashMap; // getClients() returns this data structure
import java.util.Vector;
import ray.rml.Vector3f;
import ray.rml.Vector3;

public class GameServerUDP extends GameConnectionServer<UUID>
{
	// Interval to check status of clients in seconds
	private long CHECK_CLIENTS = 30;
	private Vector<UUID> clientsReplies;
	private ConcurrentHashMap<UUID, ServerGhostAvatar> listOfPlayers;
	
	public GameServerUDP(int localPort) throws IOException
	{
		super(localPort, ProtocolType.UDP);
		clientsReplies = new Vector<UUID>();
		listOfPlayers = new ConcurrentHashMap<UUID, ServerGhostAvatar>();
		checkForClients();

	}
 
	// Periodically check for connected clients
	private void checkForClients()
	{
		TimerTask checkForClientsTask = new TimerTask()
		{
			public void run()
			{
				try 
				{
					System.out.println("Send status check message on: " + new Date() + " on Thread's name: " + Thread.currentThread().getName());
					sendStatusCheckToAll(); // Send status check message to all clients
					Thread.sleep(5000); // Wait 5 seconds for replies
					removeClientsWithNoReply(); // Delete clients that didn't send a reply back
					//System.out.println("Delete clients on: " + new Date() + " on Thread's name: " + Thread.currentThread().getName());
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
			}
		};
		
		Timer timer = new Timer("Timer");
		
		timer.scheduleAtFixedRate(checkForClientsTask, 1000, CHECK_CLIENTS * 1000); // Start the checking
	}
	@Override
	public void processPacket(Object o, InetAddress senderIP, int sndPort)
	{
		String message = (String) o;
		System.out.println(message);
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
			/*else if(msgTokens[0].compareTo("bye") == 0)
			{
				UUID clientID = UUID.fromString(msgTokens[1]);
				System.out.println("Received bye message from id: " + clientID.toString());
				sendByeMessages(clientID);
				removeClient(clientID);
			}*/
			// format: move,localId,x,y,z,
			else if (msgTokens[0].compareTo("move") == 0)
			{
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
				System.out.println(clientID.toString() + " moved to x: " + pos[0] + " y: " + pos[1] + " z: " + pos[2]);
				sendMoveMessages(clientID, pos);
			}
			// format: rotate,localId,rot,x,y,z
			else if (msgTokens[0].compareTo("rotate") == 0)
			{
				UUID clientID = UUID.fromString(msgTokens[1]);
				String rot = msgTokens[2];
				String[] axis = {msgTokens[3], msgTokens[4], msgTokens[5]};
				System.out.println(clientID.toString() + " rotated " + rot + " degrees with respects to the axis (" + axis[0] + "," + axis[1] + "," + axis[2] + ")");
				sendRotateMessages(clientID, rot, axis);
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
			// format: statusReply,localId
			else if (msgTokens[0].compareTo("statusReply") == 0)
			{
				UUID clientID = UUID.fromString(msgTokens[1]);
				clientsReplies.add(clientID);
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
			
			// Also add it to the list of players 
			ServerGhostAvatar newPlayer = new ServerGhostAvatar(clientID, Float.parseFloat(position[0]), Float.parseFloat(position[1]), Float.parseFloat(position[2]));
			listOfPlayers.putIfAbsent(clientID, newPlayer);
			
			forwardPacketToAll(message, clientID);
		}
		catch(IOException e)
		{
			System.out.println("Error creating create messages");
			e.printStackTrace();
		}
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
			updateGhostAvatar(requestee, position);
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
			updateGhostAvatar(clientID, position);
			forwardPacketToAll(message, clientID);
		}
		catch (IOException e)
		{
			System.out.println("Error creating send move messages");
			e.printStackTrace();
		}
	}
	
	public void sendRotateMessages(UUID clientID, String rot, String[] axis)
	{ // format: rotate,localId,rot,x,y,z
		try
		{
			String message = new String("rotate," + clientID.toString());
			message += "," + rot;
			message += "," + axis[0];
			message += "," + axis[1];
			message += "," + axis[2];
			forwardPacketToAll(message, clientID);
		}
		catch (IOException e)
		{
			System.out.println("Error creating send rotate messages");
			e.printStackTrace();
		}
	}
	
	public void sendStatusCheckToAll()
	{
		try
		{
			if (clientsReplies.isEmpty() == false)			
			{
				clientsReplies.clear();
			}
			String message = new String("statusCheck");
			sendPacketToAll(message);
		}
		catch (IOException e)
		{
			System.out.println("Error creating status check message");
			e.printStackTrace();
		}
	}
	public void sendByeMessages(UUID clientID)
	{
		try
		{
			String message = new String("bye," + clientID.toString());
			sendPacketToAll(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void removeClientsWithNoReply()
	{
		try
		{
			ConcurrentHashMap<UUID, IClientInfo> clients = getClients(); // Get current clients
			// Remove clients from server
			for (UUID key : clients.keySet())
			{
				if (clientsReplies.contains(key) == false)
				{
						System.out.println("Client was not found: " + key);
						removeClient(key);
						sendByeMessages(key);
						listOfPlayers.remove(key);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void updateGhostAvatar(UUID ghostAvaID, String position[])
	{
		ServerGhostAvatar curPlayerGhost = listOfPlayers.get(ghostAvaID);
		if (curPlayerGhost == null)
		{
			return;
		}
		
		float x = Float.parseFloat(position[0]);
		float y = Float.parseFloat(position[1]);
		float z = Float.parseFloat(position[2]);
		
		Vector3 newPos = Vector3f.createFrom(x,y,z);
		curPlayerGhost.setPos(newPos);
		
	}
}