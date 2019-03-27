package myGameEngine;

import ray.input.action.AbstractInputAction;
import net.java.games.input.Event;
import myGame.*;

public class IncrementCounterAction extends AbstractInputAction
{
	private MyGame game;
	public IncrementCounterAction(MyGame g)
	{
		game = g;
	}

	public void performAction(float time, Event e)
	{
		System.out.println("counter action initiated");
		game.incrementCounter();
	}
}