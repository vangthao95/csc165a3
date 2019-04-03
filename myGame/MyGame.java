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

public class MyGame extends VariableFrameRateGame {
	// Script files
	File rotationD2RC = new File("scripts/InitParams.js");
	File helloWorldS = new File("scripts/hello.js");
	// End of script files

	// Variables associated with scripts
	ScriptEngineManager factory = new ScriptEngineManager();
	ScriptEngine jsEngine = factory.getEngineByName("js"); // Game engine
	RotationController dolphin2RC; // InitParam.js
	Long rotationD2RCLastModifiedTime; // Modified time for rotationD2RC script
	
	// End of variables associated with scripts
	
	private static final int MAX_PLANETS = 20;
	private static final int TICK_RATE_IN_MS = 150; // Used for hp gain and loss while inside and outside box ship
	private static final int PLANET_VISITS_REQUIRED = 4; // visit 3 planets to win
	private static final int SCORE_PER_PLANET = 100;
	
	SceneNode parentNodeOfPlanets;
	RotationController planetsRC;

	// to minimize variable allocation in update()
	Camera3Pcontroller orbitController1, orbitController2;
	StretchController player1controller;
	CustomController player2controller;
	GL4RenderSystem rs;
	private float elapsTime = 0.0f;
	private float prevElapsTime;
	private String elapsTimeStr, counterStr1, dispStr1, scoreStr1, counterStr2, dispStr2, scoreStr2;
	private int elapsTimeSec, counter, score = 0;
	private Camera camera, camera2;
	private SceneNode dolphinN1, dolphinN2;
	private SceneNode cameraN1, cameraN2;
	// skybox
	private static final String SKYBOX_NAME = "MySkyBox";
	private boolean skyBoxVisible = true;
	// SceneNode array for storing the created planets
	private SceneNode planetNodes[];
	// Boolean array to keep track of status of each planet 
	private boolean visited1[], visited2[];
	// Count on how many planet objects there are
	private int size;
	
	private int hp1, hp2;
	private float tickDownHp1, tickDownHp2;
	private float tickUpHp1, tickUpHp2;
	private float deltaTime;
	
	private boolean inside;
	private Random rand;
	
	private boolean onDolphin;
	private SceneNode boxN;
	String kbName;
	private boolean gameWon1, gameWon2;
	
	private InputManager im;
	
	private int score1, score2, counter1, counter2;
	
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
	

    public MyGame(String serverAddr, int sPort)
	{
        super();
		this.serverAddress = serverAddr;
		this.serverPort = sPort;
		this.serverProtocol = ProtocolType.UDP;
	
		
		System.out.println("press w, a, s, d to move the camera or dolphin");
		System.out.println("press the up, down, left, and right arrow to move the camera");
		System.out.println("press spacebar to get on or off the dolphin");
		System.out.println("");
		System.out.println("Visit the planets for points but watch out!");
		System.out.println("If your hp drops to 0, you will start losing points");
		System.out.println("To recover hp, go inside the ship that looks like a box near Earth");
		System.out.println("You will have to be off your dolphin to recover hp");
		
		onDolphin = true;
		planetNodes = new SceneNode[MAX_PLANETS];
		visited1 = new boolean[MAX_PLANETS];
		visited2 = new boolean[MAX_PLANETS];
		size = 0; 
		rand = new Random();
		hp1 = 100;
		tickDownHp1 = 0.0f;
		tickUpHp1 = 0.0f;
		deltaTime = 0.0f;
		prevElapsTime = 0.0f;
		hp2 = 100;
		tickDownHp2 = 0.0f;
		tickUpHp2 = 0.0f;
		gameWon1 = false;
		gameWon2 = false;
		counter1 = 0;
		counter2 = 0;
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
			Entity ghostE = sceneManager.createEntity("ghost" + uniqueGhosts, "dolphinHighPoly.obj");
			ghostE.setPrimitive(Primitive.TRIANGLES);
			SceneNode ghostN = sceneManager.getRootSceneNode().createChildSceneNode(avatar.getID().toString());
			ghostN.attachObject(ghostE);
			ghostN.setLocalPosition(pos);
			avatar.setNode(ghostN);
		}
	}
	
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
		/*
    	ScriptEngineManager factory = new ScriptEngineManager();
		// get a list of the script engines on this platform
		List<ScriptEngineFactory> list = factory.getEngineFactories();
		System.out.println("Script Engine Factories found:");
		for (ScriptEngineFactory f : list)
		{
			System.out.println(" Name = " + f.getEngineName()
			+ " language = " + f.getLanguageName()
			+ " extensions = " + f.getExtensions());
		}*/
		
		// run hello world script
		executeScript(helloWorldS);
		
		// Run the InitParams.js script to initialize spinSpeed
		executeScript(rotationD2RC);
		rotationD2RCLastModifiedTime = rotationD2RC.lastModified();
		// Initialize the rotation controller with the variable spinSpeed
		dolphin2RC = new RotationController(Vector3f.createUnitVectorY(),
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
    	// All textures must have the same dimensions, so any image’s
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
    	//sm.setActiveSkyBox(sb);
    	
    	
    	
		sceneManager = sm;
		
		Entity dolphinE1 = sm.createEntity("dolphinE1", "dolphinHighPoly.obj");
        dolphinE1.setPrimitive(Primitive.TRIANGLES);
        
		Entity dolphinE2 = sm.createEntity("dolphinE2", "dolphinHighPoly.obj");
        dolphinE1.setPrimitive(Primitive.TRIANGLES);

        dolphinN1 = sm.getRootSceneNode().createChildSceneNode(dolphinE1.getName() + "Node");
        dolphinN1.moveBackward(.5f);
		//dolphinN1.scale(0.3f, 0.3f, 0.3f);
        dolphinN1.attachObject(dolphinE1);
		dolphinN1.moveUp(.25f);
        
        dolphinN2 = sm.getRootSceneNode().createChildSceneNode(dolphinE2.getName() + "Node");
        dolphinN2.moveForward(1.0f);
        dolphinN2.attachObject(dolphinE2);
        dolphinN2.moveUp(.25f);
		// Add dolphin 2 to rotation controller
		dolphin2RC.addNode(dolphinN2);
		sm.addController(dolphin2RC);

        sm.getAmbientLight().setIntensity(new Color(.2f, .2f, .2f));
	
        player1controller = new StretchController();
		player2controller = new CustomController();
		sm.addController(player1controller);
		sm.addController(player2controller);
		setupPlanets(eng, sm);
		setupManualObjects(eng, sm);
		setupNetworking();
		setupInputs();
		setupOrbitCamera(eng, sm);
    }
    
    protected void setupInputs()
    {
    	im =  new GenericInputManager();
    	String gpName = im.getFirstGamepadName();
    	
		Action quitGameAction = new QuitGameAction(this);
		Action incrementCounter = new IncrementCounterAction(this);
		Action CameraLookLeftRightA = new CameraLookLeftRightAction(camera);
		Action cameraLookUpDownA = new CameraLookUpDownAction(camera);
		Action p1MoveForwardA = new MoveForwardAction(dolphinN1, protClient);
		Action p1MoveBackwardA = new MoveBackwardAction(dolphinN1, protClient);
		Action p1MoveLeftA = new MoveLeftAction(dolphinN1, protClient);
		Action p1MoveRightA = new MoveRightAction(dolphinN1, protClient);

		Action p2MoveVerticalA = new ControllerMoveHorizontalAction(dolphinN2);
		Action p2MoveHorizontalA = new ControllerMoveVerticalAction(dolphinN2);
    	
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
    
	// Taken from the example code given by Professor Gordon
	protected ManualObject makePyramid(Engine eng, SceneManager sm)
	throws IOException
	{ 
		ManualObject pyr = sm.createManualObject("Pyramid");
		ManualObjectSection pyrSec =
		pyr.createManualSection("PyramidSection");
		pyr.setGpuShaderProgram(sm.getRenderSystem().
		getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
		float[] vertices = new float[]
		{
			-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, //front
			1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, //right
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, //back
			-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, //left
			-1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, //LF
			1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f //RR
		};
		float[] texcoords = new float[]
		{
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
		};
		float[] normals = new float[]
		{	
			0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
			0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f,
			-1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f
		};
		int[] indices = new int[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17 };
		
		FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
		FloatBuffer texBuf = BufferUtil.directFloatBuffer(texcoords);
		FloatBuffer normBuf = BufferUtil.directFloatBuffer(normals);
		IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
		
		pyrSec.setVertexBuffer(vertBuf);
		pyrSec.setTextureCoordsBuffer(texBuf);
		pyrSec.setNormalsBuffer(normBuf);
		pyrSec.setIndexBuffer(indexBuf);
		
		Texture tex = eng.getTextureManager().getAssetByPath("hexagons.jpeg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texState.setTexture(tex);
		FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().
		createRenderState(RenderState.Type.FRONT_FACE);
		
		pyr.setDataSource(DataSource.INDEX_BUFFER);
		pyr.setRenderState(texState);
		pyr.setRenderState(faceState);
		return pyr;
	}
	
	protected ManualObject makeBox(Engine eng, SceneManager sm)
	throws IOException
	{
		ManualObject boxObject = sm.createManualObject("BOX");
		ManualObjectSection boxObjectSec = boxObject.createManualSection("boxObjectSection");
		boxObject.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
		
		float[] vertices = new float[] 
		{
			//x1     y1    z1    x2     y2    z2    x3    y3     z3
			1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f , -1.0f, 0.0f, 1.0f,  // bottom CW
			 -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, // bottom CW
			 1.0f, 0.0f, 1.0f, -1.0f, 2.0f, 1.0f, -1.0f, 0.0f, 1.0f, // positive z side CCW
			 1.0f, 0.0f, 1.0f, 1.0f, 2.0f, 1.0f, -1.0f, 2.0f, 1.0f,	 // positive z side CCW
			 -1.0f, 2.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 2.0f, -1.0f, // top CCW
			 1.0f, 2.0f, -1.0f, -1.0f, 2.0f, -1.0f, -1.0f, 2.0f, 1.0f, // top CCW
			 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 2.0f, -1.0f, // negiatve z side CW
			 -1.0f, 2.0f, -1.0f, 1.0f, 2.0f, -1.0f, 1.0f, 0.0f, -1.0f, // negative z side CW
			 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 2.0f, 1.0f, // positive x side CCW
			 1.0f, 2.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 2.0f, -1.0f, // positive x side CCW
			 -1.0f, 0.0f, 1.0f, -1.0f, 2.0f, 1.0f, -1.0f, 0.0f, -1.0f, // negative x side CW
			 -1.0f, 0.0f, -1.0f, -1.0f, 2.0f, 1.0f, -1.0f, 2.0f, -1.0f, // negative x side CW
			 
			 // render inside of box
			 -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f , 1.0f, 0.0f, -1.0f,  // bottom
			 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, // bottom
			 -1.0f, 0.0f, 1.0f, -1.0f, 2.0f, 1.0f, 1.0f, 0.0f, 1.0f, // positive z side
			 -1.0f, 2.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 0.0f, 1.0f,	 // positive z side
			 1.0f, 2.0f, -1.0f, 1.0f, 2.0f, 1.0f, -1.0f, 2.0f, 1.0f, // top
			 -1.0f, 2.0f, 1.0f, -1.0f, 2.0f, -1.0f, 1.0f, 2.0f, -1.0f, // top
			 -1.0f, 2.0f, -1.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, // negiatve z side
			 1.0f, 0.0f, -1.0f, 1.0f, 2.0f, -1.0f, -1.0f, 2.0f, -1.0f, // negative z side
			 1.0f, 2.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, // positive x side
			 1.0f, 2.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 2.0f, 1.0f, // positive x side
			 -1.0f, 0.0f, -1.0f, -1.0f, 2.0f, 1.0f, -1.0f, 0.0f, 1.0f, // negative x side
			 -1.0f, 2.0f, -1.0f, -1.0f, 2.0f, 1.0f, -1.0f, 0.0f, -1.0f // negative x side
		};
		
		float[] texcoords = new float[]
		{
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f, 
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f, 
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f
		};
		
		
		float[] normals = new float[]
		{
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
			0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
			0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
			1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
			1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
			-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
			-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f
		};
		
		int[] indices = new int[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,
		19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,
		44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,
		69,70,71
		};
		
		FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
		FloatBuffer texBuf = BufferUtil.directFloatBuffer(texcoords);
		FloatBuffer normBuf = BufferUtil.directFloatBuffer(normals);
		IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
		
		boxObjectSec.setVertexBuffer(vertBuf);
		boxObjectSec.setTextureCoordsBuffer(texBuf);
		boxObjectSec.setNormalsBuffer(normBuf);
		boxObjectSec.setIndexBuffer(indexBuf);
		
		Texture tex = eng.getTextureManager().getAssetByPath("chain-fence.jpeg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		texState.setTexture(tex);
		FrontFaceState faceState = (FrontFaceState) sm.getRenderSystem().createRenderState(RenderState.Type.FRONT_FACE);
		
		boxObject.setDataSource(DataSource.INDEX_BUFFER);
		boxObject.setRenderState(texState);
		boxObject.setRenderState(faceState);
		return boxObject;
	}
	
	protected ManualObject makeGround(Engine eng, SceneManager sm)
	throws IOException
	{ 
		ManualObject line = sm.createManualObject("ground");
		ManualObjectSection lineSec = line.createManualSection("groundSection");
		line.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
		float[] vertices = new float[]
		{
			10.0f, 0.0f, 10.0f, 10.0f, 0.0f, -10.0f, -10.0f, 0.0f, -10.0f,
			-10.0f, 0.0f, -10.0f, -10.0f, 0.0f, 10.0f, 10.0f, 0.0f, 10.0f
			
		};
		int[] indices = new int[] {0,1,2,3,4,5};
		
		FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
		IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
		
		lineSec.setVertexBuffer(vertBuf);
		lineSec.setIndexBuffer(indexBuf);
		
		Texture tex = eng.getTextureManager().getAssetByPath("bright-green.jpeg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		
		texState.setTexture(tex);
		
		lineSec.setRenderState(texState);
		
		return line;
	}
	
	protected ManualObject makeXLine(Engine eng, SceneManager sm)
	throws IOException
	{ 
		ManualObject line = sm.createManualObject("xLine");
		ManualObjectSection lineSec = line.createManualSection("xLineSection");
		line.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
		float[] vertices = new float[]
		{
			-50.0f, 0.0f, 0.0f, 50.0f, 0.0f, 0.0f
		};
		int[] indices = new int[] {0,1};
		
		FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
		IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
		
		lineSec.setVertexBuffer(vertBuf);
		lineSec.setIndexBuffer(indexBuf);
		
		Texture tex = eng.getTextureManager().getAssetByPath("bright-red.jpeg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		
		texState.setTexture(tex);
		
		lineSec.setRenderState(texState);
		
		return line;
	}
	protected ManualObject makeYLine(Engine eng, SceneManager sm)
	throws IOException
	{ 
		ManualObject line = sm.createManualObject("yLine");
		ManualObjectSection lineSec = line.createManualSection("yLineSection");
		line.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
		float[] vertices = new float[]
		{
			0.0f, -50.0f, 0.0f, 00.0f, 50.0f, 0.0f
		};
		int[] indices = new int[] {0,1};
		
		FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
		IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
		
		lineSec.setVertexBuffer(vertBuf);
		lineSec.setIndexBuffer(indexBuf);
		
		Texture tex = eng.getTextureManager().getAssetByPath("bright-green.jpeg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		
		texState.setTexture(tex);
		
		lineSec.setRenderState(texState);
		
		return line;
	}
	protected ManualObject makeZLine(Engine eng, SceneManager sm)
	throws IOException
	{ 
		ManualObject line = sm.createManualObject("zLine");
		ManualObjectSection lineSec = line.createManualSection("zLineSection");
		line.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
		float[] vertices = new float[]
		{
			0.0f, 0.0f, -50.0f, 00.0f, 0.0f, 50.0f
		};
		int[] indices = new int[] {0,1};
		
		FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
		IntBuffer indexBuf = BufferUtil.directIntBuffer(indices);
		
		lineSec.setVertexBuffer(vertBuf);
		lineSec.setIndexBuffer(indexBuf);
		
		Texture tex = eng.getTextureManager().getAssetByPath("bright-blue.jpeg");
		TextureState texState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		
		texState.setTexture(tex);
		
		lineSec.setRenderState(texState);
		
		return line;
	}
	
	@Override
	protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
		rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
	}
	
	// now we add setting up viewports in the window
	protected void setupWindowViewports(RenderWindow rw)
	{
		rw.addKeyListener(this);
		Viewport topViewport = rw.getViewport(0);
		topViewport.setDimensions(.51f, .01f, .99f, .49f); // B,L,W,H
		topViewport.setClearColor(new Color(1.0f, .7f, .7f));
		Viewport botViewport = rw.createViewport(.01f, .01f, .99f, .49f);
		botViewport.setClearColor(new Color(.5f, 1.0f, .5f));
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
    	
    	camera2 = sm.createCamera("MainCamera2",Projection.PERSPECTIVE);
    	rw.getViewport(1).setCamera(camera2);
		camera2.setRt((Vector3f)Vector3f.createFrom(1.0f, 0.0f, 0.0f));
		camera2.setUp((Vector3f)Vector3f.createFrom(0.0f, 1.0f, 0.0f));
		camera2.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, -1.0f));
		camera2.setPo((Vector3f)Vector3f.createFrom(0.0f, 0.0f, 0.0f));
		cameraN2 = rootNode.createChildSceneNode("MaincameraN12");
    	cameraN2.attachObject(camera2);
    	camera.getFrustum().setFarClipDistance(1000.0f);
    	camera2.setMode('n');
    }
	
    protected void setupOrbitCamera(Engine eng, SceneManager sm)
    {
    	String gpName = im.getFirstGamepadName();
		String kbName = im.getKeyboardName();
		orbitController1 = new Camera3Pcontroller(camera, cameraN1, dolphinN1, kbName, im);
    	orbitController2 = new Camera3Pcontroller(camera2, cameraN2, dolphinN2, gpName, im);
    }
    
    
	private void setupManualObjects(Engine eng, SceneManager sm) throws IOException {
		// make box object
		RotationController rc = new RotationController(Vector3f.createUnitVectorY(), .02f);
		ManualObject box = makeBox(eng, sm);
		boxN = sm.getRootSceneNode().createChildSceneNode("boxNode");
		boxN.scale(0.5f, 0.5f, 0.5f);
		boxN.attachObject(box);
		boxN.moveBackward(2.0f);
		boxN.moveRight(1.0f);
		Vector3 boxRotationAxis = Vector3f.createFrom(3.5f, -5.03f, 11.4f);
		RotationController boxRC = new RotationController(boxRotationAxis, 0.02f);
		//boxRC.addNode(boxN);
		sm.addController(boxRC);

		// make pyramid object
		ManualObject pyr = makePyramid(eng, sm);
		SceneNode pyrN = sm.getRootSceneNode().createChildSceneNode("PyrNode");
		pyrN.scale(0.75f, 0.75f, 0.75f);
		pyrN.attachObject(pyr);
		pyrN.moveBackward(5.0f);
		pyrN.moveRight(3.0f);
		pyrN.moveUp(.4f);
		
		// make X axis line
		ManualObject xAxis = makeXLine(eng, sm);
		SceneNode xAxisN = sm.getRootSceneNode().createChildSceneNode("xAxisNode");
		xAxisN.attachObject(xAxis);
		xAxis.setPrimitive(Primitive.LINES);
		
		// make Y axis line
		ManualObject yAxis = makeYLine(eng, sm);
		SceneNode yAxisN = sm.getRootSceneNode().createChildSceneNode("yAxisNode");
		yAxisN.attachObject(yAxis);
		yAxis.setPrimitive(Primitive.LINES);
		
		// make Z axis line
		ManualObject zAxis = makeZLine(eng, sm);
		SceneNode zAxisN = sm.getRootSceneNode().createChildSceneNode("zAxisNode");
		zAxisN.attachObject(zAxis);
		zAxis.setPrimitive(Primitive.LINES);
		
		ManualObject ground = makeGround(eng, sm);
		SceneNode groundN = sm.getRootSceneNode().createChildSceneNode("groundNode");
		groundN.attachObject(ground);
		
	}
	
	private void setupPlanets(Engine eng, SceneManager sm) throws IOException {
		planetsRC = new RotationController(Vector3f.createUnitVectorY(), 0.02f);
		parentNodeOfPlanets = sm.getRootSceneNode().createChildSceneNode("PlanetParentNode");
		


		// **************************************************
		int curIndex; // Index for planetNodes array
		
		// set up earth
		curIndex = getIndex();
	
		Entity earthE = sm.createEntity("myEarth", "earth.obj");
		earthE.setPrimitive(Primitive.TRIANGLES);
		planetNodes[curIndex] = parentNodeOfPlanets.createChildSceneNode(earthE.getName()+"Node");
		planetNodes[curIndex].attachObject(earthE);
		planetNodes[curIndex].setLocalPosition(1.0f, 0.40f, -1.0f); // Can probably randomize
		planetNodes[curIndex].setLocalScale(0.2f, 0.2f, 0.2f); // Same here
		planetsRC.addNode(planetNodes[curIndex]);
		incIndex(); // Increment index for planet nodes array
		
		// set up sun
		curIndex = getIndex();

		Entity sunE = sm.createEntity("mySun", "sphere.obj");
		sunE.setPrimitive(Primitive.TRIANGLES);
		planetNodes[curIndex] = parentNodeOfPlanets.createChildSceneNode(sunE.getName()+"Node");
		planetNodes[curIndex].attachObject(sunE);
		planetNodes[curIndex].setLocalPosition(-1.0f, 0.40f, -3.0f);
		planetNodes[curIndex].setLocalScale(0.2f, 0.2f, 0.2f);
		planetsRC.addNode(planetNodes[curIndex]);
		
		Light sunLight = sm.createLight("sunLight", Light.Type.POINT);
		sunLight.setAmbient(new Color(.5f, .5f, .5f));
        sunLight.setDiffuse(new Color(.7f, .7f, .7f));
		sunLight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
        sunLight.setRange(10f);
		
		SceneNode sunLightNode = planetNodes[curIndex].createChildSceneNode("sunLightNode");
        sunLightNode.attachObject(sunLight);
		
		incIndex(); // Increment index for planet nodes array
		
		// set up venus
		curIndex = getIndex();

		// Texture obtained from NASA & rescaled to fit earth object
		// obj file renamed from earth.obj
		// mtl file renamed and modified from earth.mtl
		// Link to NASA's github with textures & models
		// https://github.com/nasa/NASA-3D-Resources
		// Link to directory where venus texture was found
		// https://github.com/nasa/NASA-3D-Resources/tree/master/Images%20and%20Textures/Venus
		// Media Usage Guidlines
		// https://www.nasa.gov/multimedia/guidelines/index.html
		Entity venusE = sm.createEntity("myVenus", "venus.obj");
		venusE.setPrimitive(Primitive.TRIANGLES);
		planetNodes[curIndex] = parentNodeOfPlanets.createChildSceneNode(venusE.getName()+"Node");
		planetNodes[curIndex].attachObject(venusE);
		planetNodes[curIndex].moveRight(1.0f);
		planetNodes[curIndex].setLocalScale(0.2f, 0.2f, 0.2f);
		planetNodes[curIndex].moveUp(0.40f);
		planetsRC.addNode(planetNodes[curIndex]);
		incIndex();
		
		// Create additional planets that looks like venus
		// using a for-loop and random numbers to determine
		// the position and scale
		float poX, poY, poZ; // 
		float scale;		
		Entity clonesOfVenus[] = new Entity[8];
		Vector3 newPlanetPosition;
		int additionalPlanets = 8;
		for (int i = 0; i < additionalPlanets; i++)
		{
			curIndex = getIndex();
			if (curIndex >= MAX_PLANETS) // If the max number of planet is reached then stop creating more
				break;
			clonesOfVenus[i] = sm.createEntity("venus"+i, "venus.obj");
			// Generate random values for the position values until it is
			// far enough from other planets
			do {
				poX = (rand.nextFloat() * 2.0f - 1.0f) * 20.0f; // X position be tween -10 and 10
				poY = .40f;
				poZ = (rand.nextFloat() * 2.0f - 1.0f) * 20.0f; // Z position between -10 and 10
				scale = 0.15f + (0.30f - 0.15f) * rand.nextFloat(); // scale between 0.15 and 0.30
				newPlanetPosition = Vector3f.createFrom(poX, poY, poZ);
			} while (validPlanetPlacement(newPlanetPosition) == false);			
			planetNodes[curIndex] = parentNodeOfPlanets.createChildSceneNode(clonesOfVenus[i].getName()+"Node"+i);
			planetNodes[curIndex].attachObject(clonesOfVenus[i]);
			planetNodes[curIndex].setLocalPosition(newPlanetPosition);
			planetNodes[curIndex].setLocalScale(scale, scale, scale);
			planetsRC.addNode(planetNodes[curIndex]);
			incIndex();
		}
		sm.addController(planetsRC);
	}
	/*
    @Override
    public void keyPressed(KeyEvent e) {	
		float dt = getTimeDifference();
        switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				if (onDolphin())
				{
					lookDolphinUp();
				}
				else
				{
					lookCameraUp();
					
				}
				break;
			case KeyEvent.VK_DOWN:
				if (onDolphin())
				{
					lookDolphinDown();
				}
				else
				{
					lookCameraDown();
				}
				break;
			case KeyEvent.VK_RIGHT:
				if (onDolphin())
				{
					lookDolphinRight();
				}
				else
				{
					lookCameraRight();
				}
				break;
            case KeyEvent.VK_LEFT:
				if (onDolphin())
				{
					lookDolphinLeft();
				}
				else
				{
					lookCameraLeft();
				}	
                break;
			case KeyEvent.VK_D:
				if (onDolphin())
				{
					moveDolphinRight();
				}
				else
				{
					moveCameraRight();
				}				
				break;
			case KeyEvent.VK_A:
				if (onDolphin())
				{
					moveDolphinLeft();
				}
				else
				{
					moveCameraLeft();
				}
				break;
			case KeyEvent.VK_W:
				if (onDolphin())
				{
					moveDolphinFoward();
				}
				else
				{
					moveCameraForward();
				}
				break;
			case KeyEvent.VK_S:
				if (onDolphin())
				{
					moveDolphinBackward();
				}
				else
				{
					moveCameraBackward();
				}
				break;
			case KeyEvent.VK_SPACE:
				if (onDolphin())
				{
					getOffDolphin();
				}
				else
				{
					getOnDolphin();
				}
				break;
        }
        super.keyPressed(e);
    }*/
	public void incrementCounter()
	{
		counter++;
	}
	public boolean onDolphin()
	{
		return onDolphin;
	}
	public void setOnDolphin(boolean value)
	{
		onDolphin = value;
	}
	protected void getOffDolphin()
	{
		setOnDolphin(false);
		dolphinN1.detachChild(cameraN1);
		Vector3 dolphinPo = dolphinN1.getLocalPosition();
		Vector3 dolphinRt = dolphinN1.getLocalRightAxis();
		Vector3 dolphinUp = dolphinN1.getLocalUpAxis();
		Vector3 dolphinFd = dolphinN1.getLocalForwardAxis();
		Vector3 newCameraFd = Vector3f.createFrom(dolphinFd.x(), dolphinFd.y(), dolphinFd.z());
		newCameraFd = newCameraFd.normalize();
		Vector3 newCameraUp = Vector3f.createFrom(dolphinUp.x(), dolphinUp.y(), dolphinUp.z());
		newCameraUp = newCameraUp.normalize();
		Vector3 newCameraRt = Vector3f.createFrom(dolphinRt.x(), dolphinRt.y(), dolphinRt.z());
		newCameraRt = (newCameraRt.normalize()).negate();
		Vector3 newCameraPo = Vector3f.createFrom(dolphinPo.x() - 0.3f*dolphinFd.x()+.03f, dolphinPo.y() - 0.3f*dolphinFd.y(), dolphinPo.z() - 0.3f*dolphinFd.z()+.08f);
		camera.setPo((Vector3f)Vector3f.createFrom(newCameraPo.x(), newCameraPo.y(), newCameraPo.z()));
		camera.setFd((Vector3f)newCameraFd);
		camera.setUp((Vector3f)newCameraUp);
		camera.setRt((Vector3f)newCameraRt);
		
		camera.setMode('c');
	}
	protected void getOnDolphin()
	{
		setOnDolphin(true);
		dolphinN1.attachChild(cameraN1);
		cameraN1.moveUp(0.3f);
		camera.setMode('n');
	}
	
	
	/***************Movement for Camera & Dolphin****************/
	protected void moveDolphinFoward()
	{
		dolphinN1.moveForward(0.1f);
	}
	protected void moveDolphinBackward()
	{
		dolphinN1.moveBackward(0.1f);
	}
	protected void moveDolphinLeft()
	{
		dolphinN1.moveLeft(-0.1f);
	}
	protected void moveDolphinRight()
	{
		dolphinN1.moveRight(-0.1f);
	}
	protected void moveCameraForward()
	{
		Vector3 f = camera.getFd();
		Vector3 p = camera.getPo();
		Vector3 p1 = (Vector3f) Vector3f.createFrom(0.01f*f.x(), 0.01f*f.y(), 0.01f*f.z());
		Vector3 p2 = (Vector3f) p.add((Vector3)p1);
		if (cameraTooFar(p2))
			return;
		camera.setPo((Vector3f)Vector3f.createFrom(p2.x(), p2.y(), p2.z()));
	}
	protected void moveCameraBackward()
	{
		Vector3 f = camera.getFd();
		Vector3 p = camera.getPo();
		Vector3 p1 = (Vector3f) Vector3f.createFrom(0.01f*f.x(), 0.01f*f.y(), 0.01f*f.z());
		Vector3 p2 = (Vector3f) p.sub((Vector3)p1);
		if (cameraTooFar(p2))
			return;
		camera.setPo((Vector3f)Vector3f.createFrom(p2.x(), p2.y(), p2.z()));
	}
	protected void moveCameraLeft()
	{
		Vector3 r = camera.getRt();
		Vector3 p = camera.getPo();
		Vector3 p1 = (Vector3f) Vector3f.createFrom(0.01f*r.x(), 0.01f*r.y(), 0.01f*r.z());
		Vector3 p2 = (Vector3f) p.sub((Vector3)p1);
		if (cameraTooFar(p2))
			return;
		camera.setPo((Vector3f)Vector3f.createFrom(p2.x(), p2.y(), p2.z()));
	}
	protected void moveCameraRight()
	{
		Vector3 r = camera.getRt();
		Vector3 p = camera.getPo();
		Vector3 p1 = (Vector3f) Vector3f.createFrom(0.01f*r.x(), 0.01f*r.y(), 0.01f*r.z());
		Vector3 p2 = (Vector3f) p.add((Vector3)p1);
		if (cameraTooFar(p2))
			return;
		camera.setPo((Vector3f)Vector3f.createFrom(p2.x(), p2.y(), p2.z()));
	}
	/************************************************************/
	
	/************Pitch and Yaw of Dolphin and Camera*************/
	protected void lookDolphinUp()
	{
		Angle rotAmt = Degreef.createFrom(-1.0f);
		dolphinN1.pitch(rotAmt);
	}
	protected void lookDolphinDown()
	{
		Angle rotAmt = Degreef.createFrom(1.0f);
		dolphinN1.pitch(rotAmt);
	}
	protected void lookDolphinLeft()
	{
		Angle rotAmt = Degreef.createFrom(1.0f);
		dolphinN1.yaw(rotAmt);
	}
	protected void lookDolphinRight()
	{
		Angle rotAmt = Degreef.createFrom(-1.0f);
		dolphinN1.yaw(rotAmt);
	}
	protected void lookCameraUp()
	{
		Angle rotAmt = Degreef.createFrom(1.0f);
		Vector3 f = camera.getFd();
        Vector3 r = camera.getRt();
        Vector3 u = camera.getUp();
		Vector3 fn = (f.rotate(rotAmt, r)).normalize();
		Vector3 un = (u.rotate(rotAmt, r)).normalize();
		camera.setFd((Vector3f)Vector3f.createFrom(fn.x(), fn.y(), fn.z()));
		camera.setUp((Vector3f)Vector3f.createFrom(un.x(), un.y(), un.z()));		
	}
	protected void lookCameraDown()
	{
		Angle rotAmt = Degreef.createFrom(-1.0f);
		Vector3 f = camera.getFd();
        Vector3 r = camera.getRt();
        Vector3 u = camera.getUp();
		Vector3 fn = (f.rotate(rotAmt, r)).normalize();
		Vector3 un = (u.rotate(rotAmt, r)).normalize();
		camera.setFd((Vector3f)Vector3f.createFrom(fn.x(), fn.y(), fn.z()));
		camera.setUp((Vector3f)Vector3f.createFrom(un.x(), un.y(), un.z()));		
	}
	protected void lookCameraLeft()
	{
		Angle rotAmt = Degreef.createFrom(1.0f);
        Vector3 f = camera.getFd();
        Vector3 r = camera.getRt();
        Vector3 u = camera.getUp();
		Vector3 fn = (f.rotate(rotAmt, u)).normalize();
		Vector3 rn = (r.rotate(rotAmt, u)).normalize();
		camera.setRt((Vector3f)Vector3f.createFrom(rn.x(), rn.y(), rn.z()));
		camera.setFd((Vector3f)Vector3f.createFrom(fn.x(), fn.y(), fn.z()));	
	}
	protected void lookCameraRight()
	{
		Angle rotAmt = Degreef.createFrom(-1.0f);
        Vector3 f = camera.getFd();
        Vector3 r = camera.getRt();
        Vector3 u = camera.getUp();
		Vector3 fn = (f.rotate(rotAmt, u)).normalize();	
		Vector3 rn = (r.rotate(rotAmt, u)).normalize();	
		camera.setRt((Vector3f)Vector3f.createFrom(rn.x(), rn.y(), rn.z()));	
		camera.setFd((Vector3f)Vector3f.createFrom(fn.x(), fn.y(), fn.z()));		
	}
	/************************************************************/
	
	protected Vector3 getPlayerPosition2(SceneNode dolphin)
	{
		if (onDolphin())
		{
			return dolphin.getLocalPosition();
		}
		else
		{
			return camera.getPo();
		}
	}
	
	// For Planets
	private boolean isCollided(SceneNode dolphin, int planetIndex)
	{	
		Vector3 planetPo = planetNodes[planetIndex].getLocalPosition();
		Vector3 playerPo = getPlayerPosition2(dolphin);
		Vector3 dist = playerPo.sub(planetPo);

		if (dist.length() <= 0.75f)
		{
			return true;
		}
		return false;
	}
	
	private boolean isVisited1(int planetIndex)
	{
		return visited1[planetIndex];
	}
	private boolean isVisited2(int planetIndex)
	{
		return visited2[planetIndex];
	}
	
	private void setVisited1(int planetIndex)
	{
		visited1[planetIndex] = true;
	}
	private void setVisited2(int planetIndex)
	{
		visited2[planetIndex] = true;
	}
	
	private int getIndex()
	{
		return size;
	}
	
	private void incIndex()
	{
		// current index for planetNodes is limited to MAX_PLANETS objects
		if (size < MAX_PLANETS)
		{
			size++; 
		}
	}
	
	private boolean validPlanetPlacement(Vector3 newPlacement)
	{
		Vector3 dist;
		for (int i = 0; i < getIndex(); i++)
		{
			dist = newPlacement.sub(planetNodes[i].getLocalPosition());
			if (dist.length() < 2.0f)
				return false;
		}
		return true;
	}
	
	private boolean isInside(SceneNode dolphin)
	{
		Vector3 boxPo = boxN.getLocalPosition();
		Vector3 playerPo = getPlayerPosition2(dolphin);
		Vector3 dist = playerPo.sub(boxPo);

		if (dist.length() <= 0.50f)
		{
			return true;
		}
		return false;
	}
	
	private boolean cameraTooFar(Vector3 newPosition)
	{
		Vector3 dolphinPo = dolphinN1.getLocalPosition();
		Vector3 dist = newPosition.sub(dolphinPo);		
		
		if (dist.length() > 1.25f)
			return true;
		return false;
	}
	
	private float getTimeDifference()
	{
		float diff = elapsTime - prevElapsTime;
		prevElapsTime = elapsTime;
		return diff;
	
	}
	public Vector3 getPlayerPosition()
	{
		return dolphinN1.getLocalPosition();
	}
	
	@Override
    protected void update(Engine engine) {
		im.update(elapsTime);
		processNetworking(elapsTime);
		orbitController1.updateCameraPosition();
		orbitController2.updateCameraPosition();
		rs = (GL4RenderSystem) engine.getRenderSystem();
		deltaTime = engine.getElapsedTimeMillis();
		elapsTime += deltaTime;
		elapsTimeSec = Math.round(elapsTime/1000.0f);
		elapsTimeStr = Integer.toString(elapsTimeSec);
		counterStr1 = Integer.toString(counter1);
		scoreStr1 = Integer.toString(score1);
		counterStr2 = Integer.toString(counter2);
		scoreStr2 = Integer.toString(score2);
		if ((gameWon1 == false) && (gameWon2 == false))
			dispStr1 = "Player 1 -- Health = " + hp1 + "   Time = " + elapsTimeStr + "   Planets visited = " + counterStr1 + "   Score = " + scoreStr1;
		else if ((gameWon1 == true) && (gameWon2 == false))
			dispStr1 = "You won!!!!    Final Score = " + scoreStr1;
		else if (gameWon2 == true)
			dispStr1 = "Player 2 has won...";
		if ((gameWon2 == false) && (gameWon1 == false))
			dispStr2 = "Player 2 -- Health = " + hp2 + "   Time = " + elapsTimeStr + "   Planets visited = " + counterStr2 + "   Score = " + scoreStr2;
		else if ((gameWon2 == true) && (gameWon1 == false))
			dispStr2 = "You won!!!!    Final Score = " + scoreStr2;
		else if (gameWon1 == true)
			dispStr2 = "Player 1 has won...";
		rs.setHUD(dispStr2, 15, 15);
		rs.setHUD2(dispStr1, 15, 345);
		// continually check for collision
		if (onDolphin == true)
		{
			for (int i = 0; i < getIndex(); i++)
			{
				// If the planet is not visited yet then
				// continually check collision status
				if (isVisited1(i) == false)
				{
					if (isCollided(dolphinN1, i))
					{
						counter1++;
						
						// Get 500 points for visiting an unvisited planet
						score1+=SCORE_PER_PLANET;
						
						setVisited1(i);
						player1controller.addNode(planetNodes[i]);
						if (counter1 == PLANET_VISITS_REQUIRED)
						{
							planetsRC.addNode(parentNodeOfPlanets);
							gameWon1 = true;
						}
					}
				}
			}
			for (int i = 0; i < getIndex(); i++)
			{
				// If the planet is not visited yet then
				// continually check collision status
				if (isVisited1(i) == false)
				{
					if (isCollided(dolphinN2, i))
					{
						counter2++;
						
						// Get 500 points for visiting an unvisited planet
						score2+=SCORE_PER_PLANET;
						
						setVisited1(i);
						player2controller.addNode(planetNodes[i]);
						if (counter2 == PLANET_VISITS_REQUIRED)
						{
							planetsRC.addNode(parentNodeOfPlanets);
							gameWon2 = true;
						}
					}
				}
			}
		}
		boolean insideStatus1 = isInside(dolphinN1);
		boolean insideStatus2 = isInside(dolphinN2);
		// if 3 seconds has passed, decrease user hp
		if (insideStatus1)
		{
			tickDownHp1 = 0f;
			tickUpHp1 += deltaTime;
			if (tickUpHp1 >= TICK_RATE_IN_MS)
			{
				// Recover hp if it isn't full
				if (hp1 < 100)
					hp1++;
				tickUpHp1 = 0;
			}
		}
		else
		{
			tickUpHp1 = 0f;
			tickDownHp1 += deltaTime;
			if (tickDownHp1 >= TICK_RATE_IN_MS * 4)
			{
				// Can't go below 0 hp
				if (hp1 > 0)
					hp1--;
				else if (gameWon1 == false)
					score1--;
				tickDownHp1 = 0f;
			}
		}
		if (insideStatus2)
		{
			tickDownHp2 = 0f;
			tickUpHp2 += deltaTime;
			if (tickUpHp2 >= TICK_RATE_IN_MS)
			{
				// Recover hp if it isn't full
				if (hp2 < 100)
					hp2++;
				tickUpHp2 = 0;
			}
		}
		else
		{
			tickUpHp2 = 0f;
			tickDownHp2 += deltaTime;
			if (tickDownHp2 >= TICK_RATE_IN_MS * 4)
			{
				// Can't go below 0 hp
				if (hp2 > 0)
					hp2--;
				else if (gameWon2 == false)
					score2--;
				tickDownHp2 = 0f;
			}
		}
		
		// run script again in update() to demonstrate dynamic modification
		long modTime = rotationD2RC.lastModified();
		if (modTime > rotationD2RCLastModifiedTime)
		{
			rotationD2RCLastModifiedTime = modTime;
			executeScript(rotationD2RC);
			dolphin2RC.setSpeed(((Double)(jsEngine.get("spinSpeed"))).floatValue());
			System.out.println("Dolphin 2 rotation speed updated");
		}
	} // End of update()
}
