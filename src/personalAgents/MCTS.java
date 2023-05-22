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
public class MCTS extends AI
{
	
	protected int player = -1;
	protected int iterations;
	public MCTS(int it)
	{
		this.friendlyName = "MCTS";
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
		final Node root = new Node(null, null, context);
		final long stopTime = (maxSeconds > 0.0) ? System.currentTimeMillis() + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;
		final int maxIts = iterations;//(maxIterations >= 0) ? maxIterations : Integer.MAX_VALUE;
		int numIterations = 0;
		while 
		(
			numIterations < maxIts && 					
			//System.currentTimeMillis() < stopTime && 
			!wantsInterrupt							
		)
		{
			Node current = root;
			while (true)
			{
				if (current.context.trial().over()) break;
				current = select(current);
				if (current.visitCount == 0) break;
			}
			
			Context contextEnd = current.context;
			int preTurn = contextEnd.trial().numMoves();
			if (!contextEnd.trial().over())
			{
				contextEnd = new Context(contextEnd);
				game.playout
				(
					contextEnd, 
					null, 
					-1.0, 
					null, 
					0, 
					-1, 
					ThreadLocalRandom.current()
				);
			}
			int finalTurn = contextEnd.trial().numMoves();
			final double[] utilities = RankUtils.utilities(contextEnd);
			for(int i = 0; i < utilities.length; i++){
				utilities[i] = utilities[i] * Math.pow(0.999, finalTurn - preTurn);
			}

			double discount = 1;
			while (current != null)
			{
				current.visitCount += 1;
				for (int p = 1; p <= game.players().count(); ++p)
				{
					current.scoreSums[p] += utilities[p] * discount;
				}
				current = current.parent;
				discount*= 0.999;
			}
			++numIterations;
		}
		//System.out.println(numIterations);
        //printEvaluation(root);
		return finalMoveSelection(root);
	}
	
	public static Node select(final Node current)
	{
		if (!current.unexpandedMoves.isEmpty())
		{
			final Move move = current.unexpandedMoves.remove(
					ThreadLocalRandom.current().nextInt(current.unexpandedMoves.size()));
			final Context context = new Context(current.context);
			context.game().apply(context, move);
			return new Node(current, move, context);
		}
		
		Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        final double twoParentLog = 2.0 * Math.log(Math.max(1, current.visitCount));
        int numBestFound = 0;
        
        final int numChildren = current.children.size();
        final int mover = current.context.state().mover();

        for (int i = 0; i < numChildren; ++i) 
        {
        	final Node child = current.children.get(i);
        	final double exploit = child.scoreSums[mover] / child.visitCount;
        	final double explore = Math.sqrt(twoParentLog / child.visitCount);
        
            final double ucb1Value = exploit + explore;
            
            if (ucb1Value > bestValue)
            {
                bestValue = ucb1Value;
                bestChild = child;
                numBestFound = 1;
            }
            else if 
            (
            	ucb1Value == bestValue && 
            	ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            )
            {
            	bestChild = child;
            }
        }
        return bestChild;
	}

	private void printEvaluation(Node n){
		System.out.println(String.format("ROOT  %d nodes", n.visitCount));
		for(Node ch : n.children){
            System.out.println(String.format("\t%s | (%.0f/%d) %.4f", ch.moveFromParent, ch.scoreSums[this.player], ch.visitCount,ch.scoreSums[this.player]/ch.visitCount));
        }
        System.out.println("\n");
	}
	
	public static Move finalMoveSelection(final Node rootNode)
	{
		Node bestChild = null;
        int bestVisitCount = Integer.MIN_VALUE;
        int numBestFound = 0;
        
        final int numChildren = rootNode.children.size();

        for (int i = 0; i < numChildren; ++i) 
        {
        	final Node child = rootNode.children.get(i);
        	final int visitCount = child.visitCount;
            
            if (visitCount > bestVisitCount)
            {
                bestVisitCount = visitCount;
                bestChild = child;
                numBestFound = 1;
            }
            else if 
            (
            	visitCount == bestVisitCount && 
            	ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            )
            {
            	bestChild = child;
            }
        }
        
        return bestChild.moveFromParent;
	}
	
	@Override
	public void initAI(final Game game, final int playerID)
	{
		this.player = playerID;
		MemoryMonitor.memory();
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
	
	public static class Node
	{
		private final Node parent;
		private final Move moveFromParent;
		private final Context context;
		private int visitCount = 0;
		private final double[] scoreSums;
		private final List<Node> children = new ArrayList<Node>();
		private final FastArrayList<Move> unexpandedMoves;
		public Node(final Node parent, final Move moveFromParent, final Context context)
		{
			this.parent = parent;
			this.moveFromParent = moveFromParent;
			this.context = context;
			final Game game = context.game();
			scoreSums = new double[game.players().count() + 1];
			unexpandedMoves = new FastArrayList<Move>(game.moves(context).moves());
			
			if (parent != null)
				parent.children.add(this);
		}
		
	}
	
	//-------------------------------------------------------------------------

}

