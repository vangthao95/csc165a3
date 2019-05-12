package myGameEngine;

import ray.rage.scene.*;
import ray.rml.*;
import net.java.games.input.Controller;
import ray.input.*;
import ray.input.action.*;

public class Camera3Pcontroller
{ 
	private Camera camera; //the camera being controlled
	private SceneNode cameraN; //the node the camera is attached to
	private SceneNode target; //the target the camera looks at
	private float cameraAzimuth; //rotation of camera around Y axis
	private float cameraElevation; //elevation of camera above target
	private float radias; //distance between camera and target
	private Vector3 targetPos; //target’s position in the world
	private Vector3 worldUpVec;
	
	public Camera3Pcontroller(Camera cam, SceneNode camN, SceneNode targ, String controllerName, InputManager im)
	{
		camera = cam;
		cameraN = camN;
		target = targ;
		cameraAzimuth = 225.0f; // start from BEHIND and ABOVE the target
		cameraElevation = 20.0f; // elevation is in degrees
		radias = 2.0f;
		worldUpVec = Vector3f.createFrom(0.0f, 1.0f, 0.0f);
		setupInput(im, controllerName);
		updateCameraPosition();
	}
	
	// Updates camera position: computes azimuth, elevation, and distance
	// relative to the target in spherical coordinates, then converts those
	// to world Cartesian coordinates and setting the camera position
	public void updateCameraPosition()
	{
		double theta = Math.toRadians(cameraAzimuth); // rot around target
		double phi = Math.toRadians(cameraElevation); // altitude angle
		double x = radias * Math.cos(phi) * Math.sin(theta);
		double y = radias * Math.sin(phi);
		double z = radias * Math.cos(phi) * Math.cos(theta);
		cameraN.setLocalPosition(Vector3f.createFrom((float)x, (float)y, (float)z).add(target.getWorldPosition()));
		cameraN.lookAt(target, worldUpVec);
	}
 
	private void setupInput(InputManager im, String cn)
	{
		if (cn == null)
			return;
		Action orbitControllerA = new OrbitAroundControllerAction();
		Action elevationControllerA = new OrbitElevationControllerAction();
		Action orbitKeyboardA = new OrbitAroundKeyboardAction();
		Action elevationKeyboardA = new OrbitElevationKeyboardAction();
		Action orbitRadiasMouseA = new OrbitRadiasMouseAction();
		
		String keyboardName = im.getKeyboardName();
		String controller = im.getFirstGamepadName();
		String mouse = im.getMouseName();
		
		
		if (cn == keyboardName)
		{
			for (int i = 0; i < 10; i++)
			{
				Controller keyboard = im.getKeyboardController(i);
				if (keyboard == null)
					continue;
				
			//System.out.println(mouse);
			im.associateAction(keyboard,
			net.java.games.input.Component.Identifier.Key.LEFT,
			orbitKeyboardA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
			im.associateAction(keyboard,
			net.java.games.input.Component.Identifier.Key.RIGHT,
			orbitKeyboardA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
			im.associateAction(keyboard,
			net.java.games.input.Component.Identifier.Key.UP,
			elevationKeyboardA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
			im.associateAction(keyboard,
			net.java.games.input.Component.Identifier.Key.DOWN,
			elevationKeyboardA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
			im.associateAction(mouse,
			net.java.games.input.Component.Identifier.Axis.Z,
			orbitRadiasMouseA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			
			}
			
			return;
		}
		
		im.associateAction(cn, net.java.games.input.Component.Identifier.Axis.RX, orbitControllerA,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateAction(cn, net.java.games.input.Component.Identifier.Axis.RY, elevationControllerA,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		// similar input set up for OrbitRadiasAction, OrbitElevationAction
		//System.out.println(cn);

	}
	
	private class OrbitAroundControllerAction extends AbstractInputAction
	{ // Moves the camera around the target (changes camera azimuth).
		public void performAction(float time, net.java.games.input.Event evt)
		{
			float rotAmount;
			if (evt.getValue() < -0.2)
			{
				rotAmount=-0.2f;
			}
			else
			{
				if (evt.getValue() > 0.2)
				{
					rotAmount=0.2f;
				}
				else
				{
					rotAmount=0.0f;
				}
			}
			cameraAzimuth += rotAmount;
			cameraAzimuth = cameraAzimuth % 360;
			//System.out.println(cameraAzimuth);
			updateCameraPosition();
		}
	}
	
	private class OrbitElevationControllerAction extends AbstractInputAction
	{ // Moves the camera around the target (changes camera azimuth).
		public void performAction(float time, net.java.games.input.Event evt)
		{;
			float rotAmount;
			if (evt.getValue() < -0.2)
			{
				rotAmount=0.2f;
			}
			else
			{
				if (evt.getValue() > 0.2)
				{
					rotAmount=-0.2f;
				}
				else
				{
					rotAmount=0.0f;
				}
			}
			cameraElevation += rotAmount;
			cameraElevation = cameraElevation % 360;
			if (cameraElevation < 5.0f)
				cameraElevation = 5.0f;
			else if (cameraElevation > 70.0f)
				cameraElevation = 70.0f;
			
			//System.out.println(cameraElevation);
			updateCameraPosition();
		}
	}
	
	private class OrbitAroundKeyboardAction extends AbstractInputAction
	{ // Moves the camera around the target (changes camera azimuth).
		public void performAction(float time, net.java.games.input.Event evt)
		{
			String key = (evt.getComponent()).getName();
			float rotAmount;
			if (key == "Left")
			{
				rotAmount=-0.5f;
			}
			else
			{
				if (key == "Right")
				{
					rotAmount=0.5f;
				}
				else
				{
					rotAmount=0.0f;
				}
			}
			cameraAzimuth += rotAmount;
			cameraAzimuth = cameraAzimuth % 360;
			//System.out.println(cameraAzimuth);
			updateCameraPosition();
		}
	}
	
	private class OrbitElevationKeyboardAction extends AbstractInputAction
	{ // Moves the camera around the target (changes camera elevation).
		public void performAction(float time, net.java.games.input.Event evt)
		{
			String key = (evt.getComponent()).getName();
			float rotAmount;
			if (key == "Down")
			{
				rotAmount=-0.5f;
			}
			else
			{
				if (key == "Up")
				{
					rotAmount=0.5f;
				}
				else
				{
					rotAmount=0.0f;
				}
			}
			cameraElevation += rotAmount;
			cameraElevation = cameraElevation % 360;
			if (cameraElevation < 5.0f)
				cameraElevation = 5.0f;
			else if (cameraElevation > 70.0f)
				cameraElevation = 70.0f;
			
			//System.out.println(cameraElevation);
			updateCameraPosition();
		}
	}
	
	private class OrbitRadiasMouseAction extends AbstractInputAction
	{
		public void performAction(float time, net.java.games.input.Event evt)
		{
			float rotAmount;
			float upDown = evt.getValue();
			if (upDown > 0.2f)
			{
				rotAmount=-0.02f;
			}
			else
			{
				if (upDown < -0.2f)
				{
					rotAmount=0.02f;
				}
				else
				{
					rotAmount=0.0f;
				}
			}
			radias += rotAmount;
			if (radias > 2.5f)
				radias = 2.5f;
			else if (radias < 0.5f)
				radias = 0.5f;
			updateCameraPosition();
		}
	}
 // similar for OrbitRadiasAction, OrbitElevationAction
}
