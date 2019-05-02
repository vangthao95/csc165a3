package myGame;

import myGameEngine.*;
// --------------From DolphinClick---------------
//import java.awt.*; // Split into Color, DisplayMode, and GraphicsEnvironment
import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.event.*;
import java.io.*;

import ray.rage.*;
import ray.rage.game.*;
import ray.rage.rendersystem.*;
import ray.rage.rendersystem.Renderable.*;
import ray.rage.scene.*;
import ray.rage.scene.Camera.Frustum.*;
import ray.rage.scene.controllers.*;
import ray.rml.*;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.scene.SkeletalEntity.EndType;
import static ray.rage.scene.SkeletalEntity.EndType.*;
// -----------------------------------------------

// ----------------From InputActions--------------
import ray.rage.rendersystem.states.*;
import ray.rage.asset.texture.*;
import ray.input.*;
import ray.input.action.*;
import java.awt.geom.*;
// -----------------------------------------------

import ray.rage.rendersystem.shader.GpuShaderProgram; // For GpuShaderProgram
import java.nio.*; // For FloatBuffer and IntBuffer
import ray.rage.util.*; // For BufferUtil direct float and int buffers
import java.util.Random; // For random numbers

import myGameEngine.*;

import net.java.games.input.Controller;

// Networking begin
import ray.networking.IGameConnection.ProtocolType;
import java.util.Vector;
import java.util.UUID;
import java.net.InetAddress;
import java.net.UnknownHostException;
// Networking end

// Scripting begin
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.List;
// Scripting end

// Physics begin
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsObject;
import ray.physics.PhysicsEngineFactory;
// Physics end
public class MyGame extends VariableFrameRateGame {
	// NPCs variables
	private int uniqueGhostNPCs = 0;
	//
	
	
	
	// Physics variables
	private SceneNode ball1Node, ball2Node, groundNode;
	Entity grenadeE;
	SceneNode grenadeN;
	PhysicsObject grenadePhysics;
	private PhysicsEngine physicsEng;
	private PhysicsObject ball1PhysObj, ball2PhysObj, gndPlaneP;
	private boolean running = false;
	private final static String GROUND_E = "Ground";
	private final static String GROUND_N = "GroundNode";
	private boolean grenadeExist = false;
	// End of Physics variables
	
	// Script files
	File rotationD2RC = new File("scripts/InitParams.js");
	File helloWorldS = new File("scripts/hello.js");
	// End of script files

	// Variables associated with scripts
	ScriptEngineManager factory = new ScriptEngineManager();
	ScriptEngine jsEngine = factory.getEngineByName("js"); // Game engine
	RotationController testRC; // InitParam.js
	Long rotationD2RCLastModifiedTime; // Modified time for rotationD2RC script
	
	// End of variables associated with scripts

	// to minimize variable allocation in update()
	Camera3Pcontroller orbitController1, orbitController2;
	StretchController player1controller;
	CustomController player2controller;
	GL4RenderSystem rs;
	private float elapsTime = 0.0f;
	private int counter, score = 0;
	private Camera camera;
	private SceneNode avatar1, object1, object2;
	private SceneNode cameraN1;
	// skybox
	private static final String SKYBOX_NAME = "MySkyBox";
	private boolean skyBoxVisible = true;

	String kbName;
	
	private InputManager im;
	
	// Networking begin
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected;
	private Vector<UUID> gameObjectsToRemove;
	// Networking end
	
	private SceneManager sceneManager;
	private int uniqueGhosts = 0;
	
	// Terrain Variables
	private SceneNode tessN;
	private Tessellation tessE;
	// End of terrain variables
	
    public MyGame(String serverAddr, int sPort)
	{
        super();
		this.serverAddress = serverAddr;
		this.serverPort = sPort;
		this.serverProtocol = ProtocolType.UDP;
		
		System.out.println("press w, a, s, d to move the avatar");
		System.out.println("press the up, down, left, and right arrow to move the camera");
		
	}

    public static void main(String[] args)
	{
        Game game = new MyGame(args[0], Integer.parseInt(args[1]));
        try {
            game.startup();
            game.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            game.shutdown();
            game.exit();
        }
		
    }

	private void executeScript(File scriptFileName)
	{
		try
		{
			FileReader fileReader = new FileReader(scriptFileName);
			jsEngine.eval(fileReader); //execute the script statements in the file
			fileReader.close();
		}
		catch (FileNotFoundException e1)
		{
			System.out.println(scriptFileName + " not found " + e1);
		}
		catch (IOException e2)
		{
			System.out.println("IO problem with " + scriptFileName + e2);
		}
		catch (ScriptException e3)
		{
			System.out.println("ScriptException in " + scriptFileName + e3);
		}
		catch (NullPointerException e4)
		{
			System.out.println ("Null ptr exception in " + scriptFileName + e4);
		}
	}
	
	
	private void setupNetworking()
	{
		gameObjectsToRemove = new Vector<UUID>();
		isClientConnected = false;
		try
		{
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		}
		catch (UnknownHostException e) 
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		if (protClient == null)
		{
			System.out.println("missing protocol host");
		}
		else
		{
			// ask client protocol to send initial join message
			//to server, with a unique identifier for this client
			protClient.sendJoinMessage();
		}
	}
	
	public void setIsConnected(boolean value)
	{
		isClientConnected = value;
	}
	
	protected void processNetworking(float elapsTime)
	{
		// Process packets received by the client from the server
		if (protClient != null)
		{
			//System.out.println("Telling protocol client to process packets...");
			protClient.processPackets();
		}
		
		// remove ghost avatars for players who have left the game
		/*Iterator<UUID> it = gameObjectsToRemove.iterator();
		while(it.hasNext())
		{
			//sm.destroySceneNode(it.next().toString());
		}
		gameObjectsToRemove.clear();*/
	}
	
	public void addGhostAvatarToGameWorld(GhostAvatar avatar, Vector3 pos)
	throws IOException
	{
		if (avatar != null)
		{
			uniqueGhosts++;
			Entity ghostE = sceneManager.createEntity("ghost" + uniqueGhosts, "avatar_v1.obj");
			ghostE.setPrimitive(Primitive.TRIANGLES);
			SceneNode ghostN = sceneManager.getRootSceneNode().createChildSceneNode(avatar.getID().toString());
			ghostN.attachObject(ghostE);
			ghostN.setLocalPosition(pos);
			ghostN.scale(0.05f, 0.05f, 0.05f);
			avatar.setNode(ghostN);
		}
	}
	/*
	// NPC function
	public void addGhostNPCtoGameWorld(GhostNPC ghostAvatar, Vector3 pos)
	throws IOException
	{
		if (ghostAvatar != null)
		{
			uniqueGhostNPCs++;
			Entity ghostE = sceneManager.createEntity("ghostNPC" + uniqueGhostNPCs, "avatar_v1.obj");
			ghostE.setPrimitive(Primitive.TRIANGLES);
			SceneNode ghostN = sceneManager.getRootSceneNode().createChildSceneNode(avatar.getID().toString());
			ghostN.attachObject(ghostE);
			ghostN.setLocalPosition(pos);
			ghostN.scale(0.05f, 0.05f, 0.05f);
			avatar.setNode(ghostN);
		}
	}
	*/
	public void removeGhostAvatarFromGameWorld(GhostAvatar avatar)
	{
		//if(avatar != null) gameObjectsToRemove.add(avatar.getID());
	}
	
	/*private class SendCloseConnectionPacketAction extends AbstractInputAction
	{
		// for leaving the game... need to attach to an input device
		@Override
		public void performAction(float time, Event e)
		{
			if(protClient != null && isClientConnected == true)
			{
				protClient.sendByeMessage();
			}
		}
	}*/
	
    @Override
    protected void setupScene(Engine eng, SceneManager sm) throws IOException
	{
		// Physics test objects
		// Ball 1
		Entity ball1Entity = sm.createEntity("ball1", "earth.obj");
		ball1Node = sm.getRootSceneNode().createChildSceneNode("Ball1Node");
		//ball1Node.attachObject(ball1Entity);
		ball1Node.setLocalPosition(0, 2, -2);
		// Ball 2
		Entity ball2Entity = sm.createEntity("Ball2", "sphere.obj");
		ball2Node = sm.getRootSceneNode().createChildSceneNode("Ball2Node");
		//ball2Node.attachObject(ball2Entity);
		ball2Node.setLocalPosition(-1,10,-2);
		// Ground plane
		Entity groundEntity = sm.createEntity(GROUND_E, "cube.obj");
		groundNode = sm.getRootSceneNode().createChildSceneNode(GROUND_N);
		groundNode.attachObject(groundEntity);
		// End of physics test objects
		
		
    	ScriptEngineManager factory = new ScriptEngineManager();
		// get a list of the script engines on this platform
		List<ScriptEngineFactory> list = factory.getEngineFactories();
		System.out.println("Script Engine Factories found:");
		for (ScriptEngineFactory f : list)
		{
			System.out.println(" Name = " + f.getEngineName()
			+ " language = " + f.getLanguageName()
			+ " extensions = " + f.getExtensions());
		}
		
		// run hello world script
		executeScript(helloWorldS);
		
		// Run the InitParams.js script to initialize spinSpeed
		executeScript(rotationD2RC);
		rotationD2RCLastModifiedTime = rotationD2RC.lastModified();
		// Initialize the rotation controller with the variable spinSpeed
		testRC = new RotationController(Vector3f.createUnitVectorY(),
				((Double)(jsEngine.get("spinSpeed"))).floatValue());
    	
    	// set up sky box
    	Configuration conf = eng.getConfiguration();
    	TextureManager tm = getEngine().getTextureManager();
    	tm.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path"));
    	Texture front = tm.getAssetByPath("zpos.png");
    	Texture back = tm.getAssetByPath("zneg.png");
    	Texture left = tm.getAssetByPath("xneg.png");
    	Texture right = tm.getAssetByPath("xpos.png");
    	Texture top = tm.getAssetByPath("ypos.png");
    	Texture bottom = tm.getAssetByPath("yneg.png");
    	 tm.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));
    	 
    	// cubemap textures are flipped upside-down.
    	// All textures must have the same dimensions, so any image�s
    	// heights will work since they are all the same height
    	AffineTransform xform = new AffineTransform();
    	xform.translate(0, front.getImage().getHeight());
    	xform.scale(1d, -1d);
    	front.transform(xform);
    	back.transform(xform);
    	left.transform(xform);
    	right.transform(xform);
    	top.transform(xform);
    	bottom.transform(xform);
    	SkyBox sb = sm.createSkyBox(SKYBOX_NAME);
    	sb.setTexture(front, SkyBox.Face.FRONT);
    	sb.setTexture(back, SkyBox.Face.BACK);
    	sb.setTexture(left, SkyBox.Face.LEFT);
    	sb.setTexture(right, SkyBox.Face.RIGHT);
    	sb.setTexture(top, SkyBox.Face.TOP);
    	sb.setTexture(bottom, SkyBox.Face.BOTTOM);
    	sm.setActiveSkyBox(sb);
    	
		
		//animations
		SkeletalEntity manSE =
		sm.createSkeletalEntity("manAv", "robot.rkm", "robot.rks");
		Texture tex = sm.getTextureManager().getAssetByPath("robot.png");
		TextureState tstate = (TextureState) sm.getRenderSystem()
		.createRenderState(RenderState.Type.TEXTURE);
		tstate.setTexture(tex);
		manSE.setRenderState(tstate);
		// attach the entity to a scene node
		avatar1= sm.getRootSceneNode().createChildSceneNode(manSE.getName()+"Node");
		avatar1.attachObject(manSE);
		avatar1.moveBackward(2.0f);
		//avatar1.translate(0.0f,2.0f,0.0f);
		avatar1.scale(0.1f,0.1f,0.1f);
		
		
		// load animations
		manSE.loadAnimation("throwAnimation", "throwing.rka");
		//manSE.loadAnimation("waveAnimation", "wave.rka");

		
    	
    	
		sceneManager = sm;
		
		/*Entity avatar1E = sm.createEntity("avatar1", "avatar_v1.obj");
        avatar1E.setPrimitive(Primitive.TRIANGLES);
        */
		Entity object1E = sm.createEntity("object1", "monster1_textured.obj");
        object1E.setPrimitive(Primitive.TRIANGLES);
/*
        avatar1 = sm.getRootSceneNode().createChildSceneNode(avatar1E.getName() + "Node");
		avatar1.scale(0.05f, 0.05f, 0.05f);
        avatar1.attachObject(avatar1E);
  */      
        object1 = sm.getRootSceneNode().createChildSceneNode(object1E.getName() + "Node");
        object1.moveForward(1.0f);
        object1.attachObject(object1E);
		object1.scale(0.05f, 0.05f, 0.05f);
		// Add dolphin 2 to rotation controller
		
		
		Entity object2E = sm.createEntity("object2", "dolphinHighPoly.obj");
		object2 = sm.getRootSceneNode().createChildSceneNode(object2E.getName() + "Node");
        object2E.setPrimitive(Primitive.TRIANGLES);
		//object2.attachObject(object2E);
		object2.moveUp(.5f);

        //sm.getAmbientLight().setIntensity(new Color(.5f, .5f, .5f));
		Light sunLight = sm.createLight("sunLight", Light.Type.POINT);
		sunLight.setAmbient(new Color(.5f, .5f, .5f));
        sunLight.setDiffuse(new Color(.7f, .7f, .7f));
		sunLight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
        sunLight.setRange(10f);
		
		SceneNode sunLightNode = avatar1.createChildSceneNode("sunLightNode");
        sunLightNode.attachObject(sunLight);
	
        player1controller = new StretchController();
		player2controller = new CustomController();
		sm.addController(player1controller);
		sm.addController(player2controller);
		setupNetworking();
		setupInputs();
		setupOrbitCamera(eng, sm);
		
		tessE = sm.createTessellation("tessE", 6);
		// subdivisions per patch: min=0, try up to 32
		tessE.setSubdivisions(8f);
		tessN = sm.getRootSceneNode().createChildSceneNode("TessN");
		tessN.attachObject(tessE);
		tessN.scale(20, 40, 20);
		tessE.setHeightMap(this.getEngine(), "heightmap1.jpeg");
		tessE.setTexture(this.getEngine(), "hexagons.jpeg");
		
		// Physics
		initPhysicsSystem();
		createRagePhysicsWorld();
		
		// Initial vertical update of player so
		// player doesn't have to move to get above ground
		updateVerticalPosition();
		
		// Initial vertical update of monster
		updateVerticalPos(object1);
    }
	
	// Physics Function
	private void initPhysicsSystem()
	{
		String engine = "ray.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = {0, -3f, 0};
		physicsEng = PhysicsEngineFactory.createPhysicsEngine(engine);
		physicsEng.initSystem();
		physicsEng.setGravity(gravity);
	}
    
	// Physics Function
	private void createRagePhysicsWorld()
	{
		float mass = 1.0f;
		float up[] = {0,1,0};
		double[] temptf;
		/*temptf = toDoubleArray(ball1Node.getLocalTransform().toFloatArray());
		ball1PhysObj = physicsEng.addSphereObject(physicsEng.nextUID(),
			mass, temptf, 2.0f);
		ball1PhysObj.setBounciness(1.0f);
		ball1Node.setPhysicsObject(ball1PhysObj);
		temptf = toDoubleArray(ball2Node.getLocalTransform().toFloatArray());
		ball2PhysObj = physicsEng.addSphereObject(physicsEng.nextUID(),
			mass, temptf, 2.0f);
		ball2PhysObj.setBounciness(1.0f);
		ball2Node.setPhysicsObject(ball2PhysObj);*/
		temptf = toDoubleArray(groundNode.getLocalTransform().toFloatArray());
		gndPlaneP = physicsEng.addStaticPlaneObject(physicsEng.nextUID(),
			temptf, up, 0.0f);
		gndPlaneP.setBounciness(1.0f);
		groundNode.scale(3f, .05f, 3f);
		groundNode.setLocalPosition(0, -7, -2);
		groundNode.setPhysicsObject(gndPlaneP);
		// can also set damping, friction, etc.
	}
	private int MONSTER_STATE = 1;
	// 1 walk towards player
	// 2 turn in a random direction
	// 3 walk in the direction it is currently facing
	// 4 look at player
	private void updateMonster()
	{
		Vector3 up = Vector3f.createUnitVectorY();
		if (MONSTER_STATE == 1)
		{
			object1.lookAt(avatar1, up);
			Vector3 currPos = object1.getLocalPosition();
			Vector3 playerPos = getPlayerPosition();
			Vector3 distVector = Vector3f.createFrom(currPos.x() - playerPos.x(), currPos.y() - playerPos.y(), currPos.z() - playerPos.z());
			float dist = distVector.length();
			if (dist > 1.0f)
			{
				object1.moveForward(0.01f);
				updateVerticalPos(object1);
			}
		}
		else if (MONSTER_STATE == 2)
		{
			Random r = new Random();
			float degreesToTurn = r.nextInt(11) - 5;
			Angle rotAmt = rotAmt = Degreef.createFrom(degreesToTurn);
			object1.rotate(rotAmt, up);
		}
		else if (MONSTER_STATE == 3)
		{
			object1.moveForward(0.01f);
		}
		else if (MONSTER_STATE == 4)
		{
			object1.lookAt(avatar1, up);
		}
	}
	
    protected void setupInputs()
    {
    	im =  new GenericInputManager();
    	String gpName = im.getFirstGamepadName();
    	
		Action quitGameAction = new QuitGameAction(this);
		Action incrementCounter = new IncrementCounterAction(this);
		Action CameraLookLeftRightA = new CameraLookLeftRightAction(camera);
		Action cameraLookUpDownA = new CameraLookUpDownAction(camera);
		Action p1MoveForwardA = new MoveForwardAction(avatar1, protClient, this);
		Action p1MoveBackwardA = new MoveBackwardAction(avatar1, protClient, this);
		Action p1MoveLeftA = new MoveLeftAction(avatar1, protClient, this);
		Action p1MoveRightA = new MoveRightAction(avatar1, protClient, this);

		Action p2MoveVerticalA = new ControllerMoveHorizontalAction(object1);
		Action p2MoveHorizontalA = new ControllerMoveVerticalAction(object1);
    	
		for (int i = 0; i < 10; i++)
		{
			Controller keyboard = im.getKeyboardController(i);
			if (keyboard == null)
				continue;

			
			im.associateAction(keyboard,
					net.java.games.input.Component.Identifier.Key.W,
					p1MoveForwardA,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					
			im.associateAction(keyboard,
					net.java.games.input.Component.Identifier.Key.S,
					p1MoveBackwardA,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					
			im.associateAction(keyboard,
					net.java.games.input.Component.Identifier.Key.A,
					p1MoveLeftA,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					
			im.associateAction(keyboard,
					net.java.games.input.Component.Identifier.Key.D,
					p1MoveRightA,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
		}
    	//im.associateAction(kbName,net.java.games.input.Component.Identifier.Key.ESCAPE,quitGameAction,InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		//im.associateAction(kbName,net.java.games.input.Component.Identifier.Key.I,quitGameAction,InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		//im.associateAction(kbName,net.java.games.input.Component.Identifier.Key.C,incrementCounter,InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		for (int i = 0; i < 10; i++)
		{
			Controller consoleController = im.	getGamepadController(i);
			if (consoleController == null)
				continue;
			
			im.associateAction(consoleController,
					net.java.games.input.Component.Identifier.Axis.Y,
					p2MoveVerticalA,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				
			im.associateAction(consoleController,
					net.java.games.input.Component.Identifier.Axis.X,
					p2MoveHorizontalA,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			/*	
			im.associateAction(consoleController,
					net.java.games.input.Component.Identifier.Key.A,
					p1MoveLeftA,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					
			im.associateAction(consoleController,
					net.java.games.input.Component.Identifier.Key.D,
					p1MoveRightA,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);*/
		}
		
		/*
		// Camera look left and right
		im.associateAction(gpName,
				net.java.games.input.Component.Identifier.Axis.RX,
				CameraLookLeftRightA,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN );
		
		// Camera look up and down
		im.associateAction(gpName,
				net.java.games.input.Component.Identifier.Axis.RY,
				cameraLookUpDownA,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN );

		*/
		// Controller camera look left
		/*im.associateAction(gpName,
				net.java.games.input.Component.Identifier.Button._0,
				p1MoveForwardA,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
  */
    }
	
	@Override
	protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
		rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
	}

    @Override
    protected void setupCameras(SceneManager sm, RenderWindow rw) {	   	
    	SceneNode rootNode = sm.getRootSceneNode();
    	camera = sm.createCamera("MainCamera",Projection.PERSPECTIVE);
    	rw.getViewport(0).setCamera(camera);
		camera.setRt((Vector3f)Vector3f.createFrom(1.0f, 0.0f, 0.0f));
		camera.setUp((Vector3f)Vector3f.createFrom(0.0f, 1.0f, 0.0f));
		camera.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, -1.0f));
		camera.setPo((Vector3f)Vector3f.createFrom(0.0f, 0.0f, 0.0f));
    	cameraN1 = rootNode.createChildSceneNode("MaincameraN1");
    	cameraN1.attachObject(camera);
    	camera.getFrustum().setFarClipDistance(1000.0f);
		camera.setMode('n');
    }
	
    protected void setupOrbitCamera(Engine eng, SceneManager sm)
    {
    	String gpName = im.getFirstGamepadName();
		String kbName = im.getKeyboardName();
		orbitController1 = new Camera3Pcontroller(camera, cameraN1, avatar1, kbName, im);
    }
	
	public void incrementCounter()
	{
		counter++;
	}

	public Vector3 getPlayerPosition()
	{
		return avatar1.getLocalPosition();
	}
	float deleteGrenadeTime = 0.0f;
	float npcStateChangeTime = 0.0f;
	@Override
    protected void update(Engine engine) {
		im.update(elapsTime);
		processNetworking(elapsTime);
		orbitController1.updateCameraPosition();
		//orbitController2.updateCameraPosition();
		rs = (GL4RenderSystem) engine.getRenderSystem();
		elapsTime += engine.getElapsedTimeMillis();
		
		// run script again in update() to demonstrate dynamic modification
		long modTime = rotationD2RC.lastModified();
		if (modTime > rotationD2RCLastModifiedTime)
		{
			rotationD2RCLastModifiedTime = modTime;
			executeScript(rotationD2RC);
			testRC.setSpeed(((Double)(jsEngine.get("spinSpeed"))).floatValue());
			System.out.println("Dolphin 2 rotation speed updated");
		}
		
		// NPC logic and update
		npcStateChangeTime += engine.getElapsedTimeMillis();
		if (npcStateChangeTime > 5000) // Change npc state every 5 seconds
		{
			Random r = new Random();
			int chanceOfMonsterState =  r.nextInt(100) + 1;
			// 10% monster walk towards player
			// 30% monster turn in a random direction
			// 15% monster look in the direction it is facing
			// 45% monster look at player and doesnt move
			if (chanceOfMonsterState >= 1 || chanceOfMonsterState <= 10) // 1-10
				MONSTER_STATE = 1;
			else if (chanceOfMonsterState >= 11 || chanceOfMonsterState <= 40) // 11-40
				MONSTER_STATE = 2;
			else if (chanceOfMonsterState >= 41 || chanceOfMonsterState <= 55) // 41-55
				MONSTER_STATE = 3;
			else if (chanceOfMonsterState >= 56 || chanceOfMonsterState <= 100) // 56-100
				MONSTER_STATE = 4;
		}
		updateMonster();
		
		// Physics
		float time = engine.getElapsedTimeMillis();
		
		if (grenadeExist)
		{
			deleteGrenadeTime += engine.getElapsedTimeMillis();
			//System.out.println(deleteGrenadeTime);
			if (deleteGrenadeTime > 2000)
			{
				deleteGrenadeTime = 0.0f;
				deleteGrenade();
			}
		}

		if (running)
		{
			Matrix4 mat;
			physicsEng.update(time);
			for (SceneNode s : engine.getSceneManager().getSceneNodes())
			{
				if (s.getPhysicsObject() != null)
				{
					mat = Matrix4f.createFrom(toFloatArray(s.getPhysicsObject().getTransform()));
					s.setLocalPosition(mat.value(0,3), mat.value(1,3), mat.value(2,3));
				}
			}
		}

		
		// update the animation
		SkeletalEntity manSE =
		(SkeletalEntity) sceneManager.getEntity("manAv");
		//SceneNode avatarN = sm.getSceneNode("manAvNode");
		manSE.update();
		
		
		// End of physics
	} // End of update()
	
	int counterBullets = 0;
	public void shoot() throws IOException
	{
		SkeletalEntity manSE =(SkeletalEntity) sceneManager.getEntity("manAv");
		manSE.stopAnimation();
		manSE.playAnimation("throwAnimation", 5.0f, STOP, 0);
		
		SceneNode rootNode = sceneManager.getRootSceneNode();
		Entity bulletE = sceneManager.createEntity("grenade1E" + counterBullets, "sphere.obj");
		SceneNode bulletN = rootNode.createChildSceneNode("grenade1N" + counterBullets);
		bulletN.attachObject(bulletE);
		bulletN.scale(0.1f, 0.1f, 0.1f);
		Vector3 loc = avatar1.getLocalPosition();
		bulletN.setLocalPosition(loc.x(), loc.y()+0.3f, loc.z());
		float mass = 1.0f;
		float up[] = {0,1,0};
		double[] temptf;
		temptf = toDoubleArray(bulletN.getLocalTransform().toFloatArray());
		PhysicsObject bulletPhysics = physicsEng.addSphereObject(physicsEng.nextUID(),
			mass, temptf, 0.3f);
		bulletPhysics.setBounciness(0.5f);
		bulletN.setPhysicsObject(bulletPhysics);
		
		Vector3f frontV = (Vector3f)avatar1.getLocalForwardAxis();
		Vector3 frontVector = frontV.normalize();
		Vector3 currVector = bulletN.getLocalPosition();
		
		//System.out.println("X: " + frontVector.x() + "Y: " + frontVector.y() + "Z: " + frontVector.z());
		bulletPhysics.applyForce(frontVector.x() * 500.0f, 0.0f, frontVector.z() * 500.0f, currVector.x(), 0.0f, currVector.z());
		//System.out.println("Grenade: " + bulletPhysics.getFriction());
		counterBullets++;
	}
	
	public void throwGrenade() throws IOException
	{
		SkeletalEntity manSE =(SkeletalEntity) sceneManager.getEntity("manAv");
		manSE.stopAnimation();
		manSE.playAnimation("throwAnimation", 2.0f, STOP, 0);
		
		SceneNode rootNode = sceneManager.getRootSceneNode();
		grenadeE = sceneManager.createEntity("grenade1E", "sphere.obj");
		grenadeN = rootNode.createChildSceneNode("grenade1N");
		grenadeN.attachObject(grenadeE);
		grenadeN.scale(0.1f, 0.1f, 0.1f);
		Vector3 loc = avatar1.getLocalPosition();
		grenadeN.setLocalPosition(loc.x(), loc.y()+0.3f, loc.z());
		float mass = 1.0f;
		float up[] = {0,1,0};
		double[] temptf;
		temptf = toDoubleArray(grenadeN.getLocalTransform().toFloatArray());
		grenadePhysics = physicsEng.addSphereObject(physicsEng.nextUID(),
			mass, temptf, 0.3f);
		grenadePhysics.setBounciness(0.5f);
		grenadeN.setPhysicsObject(grenadePhysics);
		
		Vector3f frontV = (Vector3f)avatar1.getLocalForwardAxis();
		Vector3 frontVector = frontV.normalize();
		Vector3 currVector = grenadeN.getLocalPosition();
		
		//System.out.println("X: " + frontVector.x() + "Y: " + frontVector.y() + "Z: " + frontVector.z());
		grenadePhysics.applyForce(frontVector.x() * 150.0f, 50.0f, frontVector.z() * 150.0f, currVector.x(), 0.0f, currVector.z());
		//System.out.println("Grenade: " + grenadePhysics.getFriction());
		grenadeExist = true;
	}
	
	public void deleteGrenade()
	{
		sceneManager.destroyEntity("grenade1E");
		sceneManager.destroySceneNode("grenade1N");
		grenadeExist = false;
	}
	
	public void updateVerticalPosition()
	{
		// avatar1
		//SceneNode tessN = this.getEngine().getSceneManager().getSceneNode("tessN");
		//Tessellation tessE = ((Tessellation) tessN.getAttachedObject("tessE"));
		// Figure out Avatar's position relative to plane
		Vector3 worldAvatarPosition = avatar1.getWorldPosition();
		Vector3 localAvatarPosition = avatar1.getLocalPosition();
		
		// use avatar World coordinates to get coordinates for height
		Vector3 newAvatarPosition = Vector3f.createFrom(
			// Keep the X coordinate
			localAvatarPosition.x(),
			// The Y coordinate is the varying height
			tessE.getWorldHeight(
			worldAvatarPosition.x(),
			worldAvatarPosition.z()) +.1f,
			//Keep the Z coordinate
			localAvatarPosition.z()
		);
		
		// use avatar Local coordinates to set position, including height
		avatar1.setLocalPosition(newAvatarPosition);
	}
	
	public void updateVerticalPos(SceneNode obj)
	{
		Vector3 worldAvatarPosition = obj.getWorldPosition();
		Vector3 localAvatarPosition = obj.getLocalPosition();
		
		// use avatar World coordinates to get coordinates for height
		Vector3 newAvatarPosition = Vector3f.createFrom(
			// Keep the X coordinate
			localAvatarPosition.x(),
			// The Y coordinate is the varying height
			tessE.getWorldHeight(
			worldAvatarPosition.x(),
			worldAvatarPosition.z()),
			//Keep the Z coordinate
			localAvatarPosition.z()
		);
		
		// use avatar Local coordinates to set position, including height
		obj.setLocalPosition(newAvatarPosition);
	}
	
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_SPACE:
				if (running == false)
				{
					System.out.println("Starting Physics!");
					running = true;
				}
				else
				{
					System.out.println("Stopping Physics!");
					running = false;
				}
				break;
				case KeyEvent.VK_G:
				if ((running) && (grenadeExist == false))
				{
					try
					{
						throwGrenade();
					}
					catch (IOException exception)
					{
						exception.printStackTrace();
					}
				}
				break;
				case KeyEvent.VK_F:
				if (running)
				{
					try
					{
						shoot();
					}
					catch (IOException exception)
					{
						exception.printStackTrace();
					}
				}
		}
		super.keyPressed(e);
	}
	
	private float[] toFloatArray(double[] arr)
	{
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++)
		{
			ret[i] = (float)arr[i];
		}
		return ret;
	}
		
	private double[] toDoubleArray(float[] arr)
	{
			if (arr == null) return null;
			int n = arr.length;
			double[] ret = new double[n];
			for (int i = 0; i < n; i++)
			{
				ret[i] = (double)arr[i];
			}
		return ret;
	}
}

	