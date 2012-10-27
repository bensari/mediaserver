/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.server.scheduler;

import java.lang.InterruptedException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

/**
 * Implements scheduler with multi-level priority queue.
 *
 * This scheduler implementation follows to uniprocessor model with "super" thread.
 * The "super" thread includes IO bound thread and one or more CPU bound threads
 * with equal priorities.
 *
 * The actual priority is assigned to task instead of process and can be
 * changed dynamically at runtime using the initial priority level, feedback
 * and other parameters.
 *
 *
 * @author Oifa Yulian
 */
public class Scheduler  {
	//SS7 QUEUES
	public static final Integer RECEIVER_QUEUE=0;
	public static final Integer SENDER_QUEUE=1;
	
	//MANAGEMENT QUEUE FOR CONTROL PROCESSING
	public static final Integer MANAGEMENT_QUEUE=2;
	
	//UDP MANAGER QUEUE FOR POOLING CHANNELS
	public static final Integer UDP_MANAGER_QUEUE=3;	
	
	//CORE QUEUES
	public static final Integer INPUT_QUEUE=4;
	public static final Integer MIXER_MIX_QUEUE=5;
	public static final Integer OUTPUT_QUEUE=6;
	
	//HEARTBEAT QUEUE
	public static final Integer HEARTBEAT_QUEUE=-1;
	
    //The clock for time measurement
    private Clock clock;

    //priority queue
    protected OrderedTaskQueue[] taskQueues = new OrderedTaskQueue[7];

    protected OrderedTaskQueue heartBeatQueue;
    
    //CPU bound threads
    private CoreThread coreThread;
    private CriticalThread criticalThread;
    
    //flag indicating state of the scheduler
    private boolean isActive;

    private Logger logger = Logger.getLogger(Scheduler.class) ;
    
    private ConcurrentLinkedList<Task> waitingTasks=new ConcurrentLinkedList<Task>();
    private ConcurrentLinkedList<Task> criticalTasks=new ConcurrentLinkedList<Task>();
    
    private WorkerThread[] workerThreads;
    private CriticalWorkerThread[] criticalWorkerThreads;
    /**
     * Creates new instance of scheduler.
     */
    public Scheduler() {
    	for(int i=0;i<taskQueues.length;i++)
    		taskQueues[i]=new OrderedTaskQueue();
    	
    	heartBeatQueue=new OrderedTaskQueue();
    	
    	coreThread = new CoreThread(String.format("Scheduler"));  
    	criticalThread = new CriticalThread(String.format("Scheduler"));
    	
        workerThreads=new WorkerThread[Runtime.getRuntime().availableProcessors()*2];
        criticalWorkerThreads=new CriticalWorkerThread[Runtime.getRuntime().availableProcessors()*2];
        for(int i=0;i<workerThreads.length;i++)
        	workerThreads[i]=new WorkerThread();
        
        for(int i=0;i<criticalWorkerThreads.length;i++)
        	criticalWorkerThreads[i]=new CriticalWorkerThread();
    }    

    public int getPoolSize()
    {
    	return workerThreads.length;
    }
    
    /**
     * Sets clock.
     *
     * @param clock the clock used for time measurement.
     */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Gets the clock used by this scheduler.
     *
     * @return the clock object.
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Queues task for execution according to its priority.
     *
     * @param task the task to be executed.
     */
    public void submit(Task task,Integer index) {
        task.activate(false);
        taskQueues[index].accept(task);
    }
    
    /**
     * Queues task for execution according to its priority.
     *
     * @param task the task to be executed.
     */
    public void submitHeatbeat(Task task) {
        task.activate(true);
        heartBeatQueue.accept(task);
    }
    
    /**
     * Queues chain of the tasks for execution.
     * 
     * @param taskChanin the chain of the tasks
     */
    public void submit(TaskChain taskChain) {    	
        taskChain.start(MANAGEMENT_QUEUE);
    }    
    
    /**
     * Starts scheduler.
     */
    public void start() {
    	if(this.isActive)
    		return;
    	
        if (clock == null) {
            throw new IllegalStateException("Clock is not set");
        }

        this.isActive = true;
        
        logger.info("Starting ");
        
        coreThread.activate();
        criticalThread.activate();
        for(int i=0;i<workerThreads.length;i++)
        	workerThreads[i].activate();
        
        for(int i=0;i<criticalWorkerThreads.length;i++)
        	criticalWorkerThreads[i].activate();        
        
        logger.info("Started ");
    }

    /**
     * Stops scheduler.
     */
    public void stop() {
        if (!this.isActive) {
            return;
        }

        coreThread.shutdown();
        criticalThread.shutdown();
        for(int i=0;i<workerThreads.length;i++)
        	workerThreads[i].shutdown();
        
        for(int i=0;i<criticalWorkerThreads.length;i++)
        	criticalWorkerThreads[i].shutdown();
        
        try
        {
        	Thread.sleep(40);
        }
        catch(InterruptedException e)
		{                				
		}
        
        for(int i=0;i<taskQueues.length;i++)
        	taskQueues[i].clear();
        
        heartBeatQueue.clear();
    }

    //removed statistics to increase perfomance
    /**
     * Shows the miss rate.
     * 
     * @return the miss rate value;
     */
    public double getMissRate() {
        return 0;
    }

    public long getWorstExecutionTime() {
        return 0;
    }

    /**
     * Executor thread.
     */
    private class CoreThread extends Thread {        
        private volatile boolean active;
        private int currQueue=UDP_MANAGER_QUEUE;        
        private AtomicInteger activeTasksCount=new AtomicInteger();
        private long cycleStart=0;
        private int runIndex=0;
        private Object LOCK=new Object();
        
        public CoreThread(String name) {
            super(name);            
        }
        
        public void activate() {        	        	
        	this.active = true;
        	cycleStart = clock.getTime();
        	this.start();
        }
        
        public void notifyCompletion() {
        	if(activeTasksCount.decrementAndGet()==0)
        		synchronized(LOCK) {
        			LOCK.notify();
        		}        	        	
        }
        
        @Override
        public void run() {        	
        	long cycleDuration;
        	
        	while(active)
        	{
        		currQueue=UDP_MANAGER_QUEUE;
        		while(currQueue<=OUTPUT_QUEUE)
    			{    				    				
    				synchronized(LOCK) {    					
    					if(executeQueue(taskQueues[currQueue]))
    						try {
    							LOCK.wait();
    						}
							catch(InterruptedException e)  {                                               
								//lets continue
							}
    				}
				
    				currQueue++;
    			}				        		
        		
        		runIndex=(runIndex+1)%5;        		
    			if(runIndex==0)    				    				
    				synchronized(LOCK) {
    					if(executeQueue(heartBeatQueue))
    						try  {
    							LOCK.wait();
    						}
    						catch(InterruptedException e)  {                                               
    							//lets continue
    						}
    				}    
    			
    			//sleep till next cycle
        		cycleDuration=clock.getTime() - cycleStart;
        		if(cycleDuration<20000000L)
        			try  {                                               
        				sleep(20L-cycleDuration/1000000L,(int)((20000000L-cycleDuration)%1000000L));
        			}
                	catch(InterruptedException e)  {                                               
                		//lets continue
                	}
        		
        		//new cycle starts , updating cycle start time by 4ms
                cycleStart = cycleStart + 20000000L;                                              
        	}
        }
        
        private boolean executeQueue(OrderedTaskQueue currQueue)
        {
        	Task t;        	
        	currQueue.changePool();
            t = currQueue.poll();
            activeTasksCount.incrementAndGet();
            
            //submit all tasks in current queue
            while(t!=null)
            {
            	activeTasksCount.incrementAndGet();
            	waitingTasks.offer(t);
            	t = currQueue.poll();
            }
            
            return activeTasksCount.decrementAndGet()!=0;
        }

        /**
         * Terminates thread.
         */
        private void shutdown() {
            this.active = false;
        }
    }    
    
    /**
     * Executor thread.
     */
    private class CriticalThread extends Thread {        
        private volatile boolean active;
        private AtomicInteger activeTasksCount=new AtomicInteger();
        private long cycleStart=0;
        private Object LOCK=new Object();
        
        public CriticalThread(String name) {
            super(name);            
        }
        
        public void activate() {        	        	
        	this.active = true;
        	cycleStart = clock.getTime();
        	this.start();
        }
        
        public void notifyCompletion() {
        	if(activeTasksCount.decrementAndGet()==0)
        		synchronized(LOCK) {
        			LOCK.notify();
        		}        	        	
        }
        
        @Override
        public void run() {        	
        	long cycleDuration;
        	
        	while(active)
        	{
        		synchronized(LOCK) {    					
					if(executeQueue(taskQueues[RECEIVER_QUEUE]))
						try {
							LOCK.wait();
						}
						catch(InterruptedException e)  {                                               
							//lets continue
						}
				}
        		
        		synchronized(LOCK) {    					
					if(executeQueue(taskQueues[SENDER_QUEUE]))
						try {
							LOCK.wait();
						}
						catch(InterruptedException e)  {                                               
							//lets continue
						}
				}
        		
        		synchronized(LOCK) {    					
					if(executeQueue(taskQueues[MANAGEMENT_QUEUE]))
						try {
							LOCK.wait();
						}
						catch(InterruptedException e)  {                                               
							//lets continue
						}
				}
        		
        		//sleep till next cycle
        		cycleDuration=clock.getTime() - cycleStart;
        		if(cycleDuration<4000000L)
        			try  {                                               
        				sleep(4L-cycleDuration/1000000L,(int)((4000000L-cycleDuration)%1000000L));
        			}
                	catch(InterruptedException e)  {                                               
                		//lets continue
                	}
        		
        		//new cycle starts , updating cycle start time by 4ms
                cycleStart = cycleStart + 4000000L;                                              
        	}
        }
        
        private boolean executeQueue(OrderedTaskQueue currQueue)
        {
        	Task t;        	
        	currQueue.changePool();
            t = currQueue.poll();
            activeTasksCount.incrementAndGet();
            
            //submit all tasks in current queue
            while(t!=null)
            {
            	activeTasksCount.incrementAndGet();
            	criticalTasks.offer(t);
            	t = currQueue.poll();
            }
            
            return activeTasksCount.decrementAndGet()!=0;
        }

        /**
         * Terminates thread.
         */
        private void shutdown() {
            this.active = false;
        }
    }
    
    private class WorkerThread extends Thread {
    	private volatile boolean active;
        
    	public void run() {
    		while(active)
    		{
    			waitingTasks.take().run();
    			coreThread.notifyCompletion();
    		}
    	}
    	
    	public void activate() {        	        	
        	this.active = true;
        	this.start();
        }
    	
    	/**
         * Terminates thread.
         */
        private void shutdown() {
            this.active = false;
        }
    }
    
    private class CriticalWorkerThread extends Thread {
    	private volatile boolean active;
        
    	public void run() {
    		while(active)
    		{
    			criticalTasks.take().run();
    			criticalThread.notifyCompletion();
    		}
    	}
    	
    	public void activate() {        	        	
        	this.active = true;
        	this.start();
        }
    	
    	/**
         * Terminates thread.
         */
        private void shutdown() {
            this.active = false;
        }
    }
}
