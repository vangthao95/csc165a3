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

public class TestGameClient extends GameConnectionClient
{ 
	// same as before, plus code to handle additional NPC messages

	private Vector<GhostNPC> ghostNPCs;
	
	public TestGameClient(InetAddress remAddr, int remPort, ProtocolType pType)
	throws IOException
	{
		super(remAddr, remPort, pType);
		this.ghostNPCs = new Vector<GhostNPC>();
	}
	
	private void createGhostNPC(int id, Vector3 position)
	{
		GhostNPC newNPC = new GhostNPC(id, position);
		ghostNPCs.add(newNPC);
		//game.addGhostNPCtoGameWorld(newNPC);
	}
	
	private void updateGhostNPC(int id, Vector3 position)
	{
		ghostNPCs.get(id).setPosition(position);
	}
	
	protected void processPacket(Object msg)
	{
		String strMessage = (String)msg;
		String[] messageTokens = strMessage.split(",");
		
		// Message processing
		// 0th Index contains type of message
		if(messageTokens.length > 0)
		{

			// handle updates to NPC positions
			// format: (mnpc,npcID,x,y,z)
			if(messageTokens[0].compareTo("mnpc") == 0)
			{
				int ghostID = Integer.parseInt(messageTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[2]));
				updateGhostNPC(ghostID, ghostPosition);
			}
			
		}
	}
	
	public void askForNPCinfo()
	{	/*
		try
		{
			sendPacket(new String("needNPC," + id.toString()));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}*/
	}
}