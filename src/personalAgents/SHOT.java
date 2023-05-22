package personalAgents;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import game.Game;
import main.collections.FastArrayList;
import other.context.Context;
import other.move.Move;
import other.AI;
import other.RankUtils;
import java.util.concurrent.ThreadLocalRandom;
import utils.AIUtils;
import utils.data_structures.support.zhang_shasha.Node;
import view.container.aspects.designs.board.graph.PenAndPaperDesign;

import java.util.Queue;

/* 
 * Relaxed version of Sequential Halving Applied on Trees
 * 	- Uniformly distributes a fixed budget accross children 
*/
public class SHOT extends AI {
	protected int player = -1;
	private Game game;

	private final boolean debug = true;
	private final double DISCOUNT_FACTOR = 0.999;
	private final long MINIMUM_SMART_THINKING_TIME = 500l;
	private final long MAXIMUM_SMART_THINKING_TIME = 60000l;
	private final boolean cbt;

	protected long globalTimeLeft; // global smart think time limit
	private long TInit; // time when the agent started the current move selection

	protected long playoutTime; // total time taken to make N playouts
	protected double countPlayouts; // number of playouts taken
	protected long smartThinkTime;
	protected String analysisReport = null;
	final int ITERATIONS;

	public SHOT(final int iterations, boolean cbt) {
		this.friendlyName = "SHOT";
		// super.setMaxSecondsPerMove(MAXIMUM_SMART_THINKING_TIME);
		this.cbt = cbt;
		this.ITERATIONS = iterations;
	}

	@Override
	public void initAI(final Game game, final int playerID) {
		this.player = playerID;
		globalTimeLeft = MAXIMUM_SMART_THINKING_TIME;
		MemoryMonitor.memory();
	}

	@Override
	public Move selectAction(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
		this.game = game;
		TInit = System.currentTimeMillis();
		playoutTime = 0;
		countPlayouts = 0;
		smartThinkTime = MINIMUM_SMART_THINKING_TIME;

		NSHNode root = new NSHNode(context, null);
		if (root.unexpandedMoves.size() == 1) {
			return root.unexpandedMoves.get(0);
		}

		//int budget = this.warmup(game, root, 100) - 100;
		int budget = this.ITERATIONS;

		root.openLayer();
		search(game, root, budget);
		root.closeLayer();

		int unspendBudget = budget - root.N;
		// System.out.printf("\t%d %d %s \n", budget, root.N,
		// String.valueOf(unspendBudget));
		if (unspendBudget > 2 && root.children.size() > 1) {
			root.openLayer();
			NSHNode ch1 = root.children.get(0);
			NSHNode ch2 = root.children.get(1);
			search(game, ch1, unspendBudget / 2);
			search(game, ch2, unspendBudget / 2);
			root.updateLayerValues(ch1);
			root.updateLayerValues(ch2);
			ch1.closeLayer();
			ch2.closeLayer();
			root.closeLayer();
			//unspendBudget = budget - root.N;
			// System.out.printf("%d (%d s)\n", unspendBudget,
			// System.currentTimeMillis()-TInit);
			root.sort(2);
		}

		if (root.children.size() == 0)
			return root.unexpandedMoves.get(0);
		if (budget == 0)
			return root.children.get(0).move;

		if (debug) {
			// printEvaluation(root, budget, TInit, System.currentTimeMillis(),
			// root.children.size());
			analysisReport = String.format("%s: %d it (selected it %d, value %.4f after %.4f seconds)",
					friendlyName, root.N, root.children.get(0).N,
					root.children.get(0).Q[this.player] / root.children.get(0).N,
					(System.currentTimeMillis() - TInit) / (1024.0));
		}

		globalTimeLeft -= System.currentTimeMillis() - TInit;
		return root.children.get(0).move;
	}

	private void search(Game game, NSHNode node, int budget) {
		if (checkTimeout()) {
			return;
		}

		if (node.contextNode.trial().over()) {
			double[] ut = RankUtils.utilities(node.contextNode);
			for (int i = 0; i < ut.length; i++) {
				ut[i] *= budget;
			}
			node.updateLayerValues(ut, budget);
			return;
		}

		if (budget <= 1) {
			node.updateLayerValues(makePlayout(game, new Context(node.contextNode)));
			return;
		}

		/* UNVISITED CHILDREN CASE */
		if (node.unexpandedMoves.size() > 0) {
			budget = expand(game, node, budget);
			node.sort(node.children.size());
			if (budget <= 0) {
				return;
			}
		}

		/* SEQUENTIAL HALVING CASE */
		double logN = Math.max(1, Math.ceil((Math.log(node.children.size())) / Math.log(2)));
		int virtualChildrenLenght = node.children.size();
		int layerBudget = (int) Math.floor(budget / logN);
		do {

			int iteratedChildren = virtualChildrenLenght > layerBudget ? layerBudget : virtualChildrenLenght;
			int childBudget = Math.max(1, layerBudget / virtualChildrenLenght);
			for (int i = 0; i < iteratedChildren; i++) {
				NSHNode child = node.children.get(i);
				child.openLayer();
				search(game, child, childBudget); // layerBudget
				node.updateLayerValues(child);
				child.closeLayer();
			}
			// Thread.sleep(1000);
			node.sort(virtualChildrenLenght);
			virtualChildrenLenght = (int) Math.ceil(virtualChildrenLenght / 2f);
		} while (virtualChildrenLenght > 1);
	}

	private int expand(Game game, NSHNode node, int budget) {
		int budgetLeft = budget;
		while (node.unexpandedMoves.size() > 0) {
			if (budgetLeft == 0)
				break;

			final Move move = node.unexpandedMoves.remove(
					ThreadLocalRandom.current().nextInt(node.unexpandedMoves.size()));
			final Context context = new Context(node.contextNode);
			context.game().apply(context, move);
			NSHNode childNode = new NSHNode(context, move);

			node.children.add(childNode);

			childNode.openLayer();
			childNode.updateLayerValues(makePlayout(game, context));
			node.updateLayerValues(childNode);
			childNode.closeLayer();

			budgetLeft--;

		}
		return budgetLeft;
	}

	protected double[] makePlayout(Game game, Context context) {
		Context contextEnd = context;
		Long start = System.nanoTime();

		int preTurn = contextEnd.trial().numMoves();
		if (!contextEnd.trial().over()) {
			contextEnd = new Context(contextEnd);
			game.playout(
					contextEnd,
					null,
					0.5,
					null,
					0,
					500,
					ThreadLocalRandom.current());
		}

		Long finish = System.nanoTime();

		playoutTime += finish - start;
		countPlayouts++;

		int finalTurn = contextEnd.trial().numMoves();
		final double[] utilities = RankUtils.utilities(contextEnd);
		for (int i = 0; i < utilities.length; i++) {
			utilities[i] = utilities[i] * Math.pow(DISCOUNT_FACTOR, finalTurn - preTurn);
		}
		return utilities;
	}

	public int budget() {
		return (int) Math.floor((smartThinkTime * 1000 * 1000) / (playoutTime / countPlayouts));
	}

	private int warmup(Game game, NSHNode root, int maxNodes) {
		Queue<NSHNode> lstNodes = new LinkedList<NSHNode>();
		int countNodes = 0;
		lstNodes.add(root);

		// breadth-first-search
		while (countNodes < maxNodes && !lstNodes.isEmpty()/* && !checkTimeout() */) {
			NSHNode currN = lstNodes.poll();

			while (currN.unexpandedMoves.size() > 0 && countNodes < maxNodes && !checkTimeout()) {
				final Move move = currN.unexpandedMoves.remove(
						ThreadLocalRandom.current().nextInt(currN.unexpandedMoves.size()));
				final Context context = new Context(currN.contextNode);
				context.game().apply(context, move);
				NSHNode childN = new NSHNode(context, move);

				currN.children.add(childN);

				// * HERE: do NOT update parent and do NOT close child layer
				childN.openLayer();
				childN.updateLayerValues(makePlayout(game, context));

				lstNodes.add(childN);
				countNodes++;
			}

		}

		// :: BACKPROPAGATE VALUES
		backpropWarmup(root);
		return budget();
	}

	private void backpropWarmup(NSHNode n) {
		if (n.children.size() == 0) {
			return;
		}
		for (NSHNode ch : n.children) {
			backpropWarmup(ch);
			n.updateLayerValues(ch);
			ch.closeLayer();
		}
	}

	@Override
	public String generateAnalysisReport() {
		return analysisReport;
	}

	public Boolean checkTimeout() {
		return (System.currentTimeMillis() - TInit) >= (smartThinkTime * 1.2);
	}

	private void printEvaluation(NSHNode n, int budget, double start, double finish, int sortSize) {
		System.out.println(String.format("************* %s STATS", this.friendlyName));
		System.out.println(String.format("ROOT  %d nodes | sorted children %d | budget %d", n.N, sortSize, budget));

		for (NSHNode ch : n.children) {
			System.out.println(String.format("\t%s | (%.0f/%d) | rw %.4f", ch.move, ch.Q[this.player], ch.N,
					ch.Q[this.player] / ch.N));
			// System.out.println(String.format("\t%s | b %d | p %d | rw %.4f | Erw %.4f",
			// ch.move, ch.usedBudget, ch.playouts, ch.reward,
			// (ch.reward/(float)ch.playouts)));
		}
		System.out.println(String.format("ELAPSED TIME: %.0fms\n", (finish - start)));

	}

	public class NSHNode {
		public Context contextNode;
		public Move move;
		public FastArrayList<Move> unexpandedMoves;

		public final double[] Q;
		public final double[] Lq; // temporary Q
		public int N;
		public int Ln; // temporary N

		public ArrayList<NSHNode> children;

		public NSHNode(Context contextNode, Move move) {
			this.contextNode = contextNode;
			this.move = move;
			this.unexpandedMoves = new FastArrayList<Move>(game.moves(contextNode).moves());
			Q = new double[game.players().count() + 1];
			Lq = new double[game.players().count() + 1];
			N = 0;
			children = new ArrayList<>();
		}

		public void openLayer() {
			this.Ln = 0;
			for (int i = 0; i < Lq.length; i++) {
				this.Lq[i] = 0;
			}

		}

		public void updateLayerValues(double[] Lq) {
			for (int i = 0; i < Lq.length; i++) {
				this.Lq[i] += Lq[i] * 0.999;
			}
			this.Ln += 1;
		}

		public void updateLayerValues(double[] Lq, int Ln) {
			this.Ln += Ln;
			for (int i = 0; i < Lq.length; i++) {
				this.Lq[i] += Lq[i] * 0.999;
			}
		}

		public void updateLayerValues(NSHNode child) {
			this.Ln += child.Ln;
			for (int i = 0; i < Lq.length; i++) {
				this.Lq[i] += child.Lq[i] * 0.999;
			}
		}

		public void closeLayer() {
			this.N += Ln;
			for (int i = 0; i < Lq.length; i++) {
				this.Q[i] += Lq[i];
				this.Lq[i] = 0;
			}
			this.Ln = 0;
		}

		@Override
		public String toString() {
			return String.format("%s (%d/%d)", move.toString(), Q[1], N);
		}

		private void sort(int interval) {
			List<NSHNode> childrenCopy = children.subList(0, interval);
			final int mover = contextNode.state().mover();
			Collections.sort(childrenCopy, new Comparator<NSHNode>() {
				@Override
				public int compare(NSHNode o1, NSHNode o2) {
					double exploit1 = (o1.Q[mover] + o1.Lq[mover]) / (o1.N + o1.Ln);
					double exploit2 = (o2.Q[mover] + o2.Lq[mover]) / (o2.N + o2.Ln);

					return Double.compare(-exploit1, -exploit2);
				}
			});
			for (int i = 0; i < childrenCopy.size(); i++) {
				children.set(i, childrenCopy.get(i));
			}
		}

	}
}