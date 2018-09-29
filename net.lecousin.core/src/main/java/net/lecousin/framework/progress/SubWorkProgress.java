package net.lecousin.framework.progress;

/** Implementation of WorkProgress for a sub-aork, that will transmit its progression to its parent. */
public class SubWorkProgress extends WorkProgressImpl implements WorkProgress.MultiTask.SubTask {

	/** Constructor. */
	public SubWorkProgress(WorkProgress parent, long parentWork, long amount, String text, String subText) {
		super(amount, text, subText);
		this.parent = parent;
		this.parentWork = parentWork;
	}

	/** Constructor. */
	public SubWorkProgress(WorkProgress parent, long parentWork, long amount, String text) {
		this(parent, parentWork, amount, text, "");
	}

	/** Constructor. */
	public SubWorkProgress(WorkProgress parent, long parentWork, long amount) {
		this(parent, parentWork, amount, "", "");
	}
	
	private WorkProgress parent;
	private long parentProgress = 0;
	private long parentWork;
	
	private void updateParent() {
		long amount = getAmount();
		long newProgress = amount > 0 ? getPosition() * parentWork / amount : 0;
		if (newProgress != parentProgress) {
			parent.progress(newProgress - parentProgress);
			parentProgress = newProgress;
		}
	}
	
	@Override
	public void setAmount(long work) {
		super.setAmount(work);
		updateParent();
	}
	
	@Override
	public void progress(long amountDone) {
		super.progress(amountDone);
		updateParent();
	}

	@Override
	public void setPosition(long position) {
		super.setPosition(position);
		updateParent();
	}

	@Override
	public void done() {
		super.done();
		updateParent();
	}

	@Override
	public long getWorkOnParent() {
		return parentWork;
	}

	@Override
	public WorkProgress getProgress() {
		return this;
	}
	
}
