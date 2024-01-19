package com.chaosinmotion.git.test.protocol.network;

import java.util.LinkedList;

/**
 * This is a simple thread pool which launches a fixed number of threads and
 * then allows you to submit tasks to be run on those threads.
 */
public class ThreadPool
{
	public static final int MAX_THREADS = 10;
	public static final ThreadPool shared = new ThreadPool();

	private LinkedList<Runnable> queue = new LinkedList<>();
	private WorkerThread[] threads = new WorkerThread[MAX_THREADS];
	private int pos = 0;
	private int count = 0;
	private boolean shutdown = false;

	private class WorkerThread extends Thread
	{
		public WorkerThread(int pos)
		{
			super("WprkerThread " + (pos+1));
		}

		@Override
		public void run()
		{
			boolean done = false;
			while (!done) {
				Runnable r = null;
				synchronized (ThreadPool.this) {
					if (!queue.isEmpty()) {
						r = queue.removeFirst();
					} else if (shutdown) {
						done = true;
					} else {
						count++;
						try {
							ThreadPool.this.wait();
						}
						catch (InterruptedException e) {
							// ignore
						}
						count--;
					}
				}
				if (r != null) {
					try {
						r.run();
					}
					catch (Throwable err) {
						// Ignore all runtime errors
					}
				}
			}
		}
	}

	public synchronized void enqueue(Runnable r)
	{
		/*
		 *	Enqueue the runnable and notify the next thread that there is work
		 */

		queue.addLast(r);
		if ((count == 0) && (pos < MAX_THREADS)) {
			// There are no available threads, and we haven't created all of
			// our background threads yet.
			WorkerThread t = new WorkerThread(pos);
			t.setDaemon(true);
			t.start();

			threads[pos++] = t;
		}
		notify();		// notify if anyone is waiting
	}

	public void shutdown()
	{
		synchronized (this) {
			shutdown = true;
			notifyAll();
		}

		for (int i = 0; i < pos; i++) {
			try {
				if (threads[i] != null) {
					threads[i].join();
				}
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}
}
