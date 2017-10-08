package net.lecousin.framework.io.defrag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.lecousin.framework.collections.LinkedArrayList;
import net.lecousin.framework.collections.sort.RedBlackTreeLong;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.math.FragmentedRangeLong;
import net.lecousin.framework.math.RangeLong;
import net.lecousin.framework.util.Pair;

/**
 * This defragmenter fills free spaces, trying to do minimal amount of moves.
 * It does not support data with several fragments, each fragment is considered as an individual data,
 * in other words it does not defragment fragmented data, but only defragment free spaces so that
 * a single free space remains at the end of the file.
 *
 * @param <TError> type of error that can occur
 */
public class DefragmenterMinimumMoves<TError extends Exception> {

	/**
	 * Base class to represent a used block of data, with a RangeLong indicating its position.
	 * Implementations of the defragmenter may extend this class to attach additional data to such a block.
	 */
	public static class UsedBlock {
		public RangeLong block;
	}
	
	/** Interface that is used to move a used block of data to a new offset.
	 * @param <TError> type of error
	 */
	public static interface DataMover<TError extends Exception> {
		/** Move the given used block of data to the given new offset. */
		ISynchronizationPoint<TError> move(UsedBlock block, long offset);
	}
	
	/** Constructor.
	 * @param usedBlocks list of used blocks of data
	 * @param freeSpaces list of free spaces
	 * @param mover implementation of DataMover
	 */
	public DefragmenterMinimumMoves(List<? extends UsedBlock> usedBlocks, FragmentedRangeLong freeSpaces, DataMover<TError> mover) {
		this.usedBlocks = usedBlocks;
		this.freeSpaces = freeSpaces;
		this.mover = mover;
	}
	
	protected List<? extends UsedBlock> usedBlocks;
	protected FragmentedRangeLong freeSpaces;
	protected DataMover<TError> mover;
	
	public FragmentedRangeLong getFreeSpaces() { return freeSpaces; }
	
	/** Launch the defragmentation. */
	public ISynchronizationPoint<TError> defragment() {
		JoinPoint<TError> jp = new JoinPoint<>();
		defragment(jp);
		return jp;
	}
	
	@SuppressFBWarnings(value = {"NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"},
			justification = "block.block is supposed to be set by implementation")
	private void defragment(JoinPoint<TError> jp) {
		do {
			if (freeSpaces.isEmpty() || usedBlocks.isEmpty()) {
				// end of defragmentation
				jp.start();
				return;
			}
			RangeLong first = freeSpaces.removeFirst();
			long size = first.max - first.min + 1;
			// search for a perfect match, or a combination that perfectly fill, or at least a combination that fill as much as possible
			List<Pair<List<UsedBlock>,Long>> combinations = new LinkedArrayList<>(20);
			for (Iterator<? extends UsedBlock> it = usedBlocks.iterator(); it.hasNext(); ) {
				UsedBlock block = it.next();
				if (block.block.min < first.min) {
					// the block is before the first free space, it can stay there
					it.remove();
					continue;
				}
				long blockSize = block.block.max - block.block.min + 1;
				if (blockSize == size) {
					// we have a perfect match
					combinations = null;
					// it becomes a free space
					freeSpaces.addRange(block.block);
					// move it
					jp.addToJoin(mover.move(block, first.min));
					// no more move for this one
					it.remove();
					break;
				}
				if (blockSize > size)
					continue; // bigger than the free space, impossible to move inside
				
				ArrayList<Pair<List<UsedBlock>,Long>> list = new ArrayList<>(combinations);
				for (Pair<List<UsedBlock>,Long> combination : list) {
					long s = combination.getValue2().longValue();
					if (s + blockSize > size)
						continue; // exceed
					if (s + blockSize == size) {
						// perfect match
						combinations = null;
						long offset = 0;
						for (UsedBlock b : combination.getValue1()) {
							freeSpaces.addRange(b.block);
							jp.addToJoin(mover.move(b, first.min + offset));
							offset += b.block.max - b.block.min + 1;
							usedBlocks.remove(b); // no more move for this one
						}
						freeSpaces.addRange(block.block);
						jp.addToJoin(mover.move(block, first.min + offset));
						break;
					}
					// not exceeding, we can add this new combination
					ArrayList<UsedBlock> newComb = new ArrayList<>(combination.getValue1().size() + 1);
					newComb.addAll(combination.getValue1());
					newComb.add(block);
					combinations.add(new Pair<>(newComb, Long.valueOf(s + blockSize)));
				}
				if (combinations == null)
					break; // perfect match found
				// add a new combination with just this new block
				combinations.add(new Pair<>(Collections.singletonList(block), Long.valueOf(blockSize)));
			}
			if (combinations == null)
				continue; // perfect match found
			if (usedBlocks.isEmpty()) {
				freeSpaces.addRange(first); // put back the free space
				continue; // no more data
			}
			// get the bigger possible combination
			if (!combinations.isEmpty()) {
				List<UsedBlock> comb = null;
				long combSize = 0;
				for (Pair<List<UsedBlock>,Long> combination : combinations) {
					if (comb == null) {
						comb = combination.getValue1();
						combSize = combination.getValue2().longValue();
						continue;
					}
					if (combination.getValue2().longValue() > combSize) {
						comb = combination.getValue1();
						combSize = combination.getValue2().longValue();
					}
				}
				combinations = null;
				long offset = 0;
				for (UsedBlock b : comb) {
					freeSpaces.addRange(b.block);
					jp.addToJoin(mover.move(b, first.min + offset));
					offset += b.block.max - b.block.min + 1;
					usedBlocks.remove(b); // no more move for this one
				}
				// we adjust the remaining free size
				first.min += combSize;
			}
			// move data up to the next free space
			RangeLong nextFree = freeSpaces.isEmpty() ? null : freeSpaces.getFirst();
			RedBlackTreeLong<UsedBlock> toMove = new RedBlackTreeLong<>();
			for (Iterator<? extends UsedBlock> it = usedBlocks.iterator(); it.hasNext(); ) {
				UsedBlock block = it.next();
				if (block.block.min < first.min) {
					// the block is before the first free space, it can stay there
					it.remove();
					continue;
				}
				if (nextFree == null || block.block.min < nextFree.min) {
					toMove.add(block.block.min, block);
					it.remove();
				}
			}
			long offset = first.min;
			long lastBlockEnd = 0;
			for (UsedBlock b : toMove) {
				lastBlockEnd = b.block.max;
				jp.addToJoin(mover.move(b, offset));
				offset += b.block.max - b.block.min + 1;
			}
			// increase size of next free space
			if (nextFree != null)
				nextFree.min = offset;
			else
				freeSpaces.addRange(offset, lastBlockEnd);
		} while (true);
	}
	
}
