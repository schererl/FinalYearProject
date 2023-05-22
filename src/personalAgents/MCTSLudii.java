package personalAgents;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.Game;
import main.collections.FastArrayList;
import other.AI;
import other.RankUtils;
import other.context.Context;
import other.move.Move;

import search.mcts.MCTS;

public class MCTSLudii extends AI
{
	
	protected int player = -1;
	protected int iterations;
	private MCTS UCT_Ludii;
	public MCTSLudii(int it)
	{
		this.friendlyName = "Ludii UCT";
		this.iterations = it;
	}
	
	@Override
	public Move selectAction
	(
		final Game game,
		final Context context, 
		final double maxSeconds, 
		final int maxIterations, 
		final int maxDepth
	)
	{
		final long startTime = System.currentTimeMillis();
		Move selectedMove=UCT_Ludii.selectAction(game, context, -1, iterations, maxDepth);
		System.out.println((System.currentTimeMillis()-startTime)/1000f);
		return selectedMove;
	}
	
	@Override
	public void initAI(final Game game, final int playerID)
	{
		this.player = playerID;
		UCT_Ludii = MCTS.createUCT();
		UCT_Ludii.initAI(game, playerID);
	}
	
	@Override
	public boolean supportsGame(final Game game)
	{
		//if (game.isStochasticGame())
		//	return false;
		
		if (!game.isAlternatingMoveGame())
			return false;
		
		return true;
	}
	
	
	
	//-------------------------------------------------------------------------

}

