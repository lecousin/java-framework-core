package net.lecousin.framework.memory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * A bank of objects that holds unused instances for reuse.
 * Each time a new instance is requested, if an unused one is present it returns it, else it create a new one.
 * Each time an instance if released, it keeps it for future reuse (with a maximum number of unused instances to keep).
 * @param <T> type of object
 */
public class ObjectBank<T> implements IMemoryManageable, Closeable {

	/** Constructor. */
	public ObjectBank(int maxSize, String description) {
		this.maxSize = maxSize;
		bank = new ArrayList<>(maxSize);
		this.description = description;
		MemoryManager.register(this);
	}
	
	private ArrayList<T> bank;
	private int maxSize;
	private String description;
	
	@Override
	public void close() {
		MemoryManager.unregister(this);
		bank = null;
	}
	
	/** Get an instance. */
	public synchronized T get() {
		if (bank.isEmpty()) return null;
		return bank.remove(bank.size() - 1);
	}
	
	/** Release an instance. */
	public synchronized void free(T object) {
		if (bank.size() == maxSize) return;
		bank.add(object);
	}
	
	@Override
	public String getDescription() {
		return "Cached list of " + description + " (" + bank.size() + " items)";
	}
	
	@Override
	public List<String> getItemsDescription() {
		return null;
	}
	
	@Override
	public synchronized void freeMemory(FreeMemoryLevel level) {
		if (bank.isEmpty()) return;
		int nb;
		switch (level) {
		default:
		case LOW:
			nb = bank.size() / 10;
			if (nb == 0) nb = 1;
			break;
		case MEDIUM:
			nb = bank.size() / 3;
			if (nb == 0) nb = 1;
			break;
		case URGENT:
			nb = bank.size();
			break;
		}
		while (nb-- > 0)
			bank.remove(bank.size() - 1);
	}
	
}
