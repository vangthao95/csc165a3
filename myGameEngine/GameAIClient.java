package myGameEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.Iterator;
import java.lang.String;
import ray.networking.IGameConnection.ProtocolType;
import java.util.Vector;
import java.net.UnknownHostException;
import ray.networking.client.GameConnectionClient;
import ray.rml.*;
import java.lang.Thread;

public class GameAIClient extends GameConnectionClient
{
	private boolean isClientConnected = false;
	private UUID id; // Unique id this ai client
	public GameAIClient(InetAddress remAddr, int remPort) throws IOException
	{
		super(remAddr, remPort, ProtocolType.UDP);
		this.id = UUID.randomUUID();
	}
	
	@Override
	public void processPacket(Object msg)
	{
		String message = (String) msg;
		System.out.println(message);
		String[] msgTokens = message.split(",");
		if(msgTokens.length > 0)
		{
			// also additional cases for receiving messages about NPCs, such as:
			if(msgTokens[0].compareTo("needNPC") == 0)
			{
				//. . .
			}
			else if(msgTokens[0].compareTo("collide") == 0)
			{
			//. . .
			}
		}
	}
	// game protocol as before, plus additional NPC protocol cases. i.e.,
	// messages regarding NPC’s sent to clients, such as:
	public void sendNPCinfo() // informs clients of new NPC positions
	{/*
		for (int i=0; i<npcCtrl.getNumOfNPCs(); i++)
		{
			try
			{
				String message = new String("mnpc," + Integer.toString(i));
				message += "," + (npcCtrl.getNPC(i)).getX();
				message += "," + (npcCtrl.getNPC(i)).getY();
				message += "," + (npcCtrl.getNPC(i)).getZ();
				sendPacketToAll(message);
				//. . .
			}
			
		}*/
	}
	
	private void sendJoinMessage()
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
	public static void main(String[] args)
	{
		GameAIClient AIClient = null;
		if(args.length > 1)
		{
			try
			{
				AIClient = new GameAIClient(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		if (AIClient == null)
		{
			System.out.println("missing protocol host");
		}
		else
		{
			// ask client protocol to send initial join message
			//to server, with a unique identifier for this client
			AIClient.sendJoinMessage();
			
			while (true)
			{
				AIClient.processPackets();
				Thread.yield();
			}
		}
		
	}
}