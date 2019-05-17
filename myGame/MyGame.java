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
import ray.audio.*;
import com.jogamp.openal.ALFactory;
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

import java.util.UUID;
import java.util.Iterator;
import java.util.Vector;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
public class MyGame extends VariableFrameRateGame {
	SkeletalEntity manSE;
	
	// NPCs variables
	private int uniqueGhostNPCs = 0;
	private NPCcontroller npcController;
	private boolean controller;
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
	private Vector<Bullet> bullets = new Vector<Bullet>();
	private ConcurrentHashMap<UUID, Vector<Bullet>> ghostBulletList = new ConcurrentHashMap<UUID, Vector<Bullet>>();
	// End of Physics variables
	
	// Script files
	File initParameters = new File("scripts/InitParams.js");
	File helloWorldS = new File("scripts/hello.js");
	File cameraSpeedS = new File("scripts/cameraSpeeds.js");
	// End of script files

	// Variables associated with scripts
	ScriptEngineManager factory = new ScriptEngineManager();
	ScriptEngine jsEngine = factory.getEngineByName("js"); // Game engine
	RotationController testRC; // InitParam.js
	Long initParametersLastModifiedTime; // Modified time for initParameters script
	Long cameraSpeedLastModifiedtime;
	private float oldX = 0.0f;
	private float oldY = 0.0f;
	private float oldZ = 0.0f;
	private float oldUpdown = 0.5f;
	private float oldLeftright = 0.5f;
	// End of variables associated with scripts

	// to minimize variable allocation in update()
	Camera3Pcontroller orbitController1, orbitController2;
	StretchController player1controller;
	CustomController player2controller;
	GL4RenderSystem rs;
	float elapsTime = 0.0f;
	String elapsTimeStr, counterStr, dispStr;
	int elapsTimeSec, counter = 0;
	private Camera camera;
	private SceneNode avatar1, object1, object2, ufo, ufo2;
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
	// Networking end
	
	private SceneManager sceneManager;
	
	// Terrain Variables
	private SceneNode tessN;
	private Tessellation tessE;
	// End of terrain variables
	
	//Sound
	private IAudioManager audioMgr;
	private Sound throwingSound, bgSound, alienSound;
	
    public MyGame(String serverAddr, int sPort)
	{
        super();
		this.serverAddress = serverAddr;
		this.serverPort = sPort;
		this.serverProtocol = ProtocolType.UDP;
		
		System.out.println("press w, a, s, d to move the avatar");
		System.out.println("press the up, down, left, and right arrow to move the camera");
		
		cameraSpeedLastModifiedtime = cameraSpeedS.lastModified();
		initParametersLastModifiedTime = initParameters.lastModified();
		controller = false;
		npcController = null;
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
	
	public SceneNode addGhostAvatarToGameWorld(Vector3 pos, UUID id)
	throws IOException
	{
		//animations
		SkeletalEntity ghostSE = sceneManager.createSkeletalEntity("ghostSE" + id.toString(), "robot.rkm", "robot.rks");
		Texture tex = sceneManager.getTextureManager().getAssetByPath("robot.png");
		TextureState tstate = (TextureState) sceneManager.getRenderSystem()
		.createRenderState(RenderState.Type.TEXTURE);
		tstate.setTexture(tex);
		ghostSE.setRenderState(tstate);
		// attach the entity to a scene node
		SceneNode ghostAva = sceneManager.getRootSceneNode().createChildSceneNode("ghostAva"+id.toString());
		ghostAva.attachObject(ghostSE);
		ghostAva.setLocalPosition(pos);
		//avatar1.translate(0.0f,2.0f,0.0f);
		ghostAva.scale(0.1f,0.1f,0.1f);
		//ghostSE.loadAnimation("throwAnimation", "throwing.rka")
		return ghostAva;
		
		
		/*Entity ghostE = sceneManager.createEntity("ghostE" + id.toString(), "avatar_v1.obj");
		ghostE.setPrimitive(Primitive.TRIANGLES);
		SceneNode ghostN = sceneManager.getRootSceneNode().createChildSceneNode("ghostN" + id.toString());
		ghostN.attachObject(ghostE);
		ghostN.setLocalPosition(pos);
		ghostN.scale(0.05f, 0.05f, 0.05f);
		return ghostN;*/
	}
	
	public void removeGhostAvatar(GhostAvatar avatar)
	{
		if (avatar != null)
		{
			// Get the node
			SceneNode ghostN = avatar.getNode();
			
			// Detaches objects
			sceneManager.getRootSceneNode().detachChild(ghostN);
			sceneManager.destroySceneNode(ghostN);
			sceneManager.destroyEntity("ghostE" + avatar.getID().toString());
			System.out.println("Here");
			
		}
	}
	
	public void addGhostNPCToGameWorld(GhostNPC newNPC, Vector3 pos) throws IOException
	{
		if (newNPC != null)
		{
			Entity ghostE = sceneManager.createEntity("NpcE" + newNPC.getID().toString(), "monster1_textured.obj");
			ghostE.setPrimitive(Primitive.TRIANGLES);
			SceneNode ghostN = sceneManager.getRootSceneNode().createChildSceneNode("NpcN" + newNPC.getID().toString());
			ghostN.attachObject(ghostE);
			ghostN.setLocalPosition(pos);
			ghostN.scale(0.05f, 0.05f, 0.05f);
			newNPC.setNode(ghostN);
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
	
    @Override
    protected void setupScene(Engine eng, SceneManager sm) throws IOException
	{
		findScriptEngine();
	
		//animations
		manSE = sm.createSkeletalEntity("manAv", "robot.rkm", "robot.rks");
		Texture tex = sm.getTextureManager().getAssetByPath("robot.png");
		TextureState tstate = (TextureState) sm.getRenderSystem()
		.createRenderState(RenderState.Type.TEXTURE);
		tstate.setTexture(tex);
		manSE.setRenderState(tstate);
		// attach the entity to a scene node
		avatar1 = sm.getRootSceneNode().createChildSceneNode(manSE.getName()+"Node");
		avatar1.attachObject(manSE);
		//avatar1.moveBackward(2.0f);
		//avatar1.translate(0.0f,2.0f,0.0f);
		avatar1.setLocalPosition(0.0f, 0.0f, 0.0f);
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
        //object1.attachObject(object1E);
		object1.scale(0.05f, 0.05f, 0.05f);
		// Add dolphin 2 to rotation controller
		
		
		//Entity object2E = sm.createEntity("object2", "dolphinHighPoly.obj");
		//object2 = sm.getRootSceneNode().createChildSceneNode(object2E.getName() + "Node");
        //object2E.setPrimitive(Primitive.TRIANGLES);
		//object2.attachObject(object2E);
		//object2.moveUp(.5f);

		setupNetworking();
		setupNPC();
		setupInputs();
		setupOrbitCamera(eng, sm);
		

		initTerrain(eng, sm);
		initSkyBox(eng, sm);
		initLights(sm);
		initUFO(sm);
		
		// Physics
		initPhysicsExamples(sm);
		initPhysicsSystem();
		createRagePhysicsWorld();
		
		// Initial vertical update of player so
		// player doesn't have to move to get above ground
		updateVerticalPosition();
		
		// Initial vertical update of monster
		//updateVerticalPos(object1);
		initAudio(sm);
    }
	public void setupNPC()
	{
		npcController = new NPCcontroller(this, protClient);
	}
	
	private void findScriptEngine()
	{
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
	}
	
	private void initTerrain(Engine eng, SceneManager sm) throws IOException
	{
		tessE = sm.createTessellation("tessE", 6);
		// subdivisions per patch: min=0, try up to 32
		tessE.setSubdivisions(8f);
		tessN = sm.getRootSceneNode().createChildSceneNode("TessN");
		tessN.attachObject(tessE);
		tessN.scale(20, 40, 20);
		tessE.setHeightMap(this.getEngine(), "heightmap1.jpeg");
		tessE.setTexture(this.getEngine(), "hexagons.jpeg");
		//tessE.setMultiplier(5.0f);
	}
	
	private void initUFO(SceneManager sm) throws IOException
	{
		Entity ufoE = sm.createEntity("ufo", "untitled.obj");
		ufo = sm.getRootSceneNode().createChildSceneNode(ufoE.getName() + "Node");
        ufoE.setPrimitive(Primitive.TRIANGLES);
		ufo.attachObject(ufoE);
		ufo.moveUp(1.5f);
		ufo.moveForward(5.0f);
		ufo.scale(0.2f, 0.2f, 0.2f);
		
		Light ufoLight = sm.createLight("ufoLight", Light.Type.POINT);
        ufoLight.setAmbient(new Color(.5f, .5f, .5f));
        ufoLight.setDiffuse(java.awt.Color.RED);
		ufoLight.setSpecular(java.awt.Color.BLUE);
        ufoLight.setRange(4f);
        ufoLight.setConstantAttenuation(.0005f);
		SceneNode ufoLightNode = ufo.createChildSceneNode("ufoLightNode");
        ufoLightNode.attachObject(ufoLight);
		
		Entity ufo2E = sm.createEntity("ufo2", "untitled.obj");
        ufo2 = sm.getRootSceneNode().createChildSceneNode(ufo2E.getName() + "Node");
        ufo2E.setPrimitive(Primitive.TRIANGLES);
        ufo2.attachObject(ufo2E);
        ufo2.moveUp(1.5f);
        ufo2.moveBackward(5.0f);
        ufo2.scale(0.2f, 0.2f, 0.2f);

        Light ufo2Light = sm.createLight("ufo2Light", Light.Type.POINT);
        ufo2Light.setAmbient(new Color(.5f, .5f, .5f));
        ufo2Light.setDiffuse(java.awt.Color.RED);
        ufo2Light.setSpecular(java.awt.Color.BLUE);
        ufo2Light.setRange(4f);
        ufo2Light.setConstantAttenuation(.0005f);
        SceneNode ufo2LightNode = ufo2.createChildSceneNode("ufo2LightNode");
        ufo2LightNode.attachObject(ufo2Light);

        RotationController rc = new RotationController(Vector3f.createUnitVectorY(), .02f);

        rc.addNode(ufo);
        rc.addNode(ufo2);
        sm.addController(rc);
	}
	
	private void initSkyBox(Engine eng, SceneManager sm) throws IOException
	{
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
	}
	
	private void initLights(SceneManager sm) throws IOException
	{
		        //sm.getAmbientLight().setIntensity(new Color(.5f, .5f, .5f));
		Light sunLight = sm.createLight("sunLight", Light.Type.POINT);
		sunLight.setAmbient(new Color(.5f, .5f, .5f));
        sunLight.setDiffuse(new Color(.7f, .7f, .7f));
		sunLight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
        sunLight.setRange(10f);
		sunLight.setConstantAttenuation(.75f);
		sunLight.setDiffuse(java.awt.Color.BLUE);
		
		SceneNode sunLightNode = avatar1.createChildSceneNode("sunLightNode");
        sunLightNode.attachObject(sunLight);
	}
	
	private void initPhysicsExamples(SceneManager sm) throws IOException
	{
		// Physics test objects
		// Ball 1
		Entity ball1Entity = sm.createEntity("ball1", "earth.obj");
		ball1Node = sceneManager.getRootSceneNode().createChildSceneNode("Ball1Node");
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
	
	
	
	public void setEarParameters(SceneManager sm)
	{ SceneNode avatar1 = sm.getSceneNode("manAvNode");
	Vector3 avDir = avatar1.getWorldForwardAxis();
	// note - should get the camera's forward direction
	// - avatar direction plus azimuth	
	audioMgr.getEar().setLocation(avatar1.getWorldPosition());
	audioMgr.getEar().setOrientation(avDir, Vector3f.createFrom(0,1,0));
	}
	
	
	public void initAudio(SceneManager sm)
	{ AudioResource resource1, resource2, resource3;
	audioMgr = AudioManagerFactory.createAudioManager(
	"ray.audio.joal.JOALAudioManager");
	if (!audioMgr.initialize())
	{ System.out.println("Audio Manager failed to initialize!");
	return;
	}
	//throwing Sound
	//http://soundbible.com/1622-Spear-Throw.html
	resource1 = audioMgr.createAudioResource("throwing.wav",
	AudioResourceType.AUDIO_SAMPLE);
	//http://soundbible.com/2213-Alien-Spaceship-UFO.html
	//background Sound
	resource2 = audioMgr.createAudioResource("bgSound.wav",
	AudioResourceType.AUDIO_SAMPLE);
	
	//http://soundbible.com/1084-Slime.html
	//background Sound
	resource3 = audioMgr.createAudioResource("alienSound.wav",
	AudioResourceType.AUDIO_SAMPLE);
	
	
	throwingSound = new Sound(resource1,
	SoundType.SOUND_EFFECT, 100, false);
	
	bgSound = new Sound(resource2,
	SoundType.SOUND_EFFECT, 100, true);
	
	alienSound = new Sound(resource3,
	SoundType.SOUND_EFFECT, 100, true);
	
	throwingSound.initialize(audioMgr);
	throwingSound.setMaxDistance(100.0f);
	throwingSound.setMinDistance(0.5f);
	throwingSound.setRollOff(5.0f);
	
	bgSound.initialize(audioMgr);
	bgSound.setMaxDistance(10.0f);
	bgSound.setMinDistance(0.5f);
	bgSound.setRollOff(5.0f);
	
	alienSound.initialize(audioMgr);
	alienSound.setMaxDistance(10.0f);
	alienSound.setMinDistance(0.5f);
	alienSound.setRollOff(5.0f);
	
	SceneNode monsterN = sm.getSceneNode("object1Node");
	SceneNode musicN = sm.getSceneNode("manAvNode");
	throwingSound.setLocation(musicN.getWorldPosition());
	alienSound.setLocation(monsterN.getWorldPosition());
	bgSound.setLocation(musicN.getWorldPosition());
	setEarParameters(sm);
	
	alienSound.play();
	bgSound.play();
	
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
					
			/*im.associateAction(keyboard,
					net.java.games.input.Component.Identifier.Key.Q,
					quitGameAction,
					InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);*/ // doesnt work
					
			
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

	
	
	
	private boolean checkForSinglePlayerDone = false;
	@Override
    protected void update(Engine engine) {
		elapsTime += engine.getElapsedTimeMillis();
		im.update(elapsTime);
		if (controller && npcController != null)
		{
			npcController.update(elapsTime);
		}
		processNetworking(elapsTime);
		if (checkForSinglePlayerDone == false && isClientConnected == false)
		{
			System.out.println("Unable to join server, reverting to single player mode.");
			controller = true;
			checkForSinglePlayerDone = true;
		}
		orbitController1.updateCameraPosition();
		//orbitController2.updateCameraPosition();
		rs = (GL4RenderSystem) engine.getRenderSystem();
		
		
		SceneManager sm = engine.getSceneManager();

		// Check scripts last modified time and run them if they are changed
		checkScripts();		
		
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
		if (manSE != null)
		{
			//SceneNode avatarN = sm.getSceneNode("manAvNode");
			manSE.update();
		}
		if (bullets.size() > 0)
		{
			// This option deletes the ghost npc directly.
			// Noncontroller should not have
			// the option to delete the ghosts directly
			// so split it up into two checks for noncontrollers.
			if (controller)
			{
				checkCollisionForBullets();
			}
			else
			{
				checkCollisionForBullets_ForNonControllers();
			}
			Vector<Bullet> toDelete = new Vector<Bullet>();
			for (Bullet b : bullets)
			{
				if (b.getAliveTime() > 3000)
				{
					toDelete.add(b);
				}
				else 
				{
					b.addAlive(engine.getElapsedTimeMillis());
			}
			}

			while (toDelete.isEmpty() == false)
			{
				Bullet delete = toDelete.firstElement();
				protClient.timeoutRemoveBullet(delete.getID());
				toDelete.remove(delete);
				deleteBullet(delete);
			}
		}
		setEarParameters(sm);
		
		// End of physics
		
		//HUD infomation
		elapsTime += engine.getElapsedTimeMillis();
		elapsTimeSec = Math.round(elapsTime/1000.0f);
		elapsTimeStr = Integer.toString(elapsTimeSec);
		counterStr = Integer.toString(counter);
		int bulletCount = bulletCount();
		
		dispStr = "Time = " + elapsTimeStr + "   Score = " + counterStr + " Bullet Count:" + bulletCount;
		rs.setHUD(dispStr, 15, 15);
		
		if (counter >= 20 )
		{
			dispStr = "YOU WIN!!!";
			rs.setHUD(dispStr, 15, 15);
		}
	} // End of update()

	void checkCollisionForBullets()
	{
		HashSet<UUID> npcToDelete = new HashSet<UUID>();
		for (Bullet b : bullets)
		{
			SceneNode bulletN = b.getNode();
			Iterator iter = npcController.getIterator();
			while (iter.hasNext())
			{
				GhostNPC npc = (GhostNPC) iter.next();
				boolean collided = checkCollision(bulletN, npc.getNode());
				if (collided)
				{
					System.out.println("COLLISION DETECTED");
					npcToDelete.add(npc.getID());
					counter++;
				}
			}
		}
		
		Iterator iter = npcToDelete.iterator();
		while (iter.hasNext())
		{
			UUID npcID = (UUID) iter.next();
			npcController.deleteNPC(npcID);
		}
		
	}
	
	void checkCollisionForBullets_ForNonControllers()
	{
		HashSet<UUID> npcToRequestDelete = new HashSet<UUID>();
		for (Bullet b : bullets)
		{
			SceneNode bulletN = b.getNode();
			Iterator iter = npcController.getIterator();
			while (iter.hasNext())
			{
				GhostNPC npc = (GhostNPC) iter.next();
				boolean collided = checkCollision(bulletN, npc.getNode());
				if (collided)
				{
					System.out.println("COLLISION DETECTED");
					npcToRequestDelete.add(npc.getID());
					counter++;
				}
			}
		}
		
		Iterator iter = npcToRequestDelete.iterator();
		while (iter.hasNext())
		{
			UUID npcID = (UUID) iter.next();
			protClient.bulletCollisionRequestDelete(npcID);
		}
		
	}

	class Bullet
	{
		public SceneNode bulletN = null;
		public Entity bulletE = null;
		public UUID id = null;
		public float timeAlive;
		
		public Bullet(SceneNode n, Entity e, UUID id)
		{
			bulletN = n;
			bulletE = e;
			this.id = id;
			timeAlive = 0.0f;
		}
		
		public SceneNode getNode()
		{
			return bulletN;
		}
		
		public Entity getEntity()
		{
			return bulletE;
		}
		
		public UUID getID()
		{
			return id;
		}
		
		public void addAlive(float dt)
		{
			timeAlive += dt;
		}
		
		public float getAliveTime()
		{
			return timeAlive;
		}
	}

	public void shoot() throws IOException
	{
		if (bullets.size() == 3)
			return;
		
		manSE.stopAnimation();
		manSE.playAnimation("throwAnimation", 5.0f, STOP, 0);
		
		SceneNode rootNode = sceneManager.getRootSceneNode();
		UUID id = UUID.randomUUID();
		Entity bulletE = sceneManager.createEntity("bulletE" + id.toString(), "sphere.obj");
		SceneNode bulletN = rootNode.createChildSceneNode("bulletN" + id.toString());
		bulletN.attachObject(bulletE);
		bulletN.scale(0.1f, 0.1f, 0.1f);
		Vector3 loc = avatar1.getLocalPosition();
		bulletN.setLocalPosition(loc.x(), loc.y()+0.3f, loc.z());
		float mass = 1.0f;
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
		Bullet newBullet = new Bullet(bulletN, bulletE, id);
		bullets.add(newBullet);
		protClient.sendShootMessage(id, loc, frontVector, currVector);
		throwingSound.play();
	}
	
	public void ghostShoot(UUID player, UUID bullet, Vector3 pos, Vector3 frontVector, Vector3 currVector) throws IOException
	{
		SceneNode rootNode = sceneManager.getRootSceneNode();
		Entity bulletE = sceneManager.createEntity("gBE" + bullet.toString(), "sphere.obj");
		SceneNode bulletN = rootNode.createChildSceneNode("gBN" + bullet.toString());
		bulletN.attachObject(bulletE);
		bulletN.scale(0.1f, 0.1f, 0.1f);
		bulletN.setLocalPosition(pos.x(), pos.y()+0.3f, pos.z());
		float mass = 1.0f;
		double[] temptf;
		temptf = toDoubleArray(bulletN.getLocalTransform().toFloatArray());
		PhysicsObject bulletPhysics = physicsEng.addSphereObject(physicsEng.nextUID(),
			mass, temptf, 0.3f);
		bulletPhysics.setBounciness(0.5f);
		bulletN.setPhysicsObject(bulletPhysics);

		//System.out.println("X: " + frontVector.x() + "Y: " + frontVector.y() + "Z: " + frontVector.z());
		bulletPhysics.applyForce(frontVector.x() * 500.0f, 0.0f, frontVector.z() * 500.0f, currVector.x(), 0.0f, currVector.z());
		//System.out.println("Grenade: " + bulletPhysics.getFriction());
		Bullet newBullet = new Bullet(bulletN, bulletE, bullet);
		Vector<Bullet> ghostVectorBulletList = ghostBulletList.get(player);
		if (ghostVectorBulletList == null)
		{
			ghostVectorBulletList = new Vector<Bullet>();
			ghostVectorBulletList.add(newBullet);
			ghostBulletList.put(player, ghostVectorBulletList);
		}
		else
		{
			ghostVectorBulletList.add(newBullet);
		}
	}
	
	public void ghostBulletDelete(UUID playerID, UUID bulletID)
	{
		Vector<Bullet> ghostPlayerBullets = ghostBulletList.get(playerID);
		Bullet delete = null;
		for (Bullet b : ghostPlayerBullets)
		{
			UUID curBulletID = b.getID();
			if (curBulletID.toString().compareTo(bulletID.toString()) == 0)
			{
				delete = b;
				ghostPlayerBullets.remove(b);
				break;
			}
		}
		
		deleteBullet(delete);
	}
	public void throwGrenade() throws IOException
	{
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
		throwingSound.play();
	}
	
	public void deleteGrenade()
	{
		sceneManager.destroyEntity("grenade1E");
		sceneManager.destroySceneNode("grenade1N");
		grenadeExist = false;
	}
	
	public void deleteBullet(Bullet bullet)
	{
		Bullet b = bullet;
		bullets.remove(bullet);
		SceneNode bulletN = b.getNode();
		Entity bulletE = b.getEntity();
		sceneManager.destroyEntity(bulletE);
		sceneManager.destroySceneNode(bulletN);
	}
	
	public int bulletCount()
	{
		int bulletNumber = bullets.size();
		if (bulletNumber == 0)
		{
			bulletNumber = 3;
		}
		else if (bulletNumber == 1)
		{
			bulletNumber = 2;
		}
		else if (bulletNumber == 2)
		{
			bulletNumber = 1;
		}
		else if (bulletNumber == 3)
		{
			bulletNumber = 0;
		}
		
		return bulletNumber;
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
			worldAvatarPosition.z()) +.35f,
			//Keep the Z coordinate
			localAvatarPosition.z()
		);
		
		// use avatar Local coordinates to set position, including height
		avatar1.setLocalPosition(newAvatarPosition);
	}
	
	public float getVerticalPosition(float x, float z)
	{
		return tessE.getWorldHeight(x, z);
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
	
	private void updatePosFromScript()
	{
		float newX = ((Double)(jsEngine.get("avatarX"))).floatValue();
		float newY = ((Double)(jsEngine.get("avatarY"))).floatValue();
		float newZ = ((Double)(jsEngine.get("avatarZ"))).floatValue();
		if (newX != oldX || newY != oldY || newZ != oldZ)
		{
			Vector3 newPos = Vector3f.createFrom(newX, newY, newZ);
			avatar1.setLocalPosition(newPos);
			protClient.sendMoveMessage(avatar1.getLocalPosition());
			oldX = newX;
			oldY = newY;
			oldZ = newZ;
		}
	}
	
	private void updateCameraSpeedFromScript()
	{
		float newUpdown = ((Double)(jsEngine.get("updown"))).floatValue();
		float newLeftright = ((Double)(jsEngine.get("leftright"))).floatValue();
		if (newUpdown != oldUpdown)
		{
			orbitController1.setLookUpDownSpeed(newUpdown);
			oldUpdown = newUpdown;
		}
		if (newLeftright != oldLeftright)
		{
			orbitController1.setLookLeftRightSpeed(newLeftright);
			oldLeftright = newLeftright;
		}
	}
	
	private void checkScripts()
	{
		long initParamsModTime = initParameters.lastModified();
		if (initParamsModTime > initParametersLastModifiedTime)
		{
			initParametersLastModifiedTime = initParamsModTime;
			executeScript(initParameters);
			updatePosFromScript();

		}
		
		long initCameraSpeedModTime = cameraSpeedS.lastModified();
		if (initCameraSpeedModTime > cameraSpeedLastModifiedtime)
		{
			cameraSpeedLastModifiedtime = initCameraSpeedModTime;
			executeScript(cameraSpeedS);
			updateCameraSpeedFromScript();
		}
	}

	public void initializeNPCcontroller()
	{
		controller = true;
	}
	
	public SceneNode getNPCnode(Vector3 pos, UUID id) throws IOException
	{
		Entity ghostE = sceneManager.createEntity("NpcE" + id.toString(), "monster1_textured.obj");
		ghostE.setPrimitive(Primitive.TRIANGLES);
		SceneNode ghostN = sceneManager.getRootSceneNode().createChildSceneNode("NpcN" + id.toString());
		ghostN.attachObject(ghostE);
		ghostN.setLocalPosition(pos);
		ghostN.scale(0.05f, 0.05f, 0.05f);
		return ghostN;
	}
	
	public Iterator getnpcIterator()
	{
		if (npcController != null)
			return npcController.getIterator();
		System.out.println("Ghost NPC Iterator returned null");
		return null;
	}
	
	public void createNPC(UUID id, Vector3 pos)
	{
		npcController.createNPC(id, pos);
	}
	
	public boolean checkCollision(SceneNode obj1, SceneNode obj2)
	{
		Vector3 pos1 = obj1.getLocalPosition();
		Vector3 pos2 = obj2.getLocalPosition();
		Vector3 distVector = Vector3f.createFrom(pos2.x() - pos1.x(), pos2.y() - pos1.y(), pos2.z() - pos1.z());
		float dist = distVector.length();
		if (dist < 0.75f)
			return true;
		else return false;
	}
	
	public void deleteGhostNPC(UUID id)
	{
		npcController.deleteNPC(id);
	}
	
	public void deleteGhostNPC2(UUID id)
	{
		npcController.deleteNPC_ForNonControllers(id);
	}
	
	public void destroyNPCObjects(SceneNode n)
	{
		sceneManager.destroySceneNode(n);
	}
}

	