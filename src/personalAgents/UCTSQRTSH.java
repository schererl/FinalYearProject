package personalAgents;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.Game;
import main.collections.FastArrayList;
import other.AI;
import other.RankUtils;
import other.context.Context;
import other.move.Move;
import other.trial.Trial;
public class UCTSQRTSH extends AI
{
	private final long THINKING_TIME = 500l;
	private final long GLOBAL_TIME = 60000l;
	private final int MAX_TERATIONS = 10000;
	private final boolean debug = false;
	
	private final boolean TIME_BASED;
	protected int player = -1;
	private final boolean USE_CBT;
	protected long G;
	
	public UCTSQRTSH(final boolean useTime, final boolean clockBonusTime)
	{
		if(clockBonusTime){
			this.friendlyName = "SQRTSHcbt";
		}else{
			this.friendlyName = "SQRTSH";
		}
		
		this.USE_CBT = clockBonusTime;
		this.TIME_BASED = useTime;
		G = GLOBAL_TIME;
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
		final long START_TIME = System.currentTimeMillis();
		final Node root = new Node(null, null, context);
		if(root.unexpandedMoves.size()==1){
			return root.unexpandedMoves.get(0);
		}
		
		long stopTime = THINKING_TIME + START_TIME;
		final int maxIts = MAX_TERATIONS;
		
		long R; //Resource
		long r; // used Resource
		if(TIME_BASED){
			R = THINKING_TIME;
			r = System.currentTimeMillis()-START_TIME ;
		}else{
			R = maxIts;
			r = 0;
		}

		// SQRTSH Variables
		int halve = 1;
		double logN = Math.max(1, Math.ceil(Math.log(Math.max(1, root.unexpandedMoves.size()) / Math.log(2))));
		
		// CBT Variables
		boolean tmpUseCBT = USE_CBT;
		int countMoves = 0;
		int validMoves = 0;
		int movesAlreadyDone = context.trial().generateCompleteMovesList().size();
		if(debug)
			System.out.printf("START %d choices\n", root.virtualCHLen);
		while 
		(
			r < R &&
			!wantsInterrupt							
		)
		{
			//:: CLOCK BONUS TIME
			if (tmpUseCBT && r >= R / 2) {
				double avgCountMoves = validMoves > 0 && validMoves < countMoves ? countMoves / Math.max(1, validMoves)
						: Integer.MAX_VALUE;
				tmpUseCBT = false;
				long bonus = (long) Math.max(r, Math.min(2000, Math.floor(G / Math.max(1, avgCountMoves)))) - r;
				R += bonus;
				stopTime = START_TIME + R;
				
				if(debug)
					System.out.printf("(%d) bonus granted %d remain moves %.0f (%d/%d)\n", G, bonus, avgCountMoves, validMoves, root.visitCount);

				// if((float)(validMoves/root.visitCount) < 0.3){
				// 	if(debug)
				// 		System.out.println("EARLY STOPPING\n");
				// 	break;
				// }
			}

			//:: HALVE
			if(root.virtualCHLen>4 && r > R*((halve)/logN) && root.unexpandedMoves.size()==0){
				if(debug){
					System.out.printf("(%d) halve %d after %d simul at %ds\n", root.virtualCHLen, halve, root.visitCount, r);
					printEvaluation(root);
				}
					halve++;
				root.sort(root.virtualCHLen);
				root.virtualCHLen = Math.max(4, (int)Math.ceil(root.virtualCHLen/2));
			}

			
			//:: SEARCH
			Node current = SRPolicy(root);
			while (current.visitCount > 0 && !current.context.trial().over())
			{
				current = select(current);
			}
			
			//:: SIMULATE
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
					600, 
					ThreadLocalRandom.current()
				);
			}
			int finalTurn = contextEnd.trial().numMoves(); // compute number of moves the agent made
			if (tmpUseCBT && contextEnd.trial().over()) {
				countMoves += countMovesPlayer(contextEnd.trial(), movesAlreadyDone);
				validMoves++;
			}

			//:: BACKPROPAGATION
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

			// increase used resource
			if(TIME_BASED) r = System.currentTimeMillis() - START_TIME;
			else r++;
		}

		G -= System.currentTimeMillis() - START_TIME; 
		if(debug){
			System.out.printf("FINISH after %d simul at %ds\n\n", root.visitCount, r);
			printEvaluation(root);
		}
        return finalMoveSelection(root);
	}
	
	public static Node SRPolicy(final Node current)
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
        final double twoParentLog = 2.0 * Math.sqrt(current.visitCount);
        int numBestFound = 0;
        
        final int numChildren = current.virtualCHLen; 
        final int mover = current.context.state().mover();

        for (int i = 0; i < numChildren; ++i) 
        {
        	final Node child = current.children.get(i);
        	final double exploit = child.scoreSums[mover] / child.visitCount;
        	final double explore = Math.sqrt(twoParentLog / child.visitCount);
        
            final double ucb1Value = exploit + 0.7*explore;
            
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
        
            final double ucb1Value = exploit + 0.7*explore;
            
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

	public int countMovesPlayer(Trial trial, int movesAlreadyDone) {
		final List<Move> movesIterated = trial.generateCompleteMovesList();
		int count = 0;
		int numberMoves = movesIterated.size();
		for (Move m : movesIterated) {
			if (numberMoves <= movesAlreadyDone)
				break;

			final int idPlayer = m.mover();
			if (idPlayer == this.player)
				count++;
			numberMoves--;
		}

		return count;
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
		private List<Node> children = new ArrayList<Node>();
		private final FastArrayList<Move> unexpandedMoves;
		private int virtualCHLen;
		public Node(final Node parent, final Move moveFromParent, final Context context)
		{
			this.parent = parent;
			this.moveFromParent = moveFromParent;
			this.context = context;
			final Game game = context.game();
			scoreSums = new double[game.players().count() + 1];
			unexpandedMoves = new FastArrayList<Move>(game.moves(context).moves());
			virtualCHLen = unexpandedMoves.size();
			if (parent != null)
				parent.children.add(this);
		}

		public void sort(final int interval) {
			List<Node> childrenCopy = children.subList(0, interval);
			final int mover = context.state().mover();
			Collections.sort(childrenCopy, new Comparator<Node>() {
				@Override
				public int compare(Node o1, Node o2) {
					double f1 = o1.scoreSums[mover] / o1.visitCount;
					double f2 = o2.scoreSums[mover] / o2.visitCount;
					return Double.compare(-f1, -f2);
				}
			});
			for (int i = 0; i < childrenCopy.size(); i++) {
				children.set(i, childrenCopy.get(i));
			}
		}
	}
}

