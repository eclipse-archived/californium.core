package org.eclipse.californium.core.server.resources;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The default implementation for supporting accept options in resources. An
 * instance of AcceptDefaultSupport contains a set with all acceptable content
 * formats. <b>If it holds no value at all, then ALL content formats are
 * considered accepted</b>.
 */
public class AcceptDefaultSupport implements AcceptSupport {

	/** The acceptable content formats. */
	private Set<Integer> acceptables;
	
	/**
	 * Instantiates a support object for accept options.
	 */
	public AcceptDefaultSupport() {
		acceptables = new CopyOnWriteArraySet<Integer>();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.californium.core.server.resources.AcceptSupport#addAcceptable(int)
	 */
	public void addAcceptable(int... acceptableValues) {
		for (int acc:acceptableValues)
			acceptables.add(acc);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.californium.core.server.resources.AcceptSupport#removeAcceptable(int)
	 */
	public void removeAcceptable(int... values) {
		for (int val:values)
			acceptables.remove(val);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.californium.core.server.resources.AcceptSupport#accepts(int)
	 */
	public boolean isAcceptable(int acceptable) {
		return acceptables.isEmpty() || acceptables.contains(acceptable);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AcceptDefaultSupport("+acceptables.toString()+")";
	}
}
