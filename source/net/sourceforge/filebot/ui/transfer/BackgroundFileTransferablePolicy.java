
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;


public abstract class BackgroundFileTransferablePolicy<V> extends FileTransferablePolicy {
	
	public static final String LOADING_PROPERTY = "loading";
	
	private BackgroundWorker worker = null;
	
	
	@Override
	public boolean accept(Transferable tr) {
		if (isActive())
			return false;
		
		return super.accept(tr);
	}
	

	@Override
	public synchronized void handleTransferable(Transferable tr, TransferAction action) {
		List<File> files = getFilesFromTransferable(tr);
		
		if (action != TransferAction.ADD)
			clear();
		
		worker = new BackgroundWorker(files);
		worker.addPropertyChangeListener(backgroundWorkerListener);
		worker.execute();
	}
	

	public synchronized boolean isActive() {
		return worker != null && !worker.isDone();
	}
	

	public synchronized void reset() {
		if (isActive()) {
			worker.cancel(true);
		}
	}
	

	/**
	 * Receives data chunks from the publish method asynchronously on the Event Dispatch
	 * Thread.
	 * 
	 * @param chunks
	 */
	protected abstract void process(List<V> chunks);
	

	/**
	 * Sends data chunks to the process method.
	 * 
	 * @param chunks
	 */
	protected synchronized void publish(V... chunks) {
		if (worker != null) {
			worker.publishChunks(chunks);
		}
	}
	
	
	private class BackgroundWorker extends SwingWorker<Void, V> {
		
		private final List<File> files;
		
		
		public BackgroundWorker(List<File> files) {
			this.files = files;
		}
		

		@Override
		protected Void doInBackground() {
			try {
				load(files);
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.WARNING, e.getMessage(), e);
			}
			
			return null;
		}
		

		public void publishChunks(V... chunks) {
			if (!isCancelled()) {
				publish(chunks);
			}
		}
		

		@Override
		protected void process(List<V> chunks) {
			if (!isCancelled()) {
				BackgroundFileTransferablePolicy.this.process(chunks);
			}
		}
	}
	
	private final PropertyChangeListener backgroundWorkerListener = new SwingWorkerPropertyChangeAdapter() {
		
		@Override
		public void started(PropertyChangeEvent evt) {
			propertyChangeSupport.firePropertyChange(LOADING_PROPERTY, false, true);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			propertyChangeSupport.firePropertyChange(LOADING_PROPERTY, true, false);
		}
	};
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
}
