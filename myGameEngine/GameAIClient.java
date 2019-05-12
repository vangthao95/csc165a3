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
//import ray.rage.game.*;
//import ray.rage.Engine;
//import ray.rage.scene.*;
//import ray.rage.rendersystem.*;
//import ray.rage.scene.generic.GenericSceneManagerFactory;
import java.util.concurrent.ConcurrentHashMap;
//import ray.rage.scene.generic.GenericSceneManager;
import java.util.Timer; // Timer
import java.util.TimerTask; // Periodically scheduling a task
import java.util.Date; // Get date

public class GameAIClient extends GameConnectionClient
{
	private boolean isClientConnected = false;
	private NpcPlayer npcPlayer;
	private UUID id; // Unique id this ai client
	private ConcurrentHashMap<UUID, ServerGhostAvatar> listOfPlayers;
	//static private Tessellation tessellation;
	public GameAIClient(InetAddress remAddr, int remPort) throws IOException
	{
		super(remAddr, remPort, ProtocolType.UDP);
		this.id = UUID.randomUUID();
		npcPlayer = new NpcPlayer(this.id);
		npcPlayer.setPos(Vector3f.createFrom(0.0f,0.0f,0.0f));
		listOfPlayers = new ConcurrentHashMap<UUID, ServerGhostAvatar>();
	}
	
	/*static private void setTessellation(Tessellation t)
	{
		tessellation = t;
	}*/
	
	@Override
	public void processPacket(Object msg)
	{
		if (msg == null)
			return;
		String message = (String) msg;
		System.out.println(message);

		String[] messageTokens = message.split(",");
		if(messageTokens.length > 0)
		{
			// also additional cases for receiving messages about NPCs, such as:
			if(messageTokens[0].compareTo("join") == 0)
			{
				if(messageTokens[1].compareTo("success") == 0)
				{
					System.out.println("AI Client connected to server successfully");
					isClientConnected = true;
					sendCreateMessage();
					sendDetailsRequest();
				}
			}
			else if(messageTokens[0].compareTo("bye") == 0) // receive bye
			{ // format: bye,remoteId
				UUID ghostID = UUID.fromString(messageTokens[1]);
				removeGhostAvatar(ghostID);
			}
			else if (messageTokens[0].compareTo("create") == 0)
			{ // format: create,remoteId,x,y,z
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));

				createPlayerAvatar(ghostID, ghostPosition);
			}
			else if(messageTokens[0].compareTo("move") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
					
				//System.out.println("Received new move message for user " + ghostID.toString());
				updateGhostAvatar(ghostID, ghostPosition);
			}
			else if (messageTokens[0].compareTo("statusCheck") == 0)
			{ // format: statusCheck
				System.out.println("Status check received from server... sending reply...");
				sendStatusReply();
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
	
	public void updateGhostAvatar(UUID ghostAvaID, Vector3 pos)
	{
		ServerGhostAvatar curPlayerGhost = listOfPlayers.get(ghostAvaID);
		if (curPlayerGhost == null)
		{
			return;
		}
		curPlayerGhost.setPos(pos);
		
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
	
	public void sendCreateMessage()
	{
		try
		{
			String message = new String("createNPC," + id.toString());
			Vector3 pos = npcPlayer.getPos();
			message += "," + pos.x() + "," + pos.y() + "," + pos.z(); 
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void createPlayerAvatar(UUID ghostAvaID, Vector3 pos)
	{
		try
		{
			ServerGhostAvatar curPlayerGhost = new ServerGhostAvatar(ghostAvaID);
			curPlayerGhost.setPos(pos);
			listOfPlayers.put(ghostAvaID, curPlayerGhost);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void sendJoinMessage()
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
		try 
		{
			listOfPlayers.remove(ghostID);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// Couldnt figure out a way to create a local tessellation heightmap without 
	// implementing game class
	/*public void updateVerticalPos(GhostNPC npc, float x, float z)
	{
		
		// use avatar World coordinates to get coordinates for height
		Vector3 newAvatarPosition = Vector3f.createFrom(
			// Keep the X coordinate
			x,
			// The Y coordinate is the varying height
			tessellation.getWorldHeight(x,z),
			//Keep the Z coordinate
			z
		);
		
		// use avatar Local coordinates to set position, including height
		npc.setPos(newAvatarPosition);
	}*/
	
	public void moveNPC()
	{
		ServerGhostAvatar player = calculateClosestPlayer();
		if (player == null)
			return;
		Vector3 playerPos = player.getPos();
		Vector3 currPos = npcPlayer.getPos();
		Vector3 distVector = Vector3f.createFrom(
			currPos.x() - playerPos.x(), 
			0.0f, 
			currPos.z() - playerPos.z());
			
		float dist = distVector.length();
		if (dist > 1.0f)
		{
			distVector = Vector3f.createNormalizedFrom(distVector.x(), 0.0f, distVector.z());
			float newX = distVector.x()*0.01f;
			float newZ = distVector.z()*0.01f;
			float newY = 0.0f;
			Vector3 newPos = Vector3f.createFrom(currPos.x() + newX, newY, currPos.z() + newZ);
			npcPlayer.setPos(newPos);
			sendMoveMessageNPC(id, npcPlayer.getPos());
		}
	}
	
	public void sendMoveMessageNPC(UUID npcID, Vector3 pos)
	{
		try
		{
			String message = new String("NPC,moveNPC," + id.toString());
			message += "," + pos.x() + "," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public ServerGhostAvatar calculateClosestPlayer()
	{
		if (listOfPlayers.isEmpty())
			return null;
		ServerGhostAvatar curClosest;
		for (UUID key : listOfPlayers.keySet())
		{
			return listOfPlayers.get(key); // Get watever avatar will be changed later
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		GameAIClient AIClient = null;
		final GameAIClient client;
		
		
		/*GenericSceneManagerFactory smFactory = new GenericSceneManagerFactory();
		SceneManager sceneManager = smFactory.createInstance();
		Tessellation tessE;
		SceneNode tessN;
		Engine engine = new Engine();
		//engine.registerSceneManager(sceneManager);
		
		// Create ground so our ai can actually follow the ground
		tessE = sceneManager.createTessellation("tessE", 6);
		// subdivisions per patch: min=0, try up to 32
		tessE.setSubdivisions(8f);
		tessN = sceneManager.getRootSceneNode().createChildSceneNode("TessN");
		tessN.attachObject(tessE);
		tessN.scale(20, 40, 20);
		tessE.setHeightMap(engine, "heightmap1.jpeg");
		setTessellation(tessE);*/

		try
		{
			AIClient = new GameAIClient(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		client = AIClient;
		TimerTask moveNPC = new TimerTask()
		{
			public void run()
			{
				try 
				{
					client.moveNPC();
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
			}
		};

		Timer timer = new Timer("MoveNPC");
		
		timer.scheduleAtFixedRate(moveNPC, 0, 100);
		if (client == null)
		{
			System.out.println("missing protocol host");
		}
		else
		{
			// ask client protocol to send initial join message
			//to server, with a unique identifier for this client
			client.sendJoinMessage();
			
			while (true)
			{
				client.processPackets();
				//AIClient.moveNPC();
				Thread.yield();
			}
			
			/*Game testGame = new gameEngine();
			try 
			{
				testGame.startup();
				testGame.run();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				testGame.shutdown();
				testGame.exit();
			}*/
		}
		
	}
	
	/*public class gameEngine extends VariableFrameRateGame {
		@Override
		protected void update(Engine engine) 
		{
			while (true)
			{
				//AIClient.processPackets();
				//Thread.yield();
			}
		}
		
		@Override
		protected void setupCameras(SceneManager sm, RenderWindow rw)
		{
			
		}
		
		@Override
		protected void setupScene(Engine eng, SceneManager sm) throws IOException
		{
			
		}
	}*/
}